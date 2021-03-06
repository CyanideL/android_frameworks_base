/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.systemui.statusbar.phone;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextClock;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto;
import com.android.keyguard.KeyguardStatusView;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.qs.QSAnimator;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.qs.QSPanel.Callback;
import com.android.systemui.qs.QuickQSPanel;
import com.android.systemui.qs.TouchAnimator;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.NextAlarmController;
import com.android.systemui.statusbar.policy.NextAlarmController.NextAlarmChangeCallback;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.statusbar.policy.UserInfoController.OnUserInfoChangedListener;
import com.android.systemui.tuner.TunerService;

import com.android.internal.util.cyanide.QSColorHelper;

import java.net.URISyntaxException;

public class QuickStatusBarHeader extends BaseStatusBarHeader implements
        NextAlarmChangeCallback, OnClickListener, OnLongClickListener, OnUserInfoChangedListener {

    private static final String TAG = "QuickStatusBarHeader";

    private static final float EXPAND_INDICATOR_THRESHOLD = .93f;

    private ActivityStarter mActivityStarter;
    private NextAlarmController mNextAlarmController;
    private View mSettingsButton;

    private TextView mAlarmStatus;
    private View mAlarmStatusCollapsed;

    private View mClock;
    private View mDate;
    private TextView mTime;
    private TextView mAmPm;
    private TextView mDateCollapsed;
    private TextView mDateExpanded;

    private QSPanel mQsPanel;

    private boolean mExpanded;
    private boolean mAlarmShowing;

    private ViewGroup mDateTimeGroup;
    private ViewGroup mDateTimeAlarmGroup;
    private TextView mEmergencyOnly;

    protected ExpandableIndicator mExpandIndicator;
    private View mCyanideButton;

    private boolean mListening;
    private AlarmManager.AlarmClockInfo mNextAlarm;

    private QuickQSPanel mHeaderQsPanel;
    private boolean mShowEmergencyCallsOnly;
    protected MultiUserSwitch mMultiUserSwitch;
    private ImageView mMultiUserAvatar;

    private float mDateTimeTranslation;
    private float mDateTimeAlarmTranslation;
    private float mDateScaleFactor;
    protected float mGearTranslation;

    private TouchAnimator mSecondHalfAnimator;
    private TouchAnimator mFirstHalfAnimator;
    private TouchAnimator mDateSizeAnimator;
    private TouchAnimator mAlarmTranslation;
    protected TouchAnimator mSettingsAlpha;
    private float mExpansionAmount;
    private QSTileHost mHost;
    private boolean mShowFullAlarm;

    private SettingsObserver mSettingsObserver;

    public QuickStatusBarHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mSettingsObserver = new SettingsObserver(new Handler());

        mEmergencyOnly = (TextView) findViewById(R.id.header_emergency_calls_only);

        mDateTimeAlarmGroup = (ViewGroup) findViewById(R.id.date_time_alarm_group);
        mDateTimeAlarmGroup.findViewById(R.id.empty_time_view).setVisibility(View.GONE);
        mDateTimeGroup = (ViewGroup) findViewById(R.id.date_time_group);
        mDateTimeGroup.setPivotX(0);
        mDateTimeGroup.setPivotY(0);
        mShowFullAlarm = getResources().getBoolean(R.bool.quick_settings_show_full_alarm);

        mClock = findViewById(R.id.clock);
        mClock.setOnClickListener(this);
        mDate = findViewById(R.id.date);
        mDate.setOnClickListener(this);
        mTime = (TextView) findViewById(R.id.time_view);
        mAmPm = (TextView) findViewById(R.id.am_pm_view);
        mDateCollapsed = (TextView) findViewById(R.id.date_collapsed);
        mDateExpanded = (TextView) findViewById(R.id.date_expanded);

        mExpandIndicator = (ExpandableIndicator) findViewById(R.id.expand_indicator);

        mHeaderQsPanel = (QuickQSPanel) findViewById(R.id.quick_qs_panel);

        mSettingsButton = findViewById(R.id.settings_button);
        mSettingsButton.setOnClickListener(this);
        mCyanideButton = findViewById(R.id.cyanide_button);
        mCyanideButton.setOnClickListener(this);
        mCyanideButton.setOnLongClickListener(this);

        mAlarmStatusCollapsed = findViewById(R.id.alarm_status_collapsed);
        mAlarmStatus = (TextView) findViewById(R.id.alarm_status);
        mAlarmStatus.setOnClickListener(this);

        mMultiUserSwitch = (MultiUserSwitch) findViewById(R.id.multi_user_switch);
        mMultiUserAvatar = (ImageView) mMultiUserSwitch.findViewById(R.id.multi_user_avatar);

        // RenderThread is doing more harm than good when touching the header (to expand quick
        // settings), so disable it for this view
        ((RippleDrawable) mSettingsButton.getBackground()).setForceSoftware(true);
        //((RippleDrawable) mExpandIndicator.getBackground()).setForceSoftware(true);
        mExpandIndicator.setVisibility(View.GONE);
        ((RippleDrawable) mCyanideButton.getBackground()).setForceSoftware(true);

        updateResources();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateResources();
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        updateResources();
    }

    private void updateResources() {
        FontSizeUtils.updateFontSize(mAlarmStatus, R.dimen.qs_date_collapsed_size);
        FontSizeUtils.updateFontSize(mEmergencyOnly, R.dimen.qs_emergency_calls_only_text_size);

        mGearTranslation = mContext.getResources().getDimension(R.dimen.qs_header_gear_translation);

        mDateTimeTranslation = mContext.getResources().getDimension(
                R.dimen.qs_date_anim_translation);
        mDateTimeAlarmTranslation = mContext.getResources().getDimension(
                R.dimen.qs_date_alarm_anim_translation);
        float dateCollapsedSize = mContext.getResources().getDimension(
                R.dimen.qs_date_collapsed_text_size);
        float dateExpandedSize = mContext.getResources().getDimension(
                R.dimen.qs_date_text_size);
        mDateScaleFactor = dateExpandedSize / dateCollapsedSize;
        updateDateTimePosition();

        mSecondHalfAnimator = new TouchAnimator.Builder()
                .addFloat(mShowFullAlarm ? mAlarmStatus : findViewById(R.id.date), "alpha", 0, 1)
                .addFloat(mEmergencyOnly, "alpha", 0, 1)
                .setStartDelay(.5f)
                .build();
        if (mShowFullAlarm) {
            mFirstHalfAnimator = new TouchAnimator.Builder()
                    .addFloat(mAlarmStatusCollapsed, "alpha", 1, 0)
                    .setEndDelay(.5f)
                    .build();
        }
        mDateSizeAnimator = new TouchAnimator.Builder()
                .addFloat(mDateTimeGroup, "scaleX", 1, mDateScaleFactor)
                .addFloat(mDateTimeGroup, "scaleY", 1, mDateScaleFactor)
                .setStartDelay(.36f)
                .build();

        updateSettingsAnimator();
    }

    protected void updateSettingsAnimator() {
        mSettingsAlpha = new TouchAnimator.Builder()
                .addFloat(mSettingsButton, "translationY", -mGearTranslation, 0)
                .addFloat(mMultiUserSwitch, "translationY", -mGearTranslation, 0)
                .addFloat(mCyanideButton, "translationY", -mGearTranslation, 0)
                .addFloat(mSettingsButton, "rotation", -90, 0)
                .addFloat(mSettingsButton, "alpha", 0, 1)
                .addFloat(mMultiUserSwitch, "alpha", 0, 1)
                .addFloat(mCyanideButton, "rotation", -120, 0)
                .addFloat(mCyanideButton, "alpha", 0, 1)
                .setStartDelay(QSAnimator.EXPANDED_TILE_DELAY)
                .build();

        final boolean isRtl = isLayoutRtl();
        if (isRtl && mDateTimeGroup.getWidth() == 0) {
            mDateTimeGroup.addOnLayoutChangeListener(new OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom,
                        int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    mDateTimeGroup.setPivotX(getWidth());
                    mDateTimeGroup.removeOnLayoutChangeListener(this);
                }
            });
        } else {
            mDateTimeGroup.setPivotX(isRtl ? mDateTimeGroup.getWidth() : 0);
        }
    }

    @Override
    public int getCollapsedHeight() {
        return getHeight();
    }

    @Override
    public int getExpandedHeight() {
        return getHeight();
    }

    @Override
    public void setExpanded(boolean expanded) {
        if (mExpanded == expanded) return;
        mExpanded = expanded;
        mHeaderQsPanel.setExpanded(expanded);
        updateEverything();
    }

    @Override
    public void onNextAlarmChanged(AlarmManager.AlarmClockInfo nextAlarm) {
        mNextAlarm = nextAlarm;
        if (nextAlarm != null) {
            String alarmString = KeyguardStatusView.formatNextAlarm(getContext(), nextAlarm);
            mAlarmStatus.setText(alarmString);
            mAlarmStatus.setContentDescription(mContext.getString(
                    R.string.accessibility_quick_settings_alarm, alarmString));
            mAlarmStatusCollapsed.setContentDescription(mContext.getString(
                    R.string.accessibility_quick_settings_alarm, alarmString));
        }
        if (mAlarmShowing != (nextAlarm != null)) {
            mAlarmShowing = nextAlarm != null;
            updateEverything();
        }
    }

    @Override
    public void setExpansion(float headerExpansionFraction) {
        mExpansionAmount = headerExpansionFraction;
        mSecondHalfAnimator.setPosition(headerExpansionFraction);
        if (mShowFullAlarm) {
            mFirstHalfAnimator.setPosition(headerExpansionFraction);
        }
        mDateSizeAnimator.setPosition(headerExpansionFraction);
        mAlarmTranslation.setPosition(headerExpansionFraction);
        mSettingsAlpha.setPosition(headerExpansionFraction);

        updateAlarmVisibilities();

        mExpandIndicator.setExpanded(headerExpansionFraction > EXPAND_INDICATOR_THRESHOLD);
    }

    @Override
    protected void onDetachedFromWindow() {
        setListening(false);
        mHost.getUserInfoController().remListener(this);
        mHost.getNetworkController().removeEmergencyListener(this);
        super.onDetachedFromWindow();
    }

    private void updateAlarmVisibilities() {
        mAlarmStatus.setVisibility(mAlarmShowing && mShowFullAlarm ? View.VISIBLE : View.INVISIBLE);
        mAlarmStatusCollapsed.setVisibility(mAlarmShowing ? View.VISIBLE : View.INVISIBLE);
    }

    private void updateDateTimePosition() {
        // This one has its own because we have to rebuild it every time the alarm state changes.
        mAlarmTranslation = new TouchAnimator.Builder()
                .addFloat(mDateTimeAlarmGroup, "translationY", 0, mAlarmShowing
                        ? mDateTimeAlarmTranslation : mDateTimeTranslation)
                .build();
        mAlarmTranslation.setPosition(mExpansionAmount);
    }

    public void setListening(boolean listening) {
        if (listening == mListening) {
            return;
        }
        mHeaderQsPanel.setListening(listening);
        mListening = listening;
        updateListeners();
    }

    @Override
    public void updateEverything() {
        post(() -> {
            updateDateTimePosition();
            updateVisibilities();
            setClickable(false);
        });
        updateRippleColor();
    }

    protected void updateVisibilities() {
        updateAlarmVisibilities();
        mEmergencyOnly.setVisibility(mExpanded && mShowEmergencyCallsOnly
                ? View.VISIBLE : View.INVISIBLE);
        mSettingsButton.setVisibility(mExpanded ? View.VISIBLE : View.INVISIBLE);
        mMultiUserSwitch.setVisibility(mExpanded && mMultiUserSwitch.hasMultipleUsers()
                ? View.VISIBLE : View.INVISIBLE);
    }

    private void updateListeners() {
        if (mListening) {
            mNextAlarmController.addStateChangedCallback(this);
            mSettingsObserver.observe();
        } else {
            mNextAlarmController.removeStateChangedCallback(this);
            mSettingsObserver.unobserve();
        }
    }

    @Override
    public void setActivityStarter(ActivityStarter activityStarter) {
        mActivityStarter = activityStarter;
    }

    @Override
    public void setQSPanel(final QSPanel qsPanel) {
        mQsPanel = qsPanel;
        setupHost(qsPanel.getHost());
        if (mQsPanel != null) {
            mMultiUserSwitch.setQsPanel(qsPanel);
        }
    }

    public void setupHost(final QSTileHost host) {
        mHost = host;
        host.setHeaderView(mExpandIndicator);
        mHeaderQsPanel.setQSPanelAndHeader(mQsPanel, this);
        mHeaderQsPanel.setHost(host, null /* No customization in header */);
        setUserInfoController(host.getUserInfoController());
        setBatteryController(host.getBatteryController());
        setNextAlarmController(host.getNextAlarmController());

        final boolean isAPhone = mHost.getNetworkController().hasVoiceCallingFeature();
        if (isAPhone) {
            mHost.getNetworkController().addEmergencyListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mSettingsButton) {
            startSettingsActivity();
        } else if (v == mAlarmStatus && mNextAlarm != null) {
            PendingIntent showIntent = mNextAlarm.getShowIntent();
            if (showIntent != null && showIntent.isActivity()) {
                mActivityStarter.startActivity(showIntent.getIntent(), true /* dismissShade */);
            }
        } else if (v == mCyanideButton) {
            startCyanideMods();
        } else if (v == mClock) {
            startClockActivity();
        } else if (v == mDate) {
            startDateActivity();
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (v == mCyanideButton) {
            startCyanideModsLongClick();
        }
        return false;
    }

    private void startSettingsActivity() {
        mActivityStarter.startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS),
                true /* dismissShade */);
    }

    private void startCyanideMods() {
        Intent cyanideShortcutIntent = null;
            String cyanideShortcutIntentUri = Settings.System.getStringForUser(
                    mContext.getContentResolver(), Settings.System.CYANIDE_SHORTCUT, UserHandle.USER_CURRENT);
            if(cyanideShortcutIntentUri != null) {
                try {
                    cyanideShortcutIntent = Intent.parseUri(cyanideShortcutIntentUri, 0);
                } catch (URISyntaxException e) {
                    cyanideShortcutIntent = null;
                }
            }

            if(cyanideShortcutIntent != null) {
                mActivityStarter.startActivity(cyanideShortcutIntent, true);
            } else {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName("com.rogersb11.cyanide",
                    "com.rogersb11.cyanide.MainActivity");
                mActivityStarter.startActivity(intent, true /* dismissShade*/ );
            }
    }

    private void startCyanideModsLongClick() {
        Intent cyanideLongShortcutIntent = null;
            String cyanideLongShortcutIntentUri = Settings.System.getStringForUser(
                    mContext.getContentResolver(), Settings.System.CYANIDE_LONG_SHORTCUT, UserHandle.USER_CURRENT);
            if(cyanideLongShortcutIntentUri != null) {
                try {
                    cyanideLongShortcutIntent = Intent.parseUri(cyanideLongShortcutIntentUri, 0);
                } catch (URISyntaxException e) {
                    cyanideLongShortcutIntent = null;
                }
            }

            if(cyanideLongShortcutIntent != null) {
                mActivityStarter.startActivity(cyanideLongShortcutIntent, true);
            } else {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName("com.rogersb11.cyanide",
                    "com.rogersb11.cyanide.MainActivity");
                mActivityStarter.startActivity(intent, true /* dismissShade*/ );
            }
    }

    private void startClockActivity() {
        mActivityStarter.startActivity(new Intent(AlarmClock.ACTION_SET_ALARM),
                true /* dismissShade */);
    }

    private void startDateActivity() {
        Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
        builder.appendPath("time");
        ContentUris.appendId(builder, System.currentTimeMillis());
        Intent intent = new Intent(Intent.ACTION_VIEW).setData(builder.build());
        mActivityStarter.startActivity(intent, true /* dismissShade */);
    }

    @Override
    public void setNextAlarmController(NextAlarmController nextAlarmController) {
        mNextAlarmController = nextAlarmController;
    }

    @Override
    public void setBatteryController(BatteryController batteryController) {
        // Don't care
    }

    @Override
    public void setUserInfoController(UserInfoController userInfoController) {
        userInfoController.addListener(this);
    }

    @Override
    public void setCallback(Callback qsPanelCallback) {
        mHeaderQsPanel.setCallback(qsPanelCallback);
    }

    @Override
    public void setEmergencyCallsOnly(boolean show) {
        boolean changed = show != mShowEmergencyCallsOnly;
        if (changed) {
            mShowEmergencyCallsOnly = show;
            if (mExpanded) {
                updateEverything();
            }
        }
    }

    @Override
    public void onUserInfoChanged(String name, Drawable picture) {
        mMultiUserAvatar.setImageDrawable(picture);
    }

    private void updateRippleColor() {
        mSettingsButton.setBackground(getColoredBackgroundDrawable(
                mContext.getDrawable(R.drawable.ripple_drawable_oval), false));
        mCyanideButton.setBackground(getColoredBackgroundDrawable(
                mContext.getDrawable(R.drawable.ripple_drawable_oval), false));
        mAlarmStatus.setBackground(getColoredBackgroundDrawable(
                mContext.getDrawable(R.drawable.ripple_drawable_rectangle), false));
        mTime.setBackground(getColoredBackgroundDrawable(
                mContext.getDrawable(R.drawable.ripple_drawable_rectangle), false));
        mAmPm.setBackground(getColoredBackgroundDrawable(
                mContext.getDrawable(R.drawable.ripple_drawable_rectangle), false));
        mMultiUserSwitch.setBackground(getColoredBackgroundDrawable(
                mContext.getDrawable(R.drawable.ripple_drawable_oval), false));
        if (mDateTimeGroup instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) mDateTimeGroup).getChildCount(); i++) {
                if (((ViewGroup) mDateTimeGroup).getChildAt(i) instanceof TextView) {
                    TextView tv = (TextView) ((ViewGroup) mDateTimeGroup).getChildAt(i);
                    if (tv != null) {
                        tv.setBackground(getColoredBackgroundDrawable(
                                mContext.getDrawable(R.drawable.ripple_drawable_rectangle), false));
                    }
                }
            }
        }
    }

    private RippleDrawable getColoredBackgroundDrawable(Drawable rd, boolean applyRippleColor) {
        RippleDrawable background = (RippleDrawable) rd.mutate();

        background.setColor(QSColorHelper.getHeaderRippleColorList(mContext));
        return background;
    }

    public void setFontStyle() {
        if (mDateTimeGroup instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) mDateTimeGroup).getChildCount(); i++) {
                if (((ViewGroup) mDateTimeGroup).getChildAt(i) instanceof TextView) {
                    TextView tv = (TextView) ((ViewGroup) mDateTimeGroup).getChildAt(i);
                    if (tv != null) {
                        tv.setTypeface(QSPanel.mFontStyle);
                    }
                }
            }
        }
        mAmPm.setTypeface(QSPanel.mFontStyle);
        mTime.setTypeface(QSPanel.mFontStyle);
        mAlarmStatus.setTypeface(QSPanel.mFontStyle);
    }

    private void updateIconColor() {
        ((ImageView) mCyanideButton).setImageTintList(QSColorHelper.getHeaderIconColorList(mContext));
        ((ImageView) mSettingsButton).setImageTintList(QSColorHelper.getHeaderIconColorList(mContext));
        ((TextView) mDateTimeAlarmGroup.findViewById(R.id.date)).setCompoundDrawableTintList(QSColorHelper.getHeaderIconColorList(mContext));
        ((ImageView) mAlarmStatusCollapsed).setImageTintList(QSColorHelper.getHeaderIconColorList(mContext));
        Drawable alarmIcon =
                getResources().getDrawable(R.drawable.ic_access_alarms_small).mutate();
        alarmIcon.setTintList(QSColorHelper.getHeaderIconColorList(mContext));
        mAlarmStatus.setCompoundDrawablesWithIntrinsicBounds(alarmIcon, null, null, null);
    }

    private void updateTextColor() {
        mAmPm.setTextColor(QSColorHelper.getHeaderTextColor(mContext));
        mTime.setTextColor(QSColorHelper.getHeaderTextColor(mContext));
        if (mDateTimeGroup instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) mDateTimeGroup).getChildCount(); i++) {
                if (((ViewGroup) mDateTimeGroup).getChildAt(i) instanceof TextView) {
                    TextView tv = (TextView) ((ViewGroup) mDateTimeGroup).getChildAt(i);
                    if (tv != null) {
                        tv.setTextColor(QSColorHelper.getHeaderTextColor(mContext));
                    }
                }
            }
        }
        mAlarmStatus.setTextColor(QSColorHelper.getHeaderTextColor(mContext));
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.QS_FONT_STYLE),
                    false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.QS_HEADER_ICON_COLOR),
                    false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.QS_HEADER_RIPPLE_COLOR),
                    false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.QS_HEADER_TEXT_COLOR),
                    false, this);
            updateSettings();
        }

        void unobserve() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(Settings.System.getUriFor(
                    Settings.System.QS_FONT_STYLE))) {
                setFontStyle();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.QS_HEADER_ICON_COLOR))) {
                updateIconColor();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.QS_HEADER_RIPPLE_COLOR))) {
                updateRippleColor();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.QS_HEADER_TEXT_COLOR))) {
                updateTextColor();
            }

        }

        public void updateSettings() {
            setFontStyle();
            updateIconColor();
            updateTextColor();
        }
    }
}
