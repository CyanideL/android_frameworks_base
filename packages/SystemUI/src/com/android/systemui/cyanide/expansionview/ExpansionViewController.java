/*
 * Copyright (C) 2016 Brett Rogers (rogersb11)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.cyanide.expansionview;

import android.content.Context;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.graphics.PorterDuff.Mode;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.View;

import com.android.internal.util.cyanide.ExpansionViewColorHelper;
import com.android.internal.util.cyanide.ExpansionViewTextHelper;
import com.android.internal.util.cyanide.FontHelper;
import com.android.internal.util.cyanide.WeatherServiceController;
// SystemUI Classes
import com.android.systemui.cyanide.PanelShortcuts;
import com.android.systemui.cyanide.expansionview.panels.ExpansionViewCustomPanel;
import com.android.systemui.cyanide.expansionview.panels.ExpansionViewActivityPanel;
import com.android.systemui.cyanide.expansionview.panels.ExpansionViewWeatherPanel;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.NetworkController;

public class ExpansionViewController {
    private static Context mContext;

    private Handler mHandler;
    private SettingsObserver mSettingsObserver;
    private static ContentResolver mResolver;
    public static Typeface mFontStyle;

    private final View mExpansionViewContainer;
    private ExpansionViewCustomPanel mExpansionViewCustomPanel;
    private ExpansionViewActivityPanel mExpansionViewActivityPanel;
    private ExpansionViewWeatherPanel mExpansionViewWeatherPanel;
    private PanelShortcuts mExpansionViewPanelShortcuts;
    private View mExpansionViewLogoPanel;

    public ExpansionViewController(Context context, View expansionViewContainer) {
        mContext = context;
        mResolver = mContext.getContentResolver();
        mHandler = new Handler();
        mSettingsObserver = new SettingsObserver(mHandler);

        mExpansionViewContainer = expansionViewContainer;
        mExpansionViewActivityPanel =
                (ExpansionViewActivityPanel) mExpansionViewContainer.findViewById(R.id.expansion_view_activity_panel);
        mExpansionViewCustomPanel = (ExpansionViewCustomPanel) mExpansionViewContainer.findViewById(R.id.expansion_view_controller_container);
        mExpansionViewWeatherPanel = (ExpansionViewWeatherPanel) mExpansionViewContainer.findViewById(R.id.expansion_view_weather_container);
        mExpansionViewLogoPanel = mExpansionViewContainer.findViewById(R.id.expansion_view_logo_panel);
        mExpansionViewPanelShortcuts = (PanelShortcuts) mExpansionViewContainer.findViewById(R.id.shade_bar);

    }

    public void setUp(PhoneStatusBar statusBar, NetworkController network,
            /*BatteryController battery,*/ WeatherServiceController weather) {
        mExpansionViewCustomPanel.setUp(statusBar);
        mExpansionViewActivityPanel.setUp(statusBar, network/*, battery*/);
        mExpansionViewWeatherPanel.setUp(statusBar, weather);

        //setupExpansionViewBatteryOptions();

    }

    /*private void setupExpansionViewBatteryOptions() {
        setExpansionViewBatteryIndicator();
        setExpansionViewBatteryIconTextVisibility();
        setExpansionViewBatteryIconCircleDots();
        setExpansionViewBatteryShowChargeAnimation();
        setExpansionViewBatteryIconCutOutText();
        setExpansionViewBatteryIconColor();
        setExpansionViewBatteryTextColor();
    }

    private void setExpansionViewBatteryIndicator() {
        final int indicator = Settings.System.getInt(mResolver,
                Settings.System.EXPANSION_VIEW_BATTERY_ICON_INDICATOR, 0);
        mExpansionViewActivityPanel.setBatteryIndicator(indicator);
    }

    private void setExpansionViewBatteryIconTextVisibility() {
        final boolean show = Settings.System.getInt(mResolver,
                Settings.System.EXPANSION_VIEW_BATTERY_SHOW_TEXT, 0) == 1;
        mExpansionViewActivityPanel.setBatteryTextVisibility(show);
    }

    private void setExpansionViewBatteryIconCircleDots() {
        final int interval = Settings.System.getInt(mResolver,
                Settings.System.EXPANSION_VIEW_BATTERY_CIRCLE_DOT_INTERVAL, 0);
        final int length = Settings.System.getInt(mResolver,
                Settings.System.EXPANSION_VIEW_BATTERY_CIRCLE_DOT_LENGTH, 0);
        mExpansionViewActivityPanel.setBatteryCircleDots(interval, length);
    }

    private void setExpansionViewBatteryShowChargeAnimation() {
        final boolean show = Settings.System.getInt(mResolver,
                Settings.System.EXPANSION_VIEW_BATTERY_SHOW_CHARGE_ANIMATION, 0) == 1;
        mExpansionViewActivityPanel.setBatteryShowChargeAnimation(show);
    }

    private void setExpansionViewBatteryIconCutOutText() {
        final boolean cutOut = Settings.System.getInt(mResolver,
                Settings.System.EXPANSION_VIEW_BATTERY_CUT_OUT_TEXT, 1) == 1;
        mExpansionViewActivityPanel.setBatteryCutOutBatteryText(cutOut);
    }

    private void setExpansionViewBatteryIconColor() {
        mExpansionViewActivityPanel.setBatteryIconColor();
    }

    private void setExpansionViewBatteryTextColor() {
        final int color = ExpansionViewColorHelper.getBatteryTextColor(mContext);
        mExpansionViewActivityPanel.setBatteryTextColor(color);
    }*/

    private void setExpansionViewActivityPanelTextSize() {
        int mExpansionViewActivityPanelFontSize = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.EXPANSION_VIEW_ACTIVITY_PANEL_TEXT_SIZE, 14);

        if (mExpansionViewActivityPanel != null) {
            mExpansionViewActivityPanel.setTextSize(mExpansionViewActivityPanelFontSize);
        }
    }

    private void setExpansionViewCustomLogoImage() {

        if (mExpansionViewCustomPanel != null) {
            mExpansionViewCustomPanel.updateBackgroundImage();
        }
    }

    private void setExpansionViewShowShortcutBar() {
        final boolean showShortcuts = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.EXPANSION_VIEW_PANEL_SHORTCUTS, 1) == 1;

        if (mExpansionViewCustomPanel != null) {
            mExpansionViewCustomPanel.showShortcutPanel(showShortcuts);
        }
    }

    private void setExpansionViewActivityPanelTextColor() {
        int color = ExpansionViewColorHelper.getExpansionViewActivityPanelTextColor(mContext);
        if (mExpansionViewActivityPanel != null) {
            mExpansionViewActivityPanel.setTextColor(color);
        }
    }

    private void setExpansionViewTextColor() {
        int color = ExpansionViewColorHelper.getExpansionViewTextColor(mContext);
        if (mExpansionViewCustomPanel != null) {
            mExpansionViewCustomPanel.setTextColor(color);
        }
        if (mExpansionViewActivityPanel != null) {
            mExpansionViewActivityPanel.setTextColor(color);
        }
    }

    private void setExpansionViewIconColor() {
        int color = ExpansionViewColorHelper.getExpansionViewIconColor(mContext);
        if (mExpansionViewCustomPanel != null) {
            mExpansionViewCustomPanel.updateIconColor(color);
        }
    }

    private void setExpansionViewWeatherColors() {
        if (mExpansionViewWeatherPanel != null) {
            mExpansionViewWeatherPanel.updateItems();
        }
    }

    private void setExpansionViewFontStyle() {
        final int mExpansionViewFontStyle = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.EXPANSION_VIEW_FONT_STYLE, 0);

        getExpansionViewFontStyle(mExpansionViewFontStyle);
        mExpansionViewWeatherPanel.updateFontStyle();
    }

    public void getExpansionViewFontStyle(int font) {
        if (mExpansionViewCustomPanel == null) return;
        if (mExpansionViewActivityPanel == null) return;
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
        mExpansionViewCustomPanel.setTypeface(mFontStyle);
        mExpansionViewActivityPanel.setTypeface(mFontStyle);
    }

    private void setExpansionViewText() {
        String expansionViewText = Settings.System.getString(mContext.getContentResolver(),
                Settings.System.EXPANSION_VIEW_TEXT_CUSTOM);

        if (expansionViewText == null || expansionViewText.isEmpty()) {
            expansionViewText = ExpansionViewTextHelper.getDefaultExpansionViewText(mContext);
        }
        if (mExpansionViewCustomPanel != null) {
            mExpansionViewCustomPanel.setCustomText(expansionViewText);
        }
    }

    private void setExpansionViewTextSize() {
        int mExpansionViewFontSize = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.EXPANSION_VIEW_TEXT_SIZE, 20);

        if (mExpansionViewCustomPanel != null) {
            mExpansionViewCustomPanel.setTextSize(mExpansionViewFontSize);
        }
    }

    private void setExpansionViewWeatherPanelItems() {
        if (mExpansionViewWeatherPanel != null) {
            mExpansionViewWeatherPanel.updateItems();
        }
    }

    private void setExpansionViewWeatherTextSize() {
        if (mExpansionViewWeatherPanel != null) {
            mExpansionViewWeatherPanel.setExpansionViewWeatherTextSize();
        }
    }

    private void setExpansionViewRipple() {
        if (mExpansionViewWeatherPanel != null) {
            mExpansionViewWeatherPanel.setRippleColor();
        }
        if (mExpansionViewCustomPanel != null) {
            mExpansionViewCustomPanel.setRippleColor();
        }
        if (mExpansionViewActivityPanel != null) {
            mExpansionViewActivityPanel.setRippleColor();
        }
    }

    private void setExpansionViewVibration() {
        final boolean vibrate = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.EXPANSION_VIEW_VIBRATION, 0) == 1;

        if (mExpansionViewCustomPanel != null) {
            mExpansionViewCustomPanel.vibrateOnClick(vibrate);
        }

        if (mExpansionViewActivityPanel != null) {
            mExpansionViewActivityPanel.vibrateOnClick(vibrate);
        }

        if (mExpansionViewPanelShortcuts != null) {
            mExpansionViewPanelShortcuts.vibrateOnClick(vibrate);
        }

        if (mExpansionViewWeatherPanel != null) {
            mExpansionViewWeatherPanel.vibrateOnClick(vibrate);
        }
    }

    private void setExpansionViewPanelVisibility() {
        if (mExpansionViewCustomPanel != null) {
            mExpansionViewCustomPanel.updatePanelViews();
        }
    }

    private void setExpansionViewStroke() {
        if (mExpansionViewCustomPanel != null) {
            mExpansionViewCustomPanel.setStroke();
        }
    }

    private void setExpansionViewBgOrientation() {
        if (mExpansionViewCustomPanel != null) {
            mExpansionViewCustomPanel.updateBgGradientOrientation();
        }
    }

    public void setObserving(boolean observe) {
        if (observe) {
            mSettingsObserver.observe();
        } else {
            mSettingsObserver.unobserve();
        }
    }

    public void setListening(boolean listening) {
        mExpansionViewActivityPanel.setListening(listening);
        mExpansionViewWeatherPanel.setListening(listening);
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_ACTIVITY_PANEL_TEXT_SIZE),
                    false, this, UserHandle.USER_ALL);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_ACTIVITY_PANEL_TEXT_COLOR),
                    false, this, UserHandle.USER_ALL);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_CUSTOM_LOGO),
                    false, this, UserHandle.USER_ALL);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_PANEL_SHORTCUTS),
                    false, this, UserHandle.USER_ALL);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_TEXT_COLOR),
                    false, this, UserHandle.USER_ALL);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_ICON_COLOR),
                    false, this, UserHandle.USER_ALL);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_FONT_STYLE),
                    false, this, UserHandle.USER_ALL);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_TEXT_SIZE),
                    false, this, UserHandle.USER_ALL);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_TEXT_CUSTOM),
                    false, this, UserHandle.USER_ALL);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_RIPPLE_COLOR),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_WEATHER_SHOW_CURRENT),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_WEATHER_ICON_TYPE),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_WEATHER_ICON_COLOR),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_WEATHER_TEXT_COLOR),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_BACKGROUND_COLOR_START),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_WEATHER_TEXT_SIZE),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_VIBRATION),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_BATTERY_ICON_INDICATOR),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_BATTERY_SHOW_TEXT),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_BATTERY_CIRCLE_DOT_INTERVAL),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_BATTERY_CIRCLE_DOT_LENGTH),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_BATTERY_SHOW_CHARGE_ANIMATION),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_BATTERY_CUT_OUT_TEXT),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_BATTERY_ICON_COLOR),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_BATTERY_TEXT_COLOR),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_PANEL_ONE),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_PANEL_TWO),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_PANEL_THREE),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_PANEL_FOUR),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_STROKE),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_STROKE_COLOR),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_STROKE_THICKNESS),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_CORNER_RADIUS),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_STROKE_DASH_GAP),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_STROKE_DASH_WIDTH),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_BACKGROUND_COLOR_START),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_BACKGROUND_COLOR_CENTER),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_BACKGROUND_COLOR_END),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_BACKGROUND_GRADIENT_USE_CENTER_COLOR),
                    false, this);
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_BACKGROUND_GRADIENT_ORIENTATION),
                    false, this);
            update();
        }

        void unobserve() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }

        public void update() {
            setExpansionViewActivityPanelTextColor();
            setExpansionViewActivityPanelTextSize();
            setExpansionViewCustomLogoImage();
            setExpansionViewShowShortcutBar();
            setExpansionViewTextColor();
            setExpansionViewIconColor();
            setExpansionViewFontStyle();
            setExpansionViewTextSize();
            setExpansionViewText();
            setExpansionViewRipple();
            setExpansionViewWeatherPanelItems();
            setExpansionViewWeatherColors();
            setExpansionViewStroke();
            setExpansionViewWeatherTextSize();
            setExpansionViewVibration();
            setExpansionViewPanelVisibility();
            setExpansionViewBgOrientation();

        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_ACTIVITY_PANEL_TEXT_SIZE))) {
                setExpansionViewActivityPanelTextSize();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_ACTIVITY_PANEL_TEXT_COLOR))) {
                setExpansionViewActivityPanelTextColor();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_CUSTOM_LOGO))) {
                setExpansionViewCustomLogoImage();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_PANEL_SHORTCUTS))) {
                setExpansionViewShowShortcutBar();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_TEXT_COLOR))) {
                setExpansionViewTextColor();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_ICON_COLOR))) {
                setExpansionViewIconColor();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_FONT_STYLE))) {
                setExpansionViewFontStyle();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_TEXT_SIZE))) {
                setExpansionViewTextSize();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_TEXT_CUSTOM))) {
                setExpansionViewText();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_RIPPLE_COLOR))) {
                setExpansionViewRipple();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_WEATHER_SHOW_CURRENT))
                || uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_WEATHER_ICON_TYPE))) {
                setExpansionViewWeatherPanelItems();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_WEATHER_ICON_COLOR))
                || uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_WEATHER_TEXT_COLOR))) {
                setExpansionViewWeatherColors();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_WEATHER_TEXT_SIZE))) {
                setExpansionViewWeatherTextSize();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_VIBRATION))) {
                setExpansionViewVibration();
            /*} else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_BATTERY_ICON_INDICATOR))) {
                setExpansionViewBatteryIndicator();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_BATTERY_SHOW_TEXT))) {
                setExpansionViewBatteryIconTextVisibility();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_BATTERY_CIRCLE_DOT_INTERVAL))
                || uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_BATTERY_CIRCLE_DOT_LENGTH))) {
                setExpansionViewBatteryIconCircleDots();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_BATTERY_SHOW_CHARGE_ANIMATION))) {
                setExpansionViewBatteryShowChargeAnimation();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_BATTERY_CUT_OUT_TEXT))) {
                setExpansionViewBatteryIconCutOutText();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_BATTERY_ICON_COLOR))) {
                setExpansionViewBatteryIconColor();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_BATTERY_TEXT_COLOR))) {
                setExpansionViewBatteryTextColor();*/
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_PANEL_ONE))
                || uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_PANEL_TWO))
                || uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_PANEL_THREE))
                || uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_PANEL_FOUR))) {
                setExpansionViewPanelVisibility();
            } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_BACKGROUND_COLOR_START))
                || uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_BACKGROUND_COLOR_CENTER))
                || uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_BACKGROUND_COLOR_END))
                || uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_BACKGROUND_GRADIENT_USE_CENTER_COLOR))
                || uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_STROKE))
                || uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_STROKE_COLOR))
                || uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_STROKE_THICKNESS))
                || uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_CORNER_RADIUS))
                || uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_STROKE_DASH_GAP))
                || uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_STROKE_DASH_WIDTH))) {
                 setExpansionViewStroke();
                } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_BACKGROUND_GRADIENT_ORIENTATION))) {
                setExpansionViewBgOrientation();
            }
        }
    }
}
