/*
* Copyright (C) 2015 DarkKat
*
*  Additional Options 2015 CyanideL
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
import android.provider.Settings;

public class QSColorHelper {

    private static int WHITE = 0xffffffff;

    public static int getBrightnessSliderColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.QS_BRIGHTNESS_SLIDER_COLOR, WHITE);
    }

    public static int getBrightnessSliderEmptyColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.QS_BRIGHTNESS_SLIDER_BG_COLOR, WHITE);
    }

    public static int getBrightnessSliderIconColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.QS_BRIGHTNESS_SLIDER_ICON_COLOR, WHITE);
    }

    public static int getIconColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.QS_ICON_COLOR, WHITE);
    }

    public static int getRippleColor(Context context) {
        int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.QS_RIPPLE_COLOR, WHITE);
        int colorToUse =  (74 << 24) | (color & 0x00ffffff);
        return colorToUse;
    }

    public static int getTextColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.QS_TEXT_COLOR, WHITE);
    }
}
