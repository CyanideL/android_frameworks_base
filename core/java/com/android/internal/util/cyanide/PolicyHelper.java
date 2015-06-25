/*
* Copyright (C) 2013 SlimRoms Project
* Copyright (C) 2015 CyanideL && Fusion (Port/Modify text/icons colors)
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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.ArrayList;

public class PolicyHelper {

    private static final String SYSTEM_METADATA_NAME = "android";
    private static final String SYSTEMUI_METADATA_NAME = "com.android.systemui";

    public static ArrayList<ActionConfig> getPowerMenuConfigWithDescription(
            Context context, String values, String entries) {
        String config = Settings.System.getStringForUser(
                    context.getContentResolver(),
                    Settings.System.POWER_MENU_CONFIG,
                    UserHandle.USER_CURRENT);
        if (config == null) {
            config = PolicyConstants.POWER_MENU_CONFIG_DEFAULT;
        }
        return ConfigSplitHelper.getActionConfigValues(context, config, values, entries, true);
    }

    public static void setPowerMenuConfig(Context context,
            ArrayList<ActionConfig> actionConfig, boolean reset) {
        String config;
        if (reset) {
            config = PolicyConstants.POWER_MENU_CONFIG_DEFAULT;
        } else {
            config = ConfigSplitHelper.setActionConfig(actionConfig, true);
        }
        Settings.System.putString(context.getContentResolver(),
                    Settings.System.POWER_MENU_CONFIG,
                    config);
    }

    public static Drawable getPowerMenuIconImage(Context context,
            String clickAction, String customIcon, boolean colorize) {
        int resId = -1;
        int iconColor = -2;
        int colorMode = 0;
        Drawable d = null;
        Drawable dError = null;
        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            return null;
        }

        if (colorize) {
            iconColor = Settings.System.getIntForUser(
                    context.getContentResolver(),
                    Settings.System.POWER_MENU_ICON_COLOR, -2,
                    UserHandle.USER_CURRENT);
            colorMode = Settings.System.getIntForUser(
                    context.getContentResolver(),
                    Settings.System.POWER_MENU_ICON_COLOR_MODE, 0,
                    UserHandle.USER_CURRENT);

            if (iconColor == -2) {
                iconColor = context.getResources().getColor(
                    com.android.internal.R.color.power_menu_icon_default_color);
            }
        }

        if (!clickAction.startsWith("**")) {
            try {
                String extraIconPath = clickAction.replaceAll(".*?hasExtraIcon=", "");
                if (extraIconPath != null && !extraIconPath.isEmpty()) {
                    File f = new File(Uri.parse(extraIconPath).getPath());
                    if (f.exists()) {
                        d = new BitmapDrawable(context.getResources(),
                                f.getAbsolutePath());
                    }
                }
                if (d == null) {
                    d = pm.getActivityIcon(Intent.parseUri(clickAction, 0));
                }
            } catch (NameNotFoundException e) {
                Resources systemUiResources;
                try {
                    systemUiResources = pm.getResourcesForApplication(SYSTEMUI_METADATA_NAME);
                } catch (Exception ex) {
                    Log.e("PolicyHelper:", "can't access systemui resources",e);
                    return null;
                }
                resId = systemUiResources.getIdentifier(
                    SYSTEMUI_METADATA_NAME + ":drawable/ic_sysbar_null", null, null);
                if (resId > 0) {
                    dError = systemUiResources.getDrawable(resId);
                    if (colorMode != 3 && colorMode == 0 && colorize) {
                        dError = new BitmapDrawable(
                            ImageHelper.getColoredBitmap(dError, iconColor));
                    }
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        boolean coloring = false;
        if (customIcon != null && customIcon.startsWith(PolicyConstants.SYSTEM_ICON_IDENTIFIER)) {
            Resources systemResources;
            try {
                systemResources = pm.getResourcesForApplication(SYSTEM_METADATA_NAME);
            } catch (Exception e) {
                Log.e("ActionHelper:", "can't access system resources",e);
                return null;
            }

            resId = systemResources.getIdentifier(customIcon.substring(
                        ActionConstants.SYSTEM_ICON_IDENTIFIER.length()), "drawable", "android");
            if (resId > 0) {
                d = systemResources.getDrawable(resId);
                if (colorMode != 3 && colorize) {
                    coloring = true;
                }
            }
        } else if (customIcon != null && !customIcon.equals(ActionConstants.ICON_EMPTY)) {
            File f = new File(Uri.parse(customIcon).getPath());
            if (f.exists()) {
                d = new BitmapDrawable(context.getResources(),
                        ImageHelper.getRoundedCornerBitmap(
                        new BitmapDrawable(context.getResources(),
                        f.getAbsolutePath()).getBitmap()));
                if (colorMode != 3 && colorMode != 1 && colorize) {
                    coloring = true;
                }
            } else {
                Log.e("ActionHelper:", "can't access custom icon image");
                return null;
            }
        } else if (clickAction.startsWith("**")) {
            d = getPowerMenuSystemIcon(context, clickAction);
            if (colorMode != 3 && colorize) {
                coloring = true;
            }
        } else if (colorMode != 3 && colorMode == 0 && colorize) {
            coloring = true;
        }
        if (dError == null) {
            if (coloring) {
                d = new BitmapDrawable(ImageHelper.getColoredBitmap(d, iconColor));
            }
            return ImageHelper.resize(context, d, 36);
        } else {
            return ImageHelper.resize(context, dError, 36);
        }
    }

    public static Drawable getPowerMenuIconImage(Context context,
            String clickAction, String customIcon) {
        int resId = -1;
        Drawable d = null;
        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            return null;
        }

        Resources systemUiResources;
        try {
            systemUiResources = pm.getResourcesForApplication(SYSTEMUI_METADATA_NAME);
        } catch (Exception e) {
            Log.e("ActionHelper:", "can't access systemui resources",e);
            return null;
        }

        if (!clickAction.startsWith("**")) {
            try {
                String extraIconPath = clickAction.replaceAll(".*?hasExtraIcon=", "");
                if (extraIconPath != null && !extraIconPath.isEmpty()) {
                    File f = new File(Uri.parse(extraIconPath).getPath());
                    if (f.exists()) {
                        d = new BitmapDrawable(context.getResources(),
                                f.getAbsolutePath());
                    }
                }
                if (d == null) {
                    d = pm.getActivityIcon(Intent.parseUri(clickAction, 0));
                }
            } catch (NameNotFoundException e) {
                resId = systemUiResources.getIdentifier(
                    SYSTEMUI_METADATA_NAME + ":drawable/ic_sysbar_null", null, null);
                if (resId > 0) {
                    d = systemUiResources.getDrawable(resId);
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        if (customIcon != null && customIcon.startsWith(ActionConstants.SYSTEM_ICON_IDENTIFIER)) {
            resId = systemUiResources.getIdentifier(customIcon.substring(
                        ActionConstants.SYSTEM_ICON_IDENTIFIER.length()), "drawable", "android");
            if (resId > 0) {
                d = systemUiResources.getDrawable(resId);
                if (d != null) {
                    d = new BitmapDrawable(ImageHelper.getColoredBitmap(d, 
                            context.getResources().getColor(com.android.internal.R.color.dslv_icon_dark)));
                }
            }
        } else if (customIcon != null && !customIcon.equals(ActionConstants.ICON_EMPTY)) {
            File f = new File(Uri.parse(customIcon).getPath());
            if (f.exists()) {
                d = new BitmapDrawable(context.getResources(),
                    ImageHelper.getRoundedCornerBitmap(
                        new BitmapDrawable(context.getResources(),
                        f.getAbsolutePath()).getBitmap()));
            } else {
                Log.e("ActionHelper:", "can't access custom icon image");
                return null;
            }
        } else if (clickAction.startsWith("**")) {
            d = getPowerMenuSystemIcon(context, clickAction);
            if (d != null) {
                d = new BitmapDrawable(ImageHelper.getColoredBitmap(d, 
                            context.getResources().getColor(com.android.internal.R.color.dslv_icon_dark)));
            }
        }
        return ImageHelper.resize(context, d, 36);
    }

    private static Drawable getPowerMenuSystemIcon(Context context, String clickAction) {
        if (clickAction.equals(PolicyConstants.ACTION_POWER_OFF)) {
            return context.getResources().getDrawable(
                com.android.internal.R.drawable.ic_lock_power_off_alpha);
        } else if (clickAction.equals(PolicyConstants.ACTION_REBOOT)) {
            return context.getResources().getDrawable(
                com.android.internal.R.drawable.ic_lock_reboot_alpha);
        } else if (clickAction.equals(PolicyConstants.ACTION_SCREENSHOT)) {
            return context.getResources().getDrawable(
                com.android.internal.R.drawable.ic_lock_screenshot_alpha);
        } else if (clickAction.equals(PolicyConstants.ACTION_AIRPLANE)) {
            return context.getResources().getDrawable(
                com.android.internal.R.drawable.ic_lock_airplane_mode_off_am_alpha);
        } else if (clickAction.equals(PolicyConstants.ACTION_SOUND)) {
            return context.getResources().getDrawable(
                com.android.internal.R.drawable.ic_audio_ring_notif_cyanide);
        } else if (clickAction.equals(PolicyConstants.ACTION_EXPANDED_DESKTOP)) {
            return context.getResources().getDrawable(
                com.android.internal.R.drawable.ic_lock_expanded_desktop);
        } else if (clickAction.equals(PolicyConstants.ACTION_PIE)) {
            return context.getResources().getDrawable(
                com.android.internal.R.drawable.ic_lock_pie);
        } else if (clickAction.equals(PolicyConstants.ACTION_PA_PIE)) {
            return context.getResources().getDrawable(
                com.android.internal.R.drawable.ic_lock_pa_pie);
        } else if (clickAction.equals(PolicyConstants.ACTION_NAVBAR)) {
            return context.getResources().getDrawable(
                com.android.internal.R.drawable.ic_lock_navbar);
        } else if (clickAction.equals(PolicyConstants.ACTION_SETTINGS)) {
            return context.getResources().getDrawable(
                com.android.internal.R.drawable.ic_settings);
        } else if (clickAction.equals(PolicyConstants.ACTION_LOCKDOWN)) {
            return context.getResources().getDrawable(
                com.android.internal.R.drawable.ic_lock_lock_alpha);
        } else if (clickAction.equals(PolicyConstants.ACTION_ONTHEGO)) {
            return context.getResources().getDrawable(
                com.android.internal.R.drawable.ic_lock_onthego_alpha);
        } else if (clickAction.equals(PolicyConstants.ACTION_PROFILE)) {
            return context.getResources().getDrawable(
                com.android.internal.R.drawable.ic_lock_profile_cyanide);
        } else if (clickAction.equals(PolicyConstants.ACTION_RESTARTUI)) {
            return context.getResources().getDrawable(
                com.android.internal.R.drawable.ic_lock_restart);
        } else if (clickAction.equals(PolicyConstants.ACTION_APP_CIRCLE_BAR)) {
            return context.getResources().getDrawable(
                com.android.internal.R.drawable.ic_lock_appcirclebar);
        } else if (clickAction.equals(PolicyConstants.ACTION_APP_SIDEBAR)) {
            return context.getResources().getDrawable(
                com.android.internal.R.drawable.ic_lock_appsidebar);
        } else if (clickAction.equals(PolicyConstants.ACTION_GESTURE_ANYWHERE)) {
            return context.getResources().getDrawable(
                com.android.internal.R.drawable.ic_lock_gestures);
        } else if (clickAction.equals(PolicyConstants.ACTION_HWKEYS)) {
            return context.getResources().getDrawable(
                com.android.internal.R.drawable.ic_lock_buttons);
        } else if (clickAction.equals(PolicyConstants.ACTION_HEADS_UP)) {
            return context.getResources().getDrawable(
                com.android.internal.R.drawable.ic_lock_heads_up);
        } else if (clickAction.equals(PolicyConstants.ACTION_AMBIENT_DISPLAY)) {
            return context.getResources().getDrawable(
                com.android.internal.R.drawable.ic_lock_doze);
        } else if (clickAction.equals(PolicyConstants.ACTION_FLOATING_WINDOWS)) {
            return context.getResources().getDrawable(
                com.android.internal.R.drawable.ic_lock_floating);
        }
        return null;
    }

}
