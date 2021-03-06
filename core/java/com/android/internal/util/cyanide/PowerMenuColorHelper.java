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
import android.content.res.ColorStateList;
import android.provider.Settings;

import com.android.internal.R;

public class PowerMenuColorHelper {

    private static int DEFAULT_COLOR = 0xff000000;

    public static ColorStateList getBackgroundColorList(Context context) {
        return ColorStateList.valueOf(getBackgroundColor(context));
    }

    public static int getBackgroundColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.POWER_MENU_BG_COLOR,
                context.getResources().getColor(
                R.color.global_actions_bg_color));
    }

    public static ColorStateList getIconNormalColorList(Context context) {
        return ColorStateList.valueOf(getIconNormalColor(context));
    }

    public static int getIconNormalColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.POWER_MENU_ICON_NORMAL_COLOR,
                context.getResources().getColor(
                R.color.global_actions_icon_color_normal));
    }

    public static ColorStateList getIconEnabledSelectedColorList(Context context) {
        return ColorStateList.valueOf(getIconEnabledSelectedColor(context));
    }

    public static int getIconEnabledSelectedColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.POWER_MENU_ICON_ENABLED_SELECTED_COLOR,
                context.getResources().getColor(
                R.color.global_actions_icon_color_enabled_selected));
    }

    public static ColorStateList getRippleColorList(Context context) {
        return ColorStateList.valueOf(getRippleColor(context));
    }

    public static int getRippleColor(Context context) {
        int color = Settings.System.getInt(context.getContentResolver(),
                Settings.System.POWER_MENU_RIPPLE_COLOR,
                context.getResources().getColor(
                R.color.global_actions_ripple_color));
        int colorToUse =  (74 << 24) | (color & 0x00ffffff);
        return colorToUse;
    }

    public static ColorStateList getTextColorList(Context context) {
        return ColorStateList.valueOf(getTextColor(context));
    }

    public static int getTextColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.POWER_MENU_TEXT_COLOR,
                context.getResources().getColor(
                R.color.global_actions_text_color));
    }
}
