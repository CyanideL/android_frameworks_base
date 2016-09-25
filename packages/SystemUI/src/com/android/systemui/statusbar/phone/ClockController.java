package com.android.systemui.statusbar.phone;

import com.android.systemui.FontSizeUtils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.View;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.Clock;

import com.android.internal.util.cyanide.FontHelper;
import com.android.internal.util.cyanide.StatusBarColorHelper;

/**
 * To control your...clock
 */
public class ClockController {

    public static final int STYLE_HIDE_CLOCK    = 0;
    public static final int STYLE_CLOCK_RIGHT   = 1;
    public static final int STYLE_CLOCK_CENTER  = 2;
    public static final int STYLE_CLOCK_LEFT    = 3;

    public static final int DEFAULT_ICON_TINT = Color.WHITE;

    private final IconMerger mNotificationIcons;
    private final Context mContext;
    private final SettingsObserver mSettingsObserver;
    private Clock mRightClock, mCenterClock, mLeftClock, mActiveClock;

    public static int mClockLocation;
    private int mAmPmStyle;
    private int mClockDateStyle;
    private int mClockDateDisplay;
    private static Typeface mClockFontStyle;
    private int mIconTint = DEFAULT_ICON_TINT;
    private final Rect mTintArea = new Rect();
    private int mClockFontSize = 14;

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_AM_PM),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_CLOCK),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_CLOCK_DATE_DISPLAY),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_CLOCK_DATE_STYLE),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_CLOCK_DATE_FORMAT),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUSBAR_CLOCK_FONT_STYLE),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_CLOCK_FONT_SIZE),
                    false, this, UserHandle.USER_ALL);
            updateSettings();
        }

        void unobserve() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    public ClockController(View statusBar, IconMerger notificationIcons, Handler handler) {
        mRightClock = (Clock) statusBar.findViewById(R.id.clock);
        mCenterClock = (Clock) statusBar.findViewById(R.id.center_clock);
        mLeftClock = (Clock) statusBar.findViewById(R.id.left_clock);
        mNotificationIcons = notificationIcons;
        mContext = statusBar.getContext();

        mActiveClock = mRightClock;
        mSettingsObserver = new SettingsObserver(handler);
        mSettingsObserver.observe();
    }

    private Clock getClockForCurrentLocation() {
        Clock clockForAlignment;
        switch (mClockLocation) {
            case STYLE_CLOCK_CENTER:
                clockForAlignment = mCenterClock;
                break;
            case STYLE_CLOCK_LEFT:
                clockForAlignment = mLeftClock;
                break;
            case STYLE_CLOCK_RIGHT:
            case STYLE_HIDE_CLOCK:
            default:
                clockForAlignment = mRightClock;
                break;
        }
        return clockForAlignment;
    }

    private void updateActiveClock() {
        mActiveClock.setVisibility(View.GONE);
        if (mClockLocation == STYLE_HIDE_CLOCK) {
            return;
        }

        mActiveClock = getClockForCurrentLocation();
        mActiveClock.setVisibility(View.VISIBLE);
        mActiveClock.setAmPmStyle(mAmPmStyle);
        mActiveClock.setClockDateDisplay(mClockDateDisplay);
        mActiveClock.setClockDateStyle(mClockDateStyle);
        mActiveClock.setTypeface(mClockFontStyle);
        mActiveClock.setTextSize(mClockFontSize);

        setClockAndDateStatus();
        setTextColor(mIconTint);
        updateFontSize();
    }

    private void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();
        mAmPmStyle = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_AM_PM, Clock.AM_PM_STYLE_GONE,
                UserHandle.USER_CURRENT);
        mClockLocation = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_CLOCK, STYLE_CLOCK_RIGHT,
                UserHandle.USER_CURRENT);
        mClockDateDisplay = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_CLOCK_DATE_DISPLAY, Clock.CLOCK_DATE_DISPLAY_GONE,
                UserHandle.USER_CURRENT);
        mClockDateStyle = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_CLOCK_DATE_STYLE, Clock.CLOCK_DATE_STYLE_REGULAR,
                UserHandle.USER_CURRENT);
        mClockFontSize = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_CLOCK_FONT_SIZE, 14,
                UserHandle.USER_CURRENT);
        updateActiveClock();
        updateClockFontStyles();
    }

    private void setClockAndDateStatus() {
        if (mNotificationIcons != null) {
            mNotificationIcons.setClockAndDateStatus(mClockLocation);
        }
    }

    public void setVisibility(boolean visible) {
        if (mActiveClock != null) {
            mActiveClock.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    public void setTintArea(Rect tintArea) {
        if (tintArea == null) {
            mTintArea.setEmpty();
        } else {
            mTintArea.set(tintArea);
        }
        //applyClockTint();
    }

    public void setTextColor(int iconTint) {
        mIconTint = iconTint;
        if (mActiveClock != null) {
            mActiveClock.setTextColor(iconTint);
        }
        //applyClockTint();
    }

    public void updateFontSize() {
        if (mActiveClock != null) {
            FontSizeUtils.updateFontSize(mActiveClock, R.dimen.status_bar_clock_size);
            mActiveClock.setPaddingRelative(
                    mContext.getResources().getDimensionPixelSize(
                            R.dimen.status_bar_clock_starting_padding),
                    0,
                    mContext.getResources().getDimensionPixelSize(
                        R.dimen.status_bar_clock_end_padding),
                    0);
        }
    }

    /*private void applyClockTint() {
        mStatusBarIconController.getTint(mTintArea, mActiveClock, mClockColor);
    }*/

    private void updateClockFontStyles() {
        final int mFontStyle = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUSBAR_CLOCK_FONT_STYLE, FontHelper.FONT_NORMAL);

        getFontStyle(mFontStyle);
        updateActiveClock();
    }

    public static void getFontStyle(int font) {
        switch (font) {
            case FontHelper.FONT_NORMAL:
            default:
                mClockFontStyle = FontHelper.NORMAL;
                break;
            case FontHelper.FONT_ITALIC:
                mClockFontStyle = FontHelper.ITALIC;
                break;
            case FontHelper.FONT_BOLD:
                mClockFontStyle = FontHelper.BOLD;
                break;
            case FontHelper.FONT_BOLD_ITALIC:
                mClockFontStyle = FontHelper.BOLD_ITALIC;
                break;
            case FontHelper.FONT_LIGHT:
                mClockFontStyle = FontHelper.LIGHT;
                break;
            case FontHelper.FONT_LIGHT_ITALIC:
                mClockFontStyle = FontHelper.LIGHT_ITALIC;
                break;
            case FontHelper.FONT_THIN:
                mClockFontStyle = FontHelper.THIN;
                break;
            case FontHelper.FONT_THIN_ITALIC:
                mClockFontStyle = FontHelper.THIN_ITALIC;
                break;
            case FontHelper.FONT_CONDENSED:
                mClockFontStyle = FontHelper.CONDENSED;
                break;
            case FontHelper.FONT_CONDENSED_ITALIC:
                mClockFontStyle = FontHelper.CONDENSED_ITALIC;
                break;
            case FontHelper.FONT_CONDENSED_LIGHT:
                mClockFontStyle = FontHelper.CONDENSED_LIGHT;
                break;
            case FontHelper.FONT_CONDENSED_LIGHT_ITALIC:
                mClockFontStyle = FontHelper.CONDENSED_LIGHT_ITALIC;
                break;
            case FontHelper.FONT_CONDENSED_BOLD:
                mClockFontStyle = FontHelper.CONDENSED_BOLD;
                break;
            case FontHelper.FONT_CONDENSED_BOLD_ITALIC:
                mClockFontStyle = FontHelper.CONDENSED_BOLD_ITALIC;
                break;
            case FontHelper.FONT_MEDIUM:
                mClockFontStyle = FontHelper.MEDIUM;
                break;
            case FontHelper.FONT_MEDIUM_ITALIC:
                mClockFontStyle = FontHelper.MEDIUM_ITALIC;
                break;
            case FontHelper.FONT_BLACK:
                mClockFontStyle = FontHelper.BLACK;
                break;
            case FontHelper.FONT_BLACK_ITALIC:
                mClockFontStyle = FontHelper.BLACK_ITALIC;
                break;
            case FontHelper.FONT_DANCINGSCRIPT:
                mClockFontStyle = FontHelper.DANCINGSCRIPT;
                break;
            case FontHelper.FONT_DANCINGSCRIPT_BOLD:
                mClockFontStyle = FontHelper.DANCINGSCRIPT_BOLD;
                break;
            case FontHelper.FONT_COMINGSOON:
                mClockFontStyle = FontHelper.COMINGSOON;
                break;
            case FontHelper.FONT_NOTOSERIF:
                mClockFontStyle = FontHelper.NOTOSERIF;
                break;
            case FontHelper.FONT_NOTOSERIF_ITALIC:
                mClockFontStyle = FontHelper.NOTOSERIF_ITALIC;
                break;
            case FontHelper.FONT_NOTOSERIF_BOLD:
                mClockFontStyle = FontHelper.NOTOSERIF_BOLD;
                break;
            case FontHelper.FONT_NOTOSERIF_BOLD_ITALIC:
                mClockFontStyle = FontHelper.NOTOSERIF_BOLD_ITALIC;
                break;
        }
    }
}
