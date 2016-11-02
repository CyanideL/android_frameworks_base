/*
 * Copyright (C) 2012 The Android Open Source Project
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
 * limitations under the License.
 */

package com.android.keyguard;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.util.TypedValue;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextClock;
import android.widget.TextView;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.util.cyanide.FontHelper;

import java.util.Locale;

public class KeyguardStatusView extends GridLayout {
    private static final boolean DEBUG = KeyguardConstants.DEBUG;
    private static final String TAG = "KeyguardStatusView";

    private final LockPatternUtils mLockPatternUtils;
    private final AlarmManager mAlarmManager;

    private TextView mAlarmStatusView;
    private TextClock mDateView;
    private TextClock mClockView;
    private KeyguardRomLogo mRomLogo;
    private int mLCFontSize = 88;
    private TextView mOwnerInfo;
    private int mOwnerSize = 14;
    private int mAlarmDateSize = 14;
    private static Typeface mFontStyle;

    //On the first boot, keyguard will start to receiver TIME_TICK intent.
    //And onScreenTurnedOff will not get called if power off when keyguard is not started.
    //Set initial value to false to skip the above case.
    private boolean enableRefresh = false;

    private KeyguardUpdateMonitorCallback mInfoCallback = new KeyguardUpdateMonitorCallback() {

        @Override
        public void onTimeChanged() {
            if (enableRefresh) {
                refresh();
            }
            //updateOwnerSize();
            //updateClockSize();
            //updateAlarmDateSize();
            //updateFontStyle();
        }

        @Override
        public void onKeyguardVisibilityChanged(boolean showing) {
            if (showing) {
                if (DEBUG) Slog.v(TAG, "refresh statusview showing:" + showing);
                refresh();
                updateOwnerInfo();
                updateLogoVisibility();
                updateLogoColor();
                updateLogoImage();
                updateOwnerSize();
                updateClockSize();
                updateAlarmDateSize();
                updateFontStyle();
            }
        }

        @Override
        public void onStartedWakingUp() {
            setEnableMarquee(true);
            enableRefresh = true;
            refresh();
            updateOwnerSize();
            updateClockSize();
            updateAlarmDateSize();
        }

        @Override
        public void onFinishedGoingToSleep(int why) {
            setEnableMarquee(false);
            enableRefresh = false;
        }

        @Override
        public void onUserSwitchComplete(int userId) {
            refresh();
            updateOwnerInfo();
            updateLogoVisibility();
            updateLogoColor();
            updateLogoImage();
            updateOwnerSize();
            updateClockSize();
            updateAlarmDateSize();
            updateFontStyle();
        }
    };

    public KeyguardStatusView(Context context) {
        this(context, null, 0);
    }

    public KeyguardStatusView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyguardStatusView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mLockPatternUtils = new LockPatternUtils(getContext());
        updateOwnerSize();
        updateClockSize();
        updateAlarmDateSize();
        updateFontStyle();
    }

    private void setEnableMarquee(boolean enabled) {
        if (DEBUG) Log.v(TAG, (enabled ? "Enable" : "Disable") + " transport text marquee");
        if (mAlarmStatusView != null) mAlarmStatusView.setSelected(enabled);
        if (mOwnerInfo != null) mOwnerInfo.setSelected(enabled);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mAlarmStatusView = (TextView) findViewById(R.id.alarm_status);
        mDateView = (TextClock) findViewById(R.id.date_view);
        mClockView = (TextClock) findViewById(R.id.clock_view);
        mDateView.setShowCurrentUserTime(true);
        mClockView.setShowCurrentUserTime(true);
        mOwnerInfo = (TextView) findViewById(R.id.owner_info);
        mRomLogo = (KeyguardRomLogo) findViewById(R.id.keyguard_rom_logo_container);

        boolean shouldMarquee = KeyguardUpdateMonitor.getInstance(mContext).isDeviceInteractive();
        setEnableMarquee(shouldMarquee);
        refresh();
        updateOwnerInfo();
        updateLogoVisibility();
        updateLogoColor();
        updateLogoImage();
        updateOwnerSize();
        updateClockSize();
        updateAlarmDateSize();
        updateFontStyle();

        // Disable elegant text height because our fancy colon makes the ymin value huge for no
        // reason.
        mClockView.setElegantTextHeight(false);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Some layouts like burmese have a different margin for the clock
        MarginLayoutParams layoutParams = (MarginLayoutParams) mClockView.getLayoutParams();
        layoutParams.bottomMargin = getResources().getDimensionPixelSize(
                R.dimen.bottom_text_spacing_digital);
        mClockView.setLayoutParams(layoutParams);
        mClockView.setTextSize(mLCFontSize);
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.widget_label_font_size));
        mOwnerInfo.setTextSize(mOwnerSize);
    }

    public void refreshTime() {
        mDateView.setFormat24Hour(Patterns.dateView);
        mDateView.setFormat12Hour(Patterns.dateView);

        mClockView.setFormat12Hour(Patterns.clockView12);
        mClockView.setFormat24Hour(Patterns.clockView24);
        updateClockSize();
    }

    private void refresh() {
        AlarmManager.AlarmClockInfo nextAlarm =
                mAlarmManager.getNextAlarmClock(UserHandle.USER_CURRENT);
        Patterns.update(mContext, nextAlarm != null);

        refreshTime();
        refreshAlarmStatus(nextAlarm);
    }

    void refreshAlarmStatus(AlarmManager.AlarmClockInfo nextAlarm) {
        if (nextAlarm != null) {
            String alarm = formatNextAlarm(mContext, nextAlarm);
            mAlarmStatusView.setText(alarm);
            mAlarmStatusView.setContentDescription(
                    getResources().getString(R.string.keyguard_accessibility_next_alarm, alarm));
            mAlarmStatusView.setVisibility(View.VISIBLE);
        } else {
            mAlarmStatusView.setVisibility(View.GONE);
        }
    }

    public static String formatNextAlarm(Context context, AlarmManager.AlarmClockInfo info) {
        if (info == null) {
            return "";
        }
        String skeleton = DateFormat.is24HourFormat(context, ActivityManager.getCurrentUser())
                ? "EHm"
                : "Ehma";
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
        return DateFormat.format(pattern, info.getTriggerTime()).toString();
    }

    private void updateOwnerInfo() {
        if (mOwnerInfo == null) return;
        String ownerInfo = getOwnerInfo();
        if (!TextUtils.isEmpty(ownerInfo)) {
            mOwnerInfo.setVisibility(View.VISIBLE);
            mOwnerInfo.setText(ownerInfo);
        } else {
            mOwnerInfo.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        KeyguardUpdateMonitor.getInstance(mContext).registerCallback(mInfoCallback);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyguardUpdateMonitor.getInstance(mContext).removeCallback(mInfoCallback);
    }

    private String getOwnerInfo() {
        String info = null;
        if (mLockPatternUtils.isDeviceOwnerInfoEnabled()) {
            // Use the device owner information set by device policy client via
            // device policy manager.
            info = mLockPatternUtils.getDeviceOwnerInfo();
        } else {
            // Use the current user owner information if enabled.
            final boolean ownerInfoEnabled = mLockPatternUtils.isOwnerInfoEnabled(
                    KeyguardUpdateMonitor.getCurrentUser());
            if (ownerInfoEnabled) {
                info = mLockPatternUtils.getOwnerInfo(KeyguardUpdateMonitor.getCurrentUser());
            }
        }
        return info;
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    private void updateClockSize() {
        mLCFontSize = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.LOCK_CLOCK_FONT_SIZE, 88);

        if (mClockView != null) {
            mClockView.setTextSize(mLCFontSize);
        }
    }

    private void updateOwnerSize() {
        mOwnerSize = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.OWNER_INFO_FONT_SIZE, 14);

        if (mOwnerInfo != null) {
            mOwnerInfo.setTextSize(mOwnerSize);
        }
    }

    private void updateAlarmDateSize() {
        mAlarmDateSize = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.LS_ALARM_DATE_FONT_SIZE, 14);

        if (mAlarmStatusView != null) {
            mAlarmStatusView.setTextSize(mAlarmDateSize);
        }
        if (mDateView != null) {
            mDateView.setTextSize(mAlarmDateSize);
        }
    }

    // DateFormat.getBestDateTimePattern is extremely expensive, and refresh is called often.
    // This is an optimization to ensure we only recompute the patterns when the inputs change.
    private static final class Patterns {
        static String dateView;
        static String clockView12;
        static String clockView24;
        static String cacheKey;

        static void update(Context context, boolean hasAlarm) {
            final Locale locale = Locale.getDefault();
            final Resources res = context.getResources();
            final String dateViewSkel = res.getString(hasAlarm
                    ? R.string.abbrev_wday_month_day_no_year_alarm
                    : R.string.abbrev_wday_month_day_no_year);
            final String clockView12Skel = res.getString(R.string.clock_12hr_format);
            final String clockView24Skel = res.getString(R.string.clock_24hr_format);
            final String key = locale.toString() + dateViewSkel + clockView12Skel + clockView24Skel;
            if (key.equals(cacheKey)) return;

            dateView = DateFormat.getBestDateTimePattern(locale, dateViewSkel);

            clockView12 = DateFormat.getBestDateTimePattern(locale, clockView12Skel);
            // CLDR insists on adding an AM/PM indicator even though it wasn't in the skeleton
            // format.  The following code removes the AM/PM indicator if we didn't want it.
            if (!clockView12Skel.contains("a")) {
                clockView12 = clockView12.replaceAll("a", "").trim();
            }

            clockView24 = DateFormat.getBestDateTimePattern(locale, clockView24Skel);

            // Use fancy colon.
            clockView24 = clockView24.replace(':', '\uee01');
            clockView12 = clockView12.replace(':', '\uee01');

            cacheKey = key;
        }
    }

    private void updateLogoVisibility() {
        final boolean showLogo = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.KEYGUARD_LOGO_SHOW, 1) == 1;

        if (mRomLogo != null) {
            mRomLogo.showLogo(showLogo);
        }
    }

    private void updateLogoColor() {
        int color = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.KEYGUARD_LOGO_COLOR, 0xffffffff);

        if (mRomLogo != null) {
            mRomLogo.setIconColor(color);

        }
    }

    private void updateLogoImage() {

        if (mRomLogo != null) {
            mRomLogo.updateLogoImage();
        }
    }

    private void updateFontStyle() {
        final int mLSFontStyle = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.LOCK_SCREEN_FONT_STYLE, FontHelper.FONT_COMINGSOON);

        getFontStyle(mLSFontStyle);
    }

    public void getFontStyle(int font) {
        switch (font) {
            case FontHelper.FONT_NORMAL:
            default:
                mFontStyle = FontHelper.NORMAL;
                break;
            case FontHelper.FONT_ITALIC:
                mFontStyle = FontHelper.ITALIC;
                break;
            case FontHelper.FONT_BOLD:
                mFontStyle = FontHelper.BOLD;
                break;
            case FontHelper.FONT_BOLD_ITALIC:
                mFontStyle = FontHelper.BOLD_ITALIC;
                break;
            case FontHelper.FONT_LIGHT:
                mFontStyle = FontHelper.LIGHT;
                break;
            case FontHelper.FONT_LIGHT_ITALIC:
                mFontStyle = FontHelper.LIGHT_ITALIC;
                break;
            case FontHelper.FONT_THIN:
                mFontStyle = FontHelper.THIN;
                break;
            case FontHelper.FONT_THIN_ITALIC:
                mFontStyle = FontHelper.THIN_ITALIC;
                break;
            case FontHelper.FONT_CONDENSED:
                mFontStyle = FontHelper.CONDENSED;
                break;
            case FontHelper.FONT_CONDENSED_ITALIC:
                mFontStyle = FontHelper.CONDENSED_ITALIC;
                break;
            case FontHelper.FONT_CONDENSED_LIGHT:
                mFontStyle = FontHelper.CONDENSED_LIGHT;
                break;
            case FontHelper.FONT_CONDENSED_LIGHT_ITALIC:
                mFontStyle = FontHelper.CONDENSED_LIGHT_ITALIC;
                break;
            case FontHelper.FONT_CONDENSED_BOLD:
                mFontStyle = FontHelper.CONDENSED_BOLD;
                break;
            case FontHelper.FONT_CONDENSED_BOLD_ITALIC:
                mFontStyle = FontHelper.CONDENSED_BOLD_ITALIC;
                break;
            case FontHelper.FONT_MEDIUM:
                mFontStyle = FontHelper.MEDIUM;
                break;
            case FontHelper.FONT_MEDIUM_ITALIC:
                mFontStyle = FontHelper.MEDIUM_ITALIC;
                break;
            case FontHelper.FONT_BLACK:
                mFontStyle = FontHelper.BLACK;
                break;
            case FontHelper.FONT_BLACK_ITALIC:
                mFontStyle = FontHelper.BLACK_ITALIC;
                break;
            case FontHelper.FONT_DANCINGSCRIPT:
                mFontStyle = FontHelper.DANCINGSCRIPT;
                break;
            case FontHelper.FONT_DANCINGSCRIPT_BOLD:
                mFontStyle = FontHelper.DANCINGSCRIPT_BOLD;
                break;
            case FontHelper.FONT_COMINGSOON:
                mFontStyle = FontHelper.COMINGSOON;
                break;
            case FontHelper.FONT_NOTOSERIF:
                mFontStyle = FontHelper.NOTOSERIF;
                break;
            case FontHelper.FONT_NOTOSERIF_ITALIC:
                mFontStyle = FontHelper.NOTOSERIF_ITALIC;
                break;
            case FontHelper.FONT_NOTOSERIF_BOLD:
                mFontStyle = FontHelper.NOTOSERIF_BOLD;
                break;
            case FontHelper.FONT_NOTOSERIF_BOLD_ITALIC:
                mFontStyle = FontHelper.NOTOSERIF_BOLD_ITALIC;
                break;
        }
        if (mDateView != null) mDateView.setTypeface(mFontStyle);
        if (mClockView != null) mClockView.setTypeface(mFontStyle);
        if (mOwnerInfo != null) mOwnerInfo.setTypeface(mFontStyle);
        if (mAlarmStatusView != null) mAlarmStatusView.setTypeface(mFontStyle);
    }
}
