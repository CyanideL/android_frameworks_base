/*
* Copyright (C) 2015 DarkKat
*
* Copyright (C) 2015 CyanideL (Brett Rogers)
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

package com.android.internal.util.cyanide;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.provider.Settings;

public class QSColorHelper {

    private static int BLACK = 0xff000000;
    private static int CYANIDE_BLUE = 0xff1976d2;
    private static int WHITE = 0xffffffff;

    public static ColorStateList getIconColorList(Context context) {
        return ColorStateList.valueOf(getIconColor(context));
    }

    public static int getIconColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.QS_ICON_COLOR, WHITE);
    }

    public static ColorStateList getRippleColorList(Context context) {
        return ColorStateList.valueOf(getRippleColor(context));
    }

    public static int getRippleColor(Context context) {
        int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.QS_RIPPLE_COLOR, CYANIDE_BLUE);
        int colorToUse =  (74 << 24) | color;
        return colorToUse;
    }

    public static int getTextColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.QS_TEXT_COLOR, WHITE);
    }

    public static ColorStateList getBackgroundColorList(Context context) {
        return ColorStateList.valueOf(getBackgroundColor(context));
    }

    public static int getBackgroundColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.QS_BACKGROUND_COLOR, BLACK);
    }

    public static ColorStateList getAccentColorList(Context context) {
        return ColorStateList.valueOf(getAccentColor(context));
    }

    public static int getAccentColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.QS_ACCENT_COLOR, CYANIDE_BLUE);
    }

    public static ColorStateList getBrightnessSliderColorList(Context context) {
        return ColorStateList.valueOf(getBrightnessSliderColor(context));
    }

    public static int getBrightnessSliderColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.QS_BRIGHTNESS_SLIDER_COLOR, WHITE);
    }

    public static ColorStateList getBrightnessSliderEmptyColorList(Context context) {
        return ColorStateList.valueOf(getBrightnessSliderEmptyColor(context));
    }

    public static int getBrightnessSliderEmptyColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.QS_BRIGHTNESS_SLIDER_BG_COLOR, WHITE);
    }

    public static ColorStateList getBrightnessSliderIconColorList(Context context) {
        return ColorStateList.valueOf(getBrightnessSliderIconColor(context));
    }

    public static int getBrightnessSliderIconColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.QS_BRIGHTNESS_SLIDER_ICON_COLOR, WHITE);
    }

    public static ColorStateList getHeaderIconColorList(Context context) {
        return ColorStateList.valueOf(getHeaderIconColor(context));
    }

    public static int getHeaderIconColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.QS_HEADER_ICON_COLOR, WHITE);
    }

    public static ColorStateList getHeaderRippleColorList(Context context) {
        return ColorStateList.valueOf(getHeaderRippleColor(context));
    }

    public static int getHeaderRippleColor(Context context) {
        int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.QS_HEADER_RIPPLE_COLOR, CYANIDE_BLUE);
        int colorToUse =  (74 << 24) | color;
        return colorToUse;
    }

    public static int getHeaderTextColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.QS_HEADER_TEXT_COLOR, WHITE);
    }
}
