/*
* Copyright (C) 2015 DarkKat
* Copyright (C) 2016 Brett Rogers
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
import android.graphics.Color;
import android.provider.Settings;

public class StatusBarColorHelper {

    private static final int LIGHT_MODE_COLOR_SINGLE_TONE = 0xffffffff;
    private static final int DARK_MODE_COLOR_SINGLE_TONE  = 0x99000000;
    private static final int LIGHT_MODE_ALPHA_SINGLE_TONE = 255;
    private static final int DARK_MODE_ALPHA_SINGLE_TONE  = 153;

    public static int getTextColor(Context context) {
        int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_TEXT_COLOR,  LIGHT_MODE_COLOR_SINGLE_TONE);
        return (LIGHT_MODE_ALPHA_SINGLE_TONE << 24) | (color & 0x00ffffff);
    }

    public static int getIconColor(Context context) {
        int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_ICON_COLOR, LIGHT_MODE_COLOR_SINGLE_TONE);
        return (LIGHT_MODE_ALPHA_SINGLE_TONE << 24) | (color & 0x00ffffff);
    }

    public static int getTextColorDarkMode(Context context) {
        int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_TEXT_COLOR_DARK_MODE,  DARK_MODE_COLOR_SINGLE_TONE);
        return (DARK_MODE_ALPHA_SINGLE_TONE << 24) | (color & 0x00ffffff);
    }

    public static int getIconColorDarkMode(Context context) {
        int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_ICON_COLOR_DARK_MODE, DARK_MODE_COLOR_SINGLE_TONE);
        return (DARK_MODE_ALPHA_SINGLE_TONE << 24) | (color & 0x00ffffff);
    }

    public static int getClockColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_CLOCK_COLOR, 0xffffffff);
    }

    public static int getClockColorDarkMode(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_CLOCK_COLOR_DARK_MODE, 0xff000000);
    }

    public static int getLogoColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_CYANIDE_LOGO_COLOR, 0xffffffff);
    }

    public static int getLogoColorDarkMode(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_CYANIDE_LOGO_COLOR_DARK_MODE, 0xff000000);
    }

    public static int getWeatherTextColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_WEATHER_TEXT_COLOR, 0xffffffff);
    }

    public static int getWeatherTextColorDarkMode(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_WEATHER_TEXT_COLOR_DARK_MODE, 0xff000000);
    }

    public static int getWeatherIconColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_WEATHER_ICON_COLOR, 0xffffffff);
    }

    public static int getWeatherIconColorDarkMode(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_WEATHER_ICON_COLOR_DARK_MODE, 0xff000000);
    }

    public static int getTrafficColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_NETWORK_TRAFFIC_COLOR, 0xffffffff);
    }

    public static int getTrafficColorDarkMode(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_NETWORK_TRAFFIC_COLOR_DARK_MODE, 0xff000000);
    }

    public static int getCustomLabelTextColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_CUSTOM_LABEL_TEXT_COLOR, 0xffffffff);
    }

    public static int getCustomLabelTextColorDarkMode(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.STATUS_BAR_CUSTOM_LABEL_TEXT_COLOR_DARK_MODE, 0xff000000);
    }
}
