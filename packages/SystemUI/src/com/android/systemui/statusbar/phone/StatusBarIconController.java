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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.internal.util.cyanide.ColorHelper;
import com.android.internal.util.cyanide.StatusBarColorHelper;
import com.android.keyguard.CarrierText;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.SystemUIFactory;
import com.android.systemui.cyanide.StatusBarWeather;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.SignalClusterView;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.tuner.TunerService.Tunable;

import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Controls everything regarding the icons in the status bar and on Keyguard, including, but not
 * limited to: notification icons, signal cluster, additional status icons, and clock in the status
 * bar.
 */
public class StatusBarIconController extends StatusBarIconList implements Tunable {

    public static final long DEFAULT_TINT_ANIMATION_DURATION = 120;
    public static final String ICON_BLACKLIST = "icon_blacklist";

    private static final int LOGO_LEFT = 0;
    private static final int LOGO_RIGHT = 1;

    private int mLogoStyle;

    private Context mContext;
    private PhoneStatusBar mPhoneStatusBar;
    private DemoStatusIcons mDemoStatusIcons;

    private CarrierText mCarrierTextKeyguard;
    private StatusBarWeather mWeatherLayout;
    private LinearLayout mSystemIconArea;
    private LinearLayout mStatusIcons;
    private LinearLayout mStatusIconsKeyguard;
    private SignalClusterView mSignalCluster;
    private SignalClusterView mSignalClusterKeyguard;
    private IconMerger mNotificationIcons;

    private NotificationIconAreaController mNotificationIconAreaController;
    private View mNotificationIconAreaInner;

    private ClockController mClockController;
    private View mCenterClockLayout;

    private ImageView mCyanideLogo;
    private ImageView mCyanideLogoLeft;
    private ImageView mCyanideLogoKeyguard;
    private ImageView mCyanideLogoKeyguardLeft;

    private boolean mShowLogo = false;
    private boolean mShowKeyguardLogo = false;

    private int mIconSize;
    private int mIconHPadding;

    private float mDarkIntensity;
    private int mClockColor;
    private int mTextColor;
    private int mIconColor;
    private int mLogoColor;
    private final Rect mTintArea = new Rect();
    private static final Rect sTmpRect = new Rect();
    private static final int[] sTmpInt2 = new int[2];

    private boolean mAnimateClockColor = false;
    private boolean mAnimateTextColor = false;
    private boolean mAnimateIconColor = false;
    private boolean mAnimateLogoColor = false;

    private boolean mTransitionPending;
    private boolean mTintChangePending;
    private float mPendingDarkIntensity;
    private ValueAnimator mTintAnimator;
    private Animator mColorTransitionAnimator;

    private final Handler mHandler;
    private boolean mTransitionDeferring;
    private long mTransitionDeferringStartTime;
    private long mTransitionDeferringDuration;

    private final ArraySet<String> mIconBlacklist = new ArraySet<>();

    private final Runnable mTransitionDeferringDoneRunnable = new Runnable() {
        @Override
        public void run() {
            mTransitionDeferring = false;
        }
    };

    public StatusBarIconController(Context context, View statusBar, View keyguardStatusBar,
            PhoneStatusBar phoneStatusBar) {
        mContext = context;
        mPhoneStatusBar = phoneStatusBar;
        mCarrierTextKeyguard = (CarrierText) keyguardStatusBar.findViewById(R.id.keyguard_carrier_text);
        mCyanideLogo = (ImageView) statusBar.findViewById(R.id.cyanide_logo);
        mCyanideLogoLeft = (ImageView) statusBar.findViewById(R.id.left_cyanide_logo);
        mCyanideLogoKeyguard = (ImageView) keyguardStatusBar.findViewById(R.id.cyanide_logo_keyguard);
        mCyanideLogoKeyguardLeft = (ImageView) keyguardStatusBar.findViewById(R.id.left_cyanide_logo_keyguard);
        mWeatherLayout = (StatusBarWeather) statusBar.findViewById(R.id.status_bar_weather_layout);
        mSystemIconArea = (LinearLayout) statusBar.findViewById(R.id.system_icon_area);
        mStatusIcons = (LinearLayout) statusBar.findViewById(R.id.statusIcons);
        mStatusIconsKeyguard = (LinearLayout) keyguardStatusBar.findViewById(R.id.statusIcons);
        mSignalCluster = (SignalClusterView) statusBar.findViewById(R.id.signal_cluster);
        mSignalClusterKeyguard = (SignalClusterView) keyguardStatusBar.findViewById(R.id.signal_cluster);
        mNotificationIcons = (IconMerger) statusBar.findViewById(R.id.notificationIcons);

        mNotificationIconAreaController = SystemUIFactory.getInstance()
                .createNotificationIconAreaController(context, phoneStatusBar, this);
        mNotificationIconAreaInner =
                mNotificationIconAreaController.getNotificationInnerAreaView();
        ViewGroup notificationIconArea =
                (ViewGroup) statusBar.findViewById(R.id.notification_icon_area);

        mSignalCluster.setIconController(this);
        mSignalClusterKeyguard.setIconController(this);
        notificationIconArea.addView(mNotificationIconAreaInner);

        mClockColor = StatusBarColorHelper.getClockColor(mContext);
        mTextColor = StatusBarColorHelper.getTextColor(mContext);
        mIconColor = StatusBarColorHelper.getIconColor(mContext);
        mLogoColor = StatusBarColorHelper.getLogoColor(mContext);
        

        mHandler = new Handler();
        mClockController = new ClockController(statusBar, mNotificationIcons, mHandler);
        mCenterClockLayout = statusBar.findViewById(R.id.center_clock_layout);
        defineSlots();
        loadDimens();

        TunerService.get(mContext).addTunable(this, ICON_BLACKLIST);
        mColorTransitionAnimator = createColorTransitionAnimator(0, 1);
    }

    public void setSignalCluster(SignalClusterView signalCluster) {
        mSignalCluster = signalCluster;
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        if (!ICON_BLACKLIST.equals(key)) {
            return;
        }
        mIconBlacklist.clear();
        mIconBlacklist.addAll(getIconBlacklist(newValue));
        ArrayList<StatusBarIconView> views = new ArrayList<StatusBarIconView>();
        // Get all the current views.
        for (int i = 0; i < mStatusIcons.getChildCount(); i++) {
            views.add((StatusBarIconView) mStatusIcons.getChildAt(i));
        }
        // Remove all the icons.
        for (int i = views.size() - 1; i >= 0; i--) {
            removeIcon(views.get(i).getSlot());
        }
        // Add them all back
        for (int i = 0; i < views.size(); i++) {
            setIcon(views.get(i).getSlot(), views.get(i).getStatusBarIcon());
        }
    }
    private void loadDimens() {
        mIconSize = mContext.getResources().getDimensionPixelSize(
                com.android.internal.R.dimen.status_bar_icon_size);
        mIconHPadding = mContext.getResources().getDimensionPixelSize(
                R.dimen.status_bar_icon_padding);
    }

    public void defineSlots() {
        defineSlots(mContext.getResources().getStringArray(
                com.android.internal.R.array.config_statusBarIcons));
    }

    private void addSystemIcon(int index, StatusBarIcon icon) {
        String slot = getSlot(index);
        int viewIndex = getViewIndex(index);
        boolean blocked = mIconBlacklist.contains(slot);
        StatusBarIconView view = new StatusBarIconView(mContext, slot, null, blocked);
        view.set(icon);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, mIconSize);
        lp.setMargins(mIconHPadding, 0, mIconHPadding, 0);
        mStatusIcons.addView(view, viewIndex, lp);

        view = new StatusBarIconView(mContext, slot, null, blocked);
        view.set(icon);
        mStatusIconsKeyguard.addView(view, viewIndex, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, mIconSize));
        applyIconTint();
    }

    public void setIcon(String slot, int resourceId, CharSequence contentDescription) {
        int index = getSlotIndex(slot);
        StatusBarIcon icon = getIcon(index);
        if (icon == null) {
            icon = new StatusBarIcon(UserHandle.SYSTEM, mContext.getPackageName(),
                    Icon.createWithResource(mContext, resourceId), 0, 0, contentDescription);
            setIcon(slot, icon);
        } else {
            icon.icon = Icon.createWithResource(mContext, resourceId);
            icon.contentDescription = contentDescription;
            handleSet(index, icon);
        }
    }

    public void setExternalIcon(String slot) {
        int viewIndex = getViewIndex(getSlotIndex(slot));
        int height = mContext.getResources().getDimensionPixelSize(
                R.dimen.status_bar_icon_drawing_size);
        ImageView imageView = (ImageView) mStatusIcons.getChildAt(viewIndex);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setAdjustViewBounds(true);
        setHeightAndCenter(imageView, height);
        imageView = (ImageView) mStatusIconsKeyguard.getChildAt(viewIndex);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setAdjustViewBounds(true);
        setHeightAndCenter(imageView, height);
    }

    private void setHeightAndCenter(ImageView imageView, int height) {
        ViewGroup.LayoutParams params = imageView.getLayoutParams();
        params.height = height;
        if (params instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) params).gravity = Gravity.CENTER_VERTICAL;
        }
        imageView.setLayoutParams(params);
    }

    public void setIcon(String slot, StatusBarIcon icon) {
        setIcon(getSlotIndex(slot), icon);
    }

    public void removeIcon(String slot) {
        int index = getSlotIndex(slot);
        removeIcon(index);
    }

    public void setIconVisibility(String slot, boolean visibility) {
        int index = getSlotIndex(slot);
        StatusBarIcon icon = getIcon(index);
        if (icon == null || icon.visible == visibility) {
            return;
        }
        icon.visible = visibility;
        handleSet(index, icon);
    }

    @Override
    public void removeIcon(int index) {
        if (getIcon(index) == null) {
            return;
        }
        super.removeIcon(index);
        int viewIndex = getViewIndex(index);
        mStatusIcons.removeViewAt(viewIndex);
        mStatusIconsKeyguard.removeViewAt(viewIndex);
    }

    @Override
    public void setIcon(int index, StatusBarIcon icon) {
        if (icon == null) {
            removeIcon(index);
            return;
        }
        boolean isNew = getIcon(index) == null;
        super.setIcon(index, icon);
        if (isNew) {
            addSystemIcon(index, icon);
        } else {
            handleSet(index, icon);
        }
    }

    private void handleSet(int index, StatusBarIcon icon) {
        int viewIndex = getViewIndex(index);
        StatusBarIconView view = (StatusBarIconView) mStatusIcons.getChildAt(viewIndex);
        view.set(icon);
        view = (StatusBarIconView) mStatusIconsKeyguard.getChildAt(viewIndex);
        view.set(icon);
        applyIconTint();
    }

    public void updateNotificationIcons(NotificationData notificationData) {
        mNotificationIconAreaController.updateNotificationIcons(notificationData);
    }

    public void hideSystemIconArea(boolean animate) {
        animateHide(mSystemIconArea, animate);
        animateHide(mCenterClockLayout, animate);
        if (mShowLogo && mLogoStyle == LOGO_LEFT) {
            animateHide(mCyanideLogoLeft, animate);
        }
    }

    public void showSystemIconArea(boolean animate) {
        animateShow(mSystemIconArea, animate);
        animateShow(mCenterClockLayout, animate);
        if (mShowLogo && mLogoStyle == LOGO_LEFT) {
            animateShow(mCyanideLogoLeft, animate);
        }
    }

    public void hideNotificationIconArea(boolean animate) {
        animateHide(mNotificationIconAreaInner, animate);
        animateHide(mCenterClockLayout, animate);
        if (mShowLogo && mLogoStyle == LOGO_LEFT) {
            animateHide(mCyanideLogoLeft, animate);
        }
        if (mShowLogo && mLogoStyle == LOGO_RIGHT) {
            animateHide(mCyanideLogo, animate);
        }
        if (mWeatherLayout.shouldShow()) {
            animateHide(mWeatherLayout, animate);
        }
    }

    public void showNotificationIconArea(boolean animate) {
        animateShow(mNotificationIconAreaInner, animate);
        animateShow(mCenterClockLayout, animate);
        if (mShowLogo && mLogoStyle == LOGO_LEFT) {
            animateShow(mCyanideLogoLeft, animate);
        }
        if (mShowLogo && mLogoStyle == LOGO_RIGHT) {
            animateShow(mCyanideLogo, animate);
        }
        if (mWeatherLayout.shouldShow()) {
            animateShow(mWeatherLayout, animate);
        }
    }

    public void setClockVisibility(boolean visible) {
        mClockController.setVisibility(visible);
    }

    public void setLogoVisibility(boolean showLogo, boolean forceHide, int maxAllowedIcons) {
        mShowLogo = showLogo;
        ContentResolver resolver = mContext.getContentResolver();
        boolean forceHideByNumberOfIcons = false;
        int notificationIconsCount = mNotificationIconAreaController.getNotificationIconsCount();
        mLogoStyle = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_CYANIDE_LOGO_STYLE, 1);
        if (forceHide && notificationIconsCount >= maxAllowedIcons) {
            forceHideByNumberOfIcons = true;
        }
        if (mLogoStyle == LOGO_LEFT) {
            mCyanideLogoLeft.setVisibility(showLogo && !forceHideByNumberOfIcons ? View.VISIBLE : View.GONE);
            mCyanideLogo.setVisibility(View.GONE);
        }
        if (mLogoStyle == LOGO_RIGHT) {
            mCyanideLogo.setVisibility(showLogo && !forceHideByNumberOfIcons ? View.VISIBLE : View.GONE);
            mCyanideLogoLeft.setVisibility(View.GONE);
        }
        showKeyguardLogo(mShowKeyguardLogo);
    }

    public void showKeyguardLogo(boolean show) {
        mShowKeyguardLogo = show;
        if (mLogoStyle == LOGO_LEFT) {
            mCyanideLogoKeyguardLeft.setVisibility(show ? View.VISIBLE : View.GONE);
            mCyanideLogoKeyguard.setVisibility(View.GONE);
        }
        if (mLogoStyle == LOGO_RIGHT) {
            mCyanideLogoKeyguard.setVisibility(show ? View.VISIBLE : View.GONE);
            mCyanideLogoKeyguardLeft.setVisibility(View.GONE);
        }
    }

    public void dump(PrintWriter pw) {
        int N = mStatusIcons.getChildCount();
        pw.println("  system icons: " + N);
        for (int i=0; i<N; i++) {
            StatusBarIconView ic = (StatusBarIconView) mStatusIcons.getChildAt(i);
            pw.println("    [" + i + "] icon=" + ic);
        }
    }

    public void dispatchDemoCommand(String command, Bundle args) {
        if (mDemoStatusIcons == null) {
            mDemoStatusIcons = new DemoStatusIcons(mStatusIcons, mIconSize);
        }
        mDemoStatusIcons.dispatchDemoCommand(command, args);
    }

    /**
     * Hides a view.
     */
    private void animateHide(final View v, boolean animate) {
        v.animate().cancel();
        if (!animate) {
            v.setAlpha(0f);
            v.setVisibility(View.INVISIBLE);
            return;
        }
        v.animate()
                .alpha(0f)
                .setDuration(160)
                .setStartDelay(0)
                .setInterpolator(Interpolators.ALPHA_OUT)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        v.setVisibility(View.INVISIBLE);
                    }
                });
    }

    /**
     * Shows a view, and synchronizes the animation with Keyguard exit animations, if applicable.
     */
    private void animateShow(View v, boolean animate) {
        v.animate().cancel();
        v.setVisibility(View.VISIBLE);
        if (!animate) {
            v.setAlpha(1f);
            return;
        }
        v.animate()
                .alpha(1f)
                .setDuration(320)
                .setInterpolator(Interpolators.ALPHA_IN)
                .setStartDelay(50)

                // We need to clean up any pending end action from animateHide if we call
                // both hide and show in the same frame before the animation actually gets started.
                // cancel() doesn't really remove the end action.
                .withEndAction(null);

        // Synchronize the motion with the Keyguard fading if necessary.
        if (mPhoneStatusBar.isKeyguardFadingAway()) {
            v.animate()
                    .setDuration(mPhoneStatusBar.getKeyguardFadingAwayDuration())
                    .setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN)
                    .setStartDelay(mPhoneStatusBar.getKeyguardFadingAwayDelay())
                    .start();
        }
    }

    /**
     * Sets the dark area so {@link #setIconsDark} only affects the icons in the specified area.
     *
     * @param darkArea the area in which icons should change it's tint, in logical screen
     *                 coordinates
     */
    public void setIconsDarkArea(Rect darkArea) {
        if (darkArea == null && mTintArea.isEmpty()) {
            return;
        }
        if (darkArea == null) {
            mTintArea.setEmpty();
        } else {
            mTintArea.set(darkArea);
        }
        applyIconTint();
        mNotificationIconAreaController.setTintArea(darkArea);
    }

    public void setIconsDark(boolean dark, boolean animate) {
        if (!animate) {
            setIconTintInternal(dark ? 1.0f : 0.0f);
        } else if (mTransitionPending) {
            deferIconTintChange(dark ? 1.0f : 0.0f);
        } else if (mTransitionDeferring) {
            animateIconTint(dark ? 1.0f : 0.0f,
                    Math.max(0, mTransitionDeferringStartTime - SystemClock.uptimeMillis()),
                    mTransitionDeferringDuration);
        } else {
            animateIconTint(dark ? 1.0f : 0.0f, 0 /* delay */, DEFAULT_TINT_ANIMATION_DURATION);
        }
    }

    private void animateIconTint(float targetDarkIntensity, long delay,
            long duration) {
        if (mTintAnimator != null) {
            mTintAnimator.cancel();
        }
        if (mDarkIntensity == targetDarkIntensity) {
            return;
        }
        mTintAnimator = ValueAnimator.ofFloat(mDarkIntensity, targetDarkIntensity);
        mTintAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setIconTintInternal((Float) animation.getAnimatedValue());
            }
        });
        mTintAnimator.setDuration(duration);
        mTintAnimator.setStartDelay(delay);
        mTintAnimator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        mTintAnimator.start();
    }

    private void setIconTintInternal(float darkIntensity) {
        mDarkIntensity = darkIntensity;
        mClockColor = (int) ArgbEvaluator.getInstance().evaluate(darkIntensity,
                StatusBarColorHelper.getClockColor(mContext),
                StatusBarColorHelper.getClockColorDarkMode(mContext));
        mTextColor = (int) ArgbEvaluator.getInstance().evaluate(darkIntensity,
                StatusBarColorHelper.getTextColor(mContext),
                StatusBarColorHelper.getTextColorDarkMode(mContext));
        mIconColor = (int) ArgbEvaluator.getInstance().evaluate(darkIntensity,
                StatusBarColorHelper.getIconColor(mContext),
                StatusBarColorHelper.getIconColorDarkMode(mContext));
        mLogoColor = (int) ArgbEvaluator.getInstance().evaluate(darkIntensity,
                StatusBarColorHelper.getLogoColor(mContext),
                StatusBarColorHelper.getLogoColorDarkMode(mContext));

        mNotificationIconAreaController.setIconTint(mIconColor);
        applyIconTint();
    }

    private void deferIconTintChange(float darkIntensity) {
        if (mTintChangePending && darkIntensity == mPendingDarkIntensity) {
            return;
        }
        mTintChangePending = true;
        mPendingDarkIntensity = darkIntensity;
    }

    /**
     * @return the tint to apply to {@param view} depending on the desired tint {@param color} and
     *         the screen {@param tintArea} in which to apply that tint
     */
    public int getTint(Rect tintArea, View view, int color) {
        if (isInArea(tintArea, view) || mDarkIntensity == 0f) {
            return color;
        } else {
            return StatusBarColorHelper.getIconColor(mContext);
        }
    }

    private int getTextTint(Rect tintArea, View view, int color) {
        if (isInArea(tintArea, view) || mDarkIntensity == 0f) {
            return color;
        } else {
            return StatusBarColorHelper.getTextColor(mContext);
        }
    }

    private int getClockTint(Rect tintArea, ClockController clock, int color) {
        if (isClockInArea(tintArea, clock) || mDarkIntensity == 0f) {
            return color;
        } else {
            return StatusBarColorHelper.getClockColor(mContext);
        }
    }

    private int getLogoTint(Rect tintArea, View view, int color) {
        if (isInArea(tintArea, view) || mDarkIntensity == 0f) {
            return color;
        } else {
            return StatusBarColorHelper.getLogoColor(mContext);
        }
    }

    /**
     * @return the dark intensity to apply to {@param view} depending on the desired dark
     *         {@param intensity} and the screen {@param tintArea} in which to apply that intensity
     */
    public static float getDarkIntensity(Rect tintArea, View view, float intensity) {
        if (isInArea(tintArea, view)) {
            return intensity;
        } else {
            return 0f;
        }
    }

    /**
     * @return true if more than half of the {@param view} area are in {@param area}, false
     *         otherwise
     */
    private static boolean isInArea(Rect area, View view) {
        if (area.isEmpty()) {
            return true;
        }
        sTmpRect.set(area);
        view.getLocationOnScreen(sTmpInt2);
        int left = sTmpInt2[0];

        int intersectStart = Math.max(left, area.left);
        int intersectEnd = Math.min(left + view.getWidth(), area.right);
        int intersectAmount = Math.max(0, intersectEnd - intersectStart);

        boolean coversFullStatusBar = area.top <= 0;
        boolean majorityOfWidth = 2 * intersectAmount > view.getWidth();
        return majorityOfWidth && coversFullStatusBar;
    }

    private static boolean isClockInArea(Rect area, ClockController clock) {
        if (area.isEmpty()) {
            return true;
        }
        sTmpRect.set(area);
        int left = sTmpInt2[0];

        int intersectStart = Math.max(left, area.left);
        int intersectEnd = Math.min(left, area.right);
        int intersectAmount = Math.max(0, intersectEnd - intersectStart);

        boolean coversFullStatusBar = area.top <= 0;
        boolean majorityOfWidth = 2 * intersectAmount > 0;
        return majorityOfWidth && coversFullStatusBar;
    }

    private void applyIconTint() {
        mWeatherLayout.setTextColor(getTextTint(mTintArea, mWeatherLayout, mTextColor));
        mWeatherLayout.setIconColor(getTint(mTintArea, mWeatherLayout, mIconColor));
        for (int i = 0; i < mStatusIcons.getChildCount(); i++) {
            StatusBarIconView v = (StatusBarIconView) mStatusIcons.getChildAt(i);
            v.setImageTintList(ColorStateList.valueOf(getTint(mTintArea, v, mIconColor)));
        }
        applyStatusIconKeyguardTint();
        mSignalCluster.setIconTint(mIconColor, StatusBarColorHelper.getIconColorDarkMode(mContext),
                mDarkIntensity, mTintArea);
        mClockController.setTextColor(getClockTint(mTintArea, mClockController, mClockColor));
        if (mShowLogo && mLogoStyle == LOGO_LEFT) {
            mCyanideLogoLeft.setImageTintList(ColorStateList.valueOf(getLogoTint(mTintArea, mCyanideLogo, mLogoColor)));
        }
        if (mShowLogo && mLogoStyle == LOGO_RIGHT) {
            mCyanideLogo.setImageTintList(ColorStateList.valueOf(getLogoTint(mTintArea, mCyanideLogo, mLogoColor)));
        }
    }

    public void appTransitionPending() {
        mTransitionPending = true;
    }

    public void appTransitionCancelled() {
        if (mTransitionPending && mTintChangePending) {
            mTintChangePending = false;
            animateIconTint(mPendingDarkIntensity, 0 /* delay */, DEFAULT_TINT_ANIMATION_DURATION);
        }
        mTransitionPending = false;
    }

    public void appTransitionStarting(long startTime, long duration) {
        if (mTransitionPending && mTintChangePending) {
            mTintChangePending = false;
            animateIconTint(mPendingDarkIntensity,
                    Math.max(0, startTime - SystemClock.uptimeMillis()),
                    duration);

        } else if (mTransitionPending) {

            // If we don't have a pending tint change yet, the change might come in the future until
            // startTime is reached.
            mTransitionDeferring = true;
            mTransitionDeferringStartTime = startTime;
            mTransitionDeferringDuration = duration;
            mHandler.removeCallbacks(mTransitionDeferringDoneRunnable);
            mHandler.postAtTime(mTransitionDeferringDoneRunnable, startTime);
        }
        mTransitionPending = false;
    }

    public static ArraySet<String> getIconBlacklist(String blackListStr) {
        ArraySet<String> ret = new ArraySet<String>();
        if (blackListStr == null) {
            blackListStr = "rotate,headset";
        }
        String[] blacklist = blackListStr.split(",");
        for (String slot : blacklist) {
            if (!TextUtils.isEmpty(slot)) {
                ret.add(slot);
            }
        }
        return ret;
    }

    public void onDensityOrFontScaleChanged() {
        loadDimens();
        mWeatherLayout.onDensityOrFontScaleChanged();
        mNotificationIconAreaController.onDensityOrFontScaleChanged(mContext);
        updateClock();
        for (int i = 0; i < mStatusIcons.getChildCount(); i++) {
            View child = mStatusIcons.getChildAt(i);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, mIconSize);
            lp.setMargins(mIconHPadding, 0, mIconHPadding, 0);
            child.setLayoutParams(lp);
        }
        for (int i = 0; i < mStatusIconsKeyguard.getChildCount(); i++) {
            View child = mStatusIconsKeyguard.getChildAt(i);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, mIconSize);
            child.setLayoutParams(lp);
        }
    }

    private void updateClock() {
        mClockController.updateFontSize();
    }

    private ValueAnimator createColorTransitionAnimator(float start, float end) {
        ValueAnimator animator = ValueAnimator.ofFloat(start, end);

        animator.setDuration(600);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
            @Override public void onAnimationUpdate(ValueAnimator animation) {
                float position = animation.getAnimatedFraction();
                if (mAnimateClockColor) {
                    final int blended = ColorHelper.getBlendColor(mClockColor,
                            StatusBarColorHelper.getClockColor(mContext), position);
                    mClockController.setTextColor(blended);
                }
                if (mAnimateTextColor) {
                    final int blended = ColorHelper.getBlendColor(mTextColor,
                            StatusBarColorHelper.getTextColor(mContext), position);
                    mWeatherLayout.setTextColor(blended);
                }
                if (mAnimateIconColor) {
                    final int blended = ColorHelper.getBlendColor(mIconColor,
                            StatusBarColorHelper.getIconColor(mContext), position);
                    mWeatherLayout.setIconColor(blended);
                    for (int i = 0; i < mStatusIcons.getChildCount(); i++) {
                        StatusBarIconView v = (StatusBarIconView) mStatusIcons.getChildAt(i);
                        v.setImageTintList(ColorStateList.valueOf(blended));
                    }
                    mSignalCluster.setIconTint(blended, 0, mDarkIntensity, mTintArea);
                    mNotificationIconAreaController.setIconTint(blended);
                }
                if (mAnimateLogoColor) {
                    final int blended = ColorHelper.getBlendColor(mLogoColor,
                            StatusBarColorHelper.getLogoColor(mContext), position);
                    if (mLogoStyle == LOGO_LEFT) {
                        mCyanideLogoLeft.setImageTintList(ColorStateList.valueOf(blended));
                    }
                    if (mLogoStyle == LOGO_RIGHT) {
                        mCyanideLogoLeft.setImageTintList(ColorStateList.valueOf(blended));
                    }
                }
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mAnimateClockColor) {
                    mClockColor = StatusBarColorHelper.getClockColor(mContext);
                    mAnimateClockColor = false;
                }
                if (mAnimateTextColor) {
                    mTextColor = StatusBarColorHelper.getTextColor(mContext);
                    mAnimateTextColor = false;
                }
                if (mAnimateIconColor) {
                    mIconColor = StatusBarColorHelper.getIconColor(mContext);
                    mAnimateIconColor = false;
                }
                if (mAnimateLogoColor) {
                    mLogoColor = StatusBarColorHelper.getLogoColor(mContext);
                    mAnimateLogoColor = false;
                }
            }
        });
        return animator;
    }

    public void updateTextColor(boolean animate) {
        mAnimateTextColor = animate;
        mCarrierTextKeyguard.setTextColor(StatusBarColorHelper.getTextColor(mContext));
    }

    public void updateIconColor(boolean animate) {
        mAnimateIconColor = animate;
        if (!mAnimateClockColor && !mAnimateLogoColor && mAnimateIconColor) {
            mColorTransitionAnimator.start();
        }
        applyStatusIconKeyguardTint();
        mSignalClusterKeyguard.setIconTint(StatusBarColorHelper.getIconColor(mContext), 0,
                mDarkIntensity, new Rect());
    }

    public void updateClockColor(boolean animate) {
        mAnimateClockColor = animate;
        if (!mAnimateIconColor &&  !mAnimateLogoColor && mAnimateClockColor) {
            mColorTransitionAnimator.start();
        }
        mClockController.setTextColor(StatusBarColorHelper.getClockColor(mContext));
    }

    public void updateLogoColor(boolean animate) {
        mAnimateLogoColor = animate;
        if (!mAnimateIconColor && !mAnimateClockColor && mAnimateLogoColor) {
            mColorTransitionAnimator.start();
        }
        mCyanideLogoLeft.setImageTintList(ColorStateList.valueOf(StatusBarColorHelper.getLogoColor(mContext)));
        mCyanideLogo.setImageTintList(ColorStateList.valueOf(StatusBarColorHelper.getLogoColor(mContext)));
        updateKeyguardLogoColor();
    }

    private void updateKeyguardLogoColor() {
        mCyanideLogoKeyguardLeft.setImageTintList(ColorStateList.valueOf(StatusBarColorHelper.getLogoColor(mContext)));
        mCyanideLogoKeyguard.setImageTintList(ColorStateList.valueOf(StatusBarColorHelper.getLogoColor(mContext)));
    }

    public void updateWeatherVisibility(boolean show, boolean forceHide, int maxAllowedIcons) {
        boolean forceHideByNumberOfIcons = false;
        int notificationIconsCount = mNotificationIconAreaController.getNotificationIconsCount();
        if (forceHide && notificationIconsCount >= maxAllowedIcons) {
            forceHideByNumberOfIcons = true;
        }
        mWeatherLayout.setShow(show && !forceHideByNumberOfIcons);
    }

    public void updateWeatherType(int type) {
        mWeatherLayout.setType(type);
    }

    private void applyStatusIconKeyguardTint() {
        for (int i = 0; i < mStatusIconsKeyguard.getChildCount(); i++) {
            StatusBarIconView v = (StatusBarIconView) mStatusIconsKeyguard.getChildAt(i);
            v.setImageTintList(ColorStateList.valueOf(StatusBarColorHelper.getIconColor(mContext)));
        }
    }
}
