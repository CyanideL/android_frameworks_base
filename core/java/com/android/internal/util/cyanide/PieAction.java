/*
* Copyright (C) 2014 SlimRoms Project
*               2015 CyanideL
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

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.IUiModeManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.res.Configuration;
import android.content.Intent;
import android.hardware.input.InputManager;
import android.hardware.TorchManager;
import android.net.Uri;
import android.content.ContentResolver;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.IWindowManager;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.InputDevice;
import android.view.WindowManagerGlobal;
import android.view.WindowManagerPolicyControl;
import android.widget.Toast;

import com.cyanide.util.Helpers;
import com.android.internal.statusbar.IStatusBarService;

import java.net.URISyntaxException;

public class PieAction {

    private static final int MSG_INJECT_KEY_DOWN = 1066;
    private static final int MSG_INJECT_KEY_UP = 1067;

    public static void processAction(Context context, String action, boolean isLongpress) {
        processActionWithOptions(context, action, isLongpress, true);
    }

    public static void processActionWithOptions(Context context,
            String action, boolean isLongpress, boolean collapseShade) {

            if (action == null || action.equals(PieConstants.NULL_BUTTON)) {
                return;
            }

            boolean isKeyguardShowing = false;
            try {
                isKeyguardShowing =
                        WindowManagerGlobal.getWindowManagerService().isKeyguardLocked();
            } catch (RemoteException e) {
                Log.w("Action", "Error getting window manager service", e);
            }

            final IStatusBarService barService = IStatusBarService.Stub.asInterface(
                    ServiceManager.getService(Context.STATUS_BAR_SERVICE));
            if (barService == null) {
                return; // ouch
            }

            final IWindowManager windowManagerService = IWindowManager.Stub.asInterface(
                    ServiceManager.getService(Context.WINDOW_SERVICE));
            if (windowManagerService == null) {
                return; // ouch
            }

            boolean isKeyguardSecure = false;
            try {
                isKeyguardSecure = windowManagerService.isKeyguardSecure();
            } catch (RemoteException e) {
                Log.w("Action", "Error getting window manager service", e);
            }

            if (collapseShade) {
                if (!action.equals(PieConstants.SETTINGS_PANEL_BUTTON)
                        && !action.equals(PieConstants.NOTIFICATIONS_BUTTON)
                        && !action.equals(PieConstants.THEME_SWITCH_BUTTON)
                        && !action.equals(PieConstants.TORCH_BUTTON)) {
                    try {
                        barService.collapsePanels();
                    } catch (RemoteException ex) {
                    }
                }
            }

            // process the actions
            if (action.equals(PieConstants.HOME_BUTTON)) {
                triggerVirtualKeypress(KeyEvent.KEYCODE_HOME, isLongpress);
                return;
            } else if (action.equals(PieConstants.BACK_BUTTON)) {
                triggerVirtualKeypress(KeyEvent.KEYCODE_BACK, isLongpress);
                return;
            } else if (action.equals(PieConstants.MENU_BUTTON)
                    || action.equals(PieConstants.MENU_BIG_BUTTON)) {
                triggerVirtualKeypress(KeyEvent.KEYCODE_MENU, isLongpress);
                return;
            } else if (action.equals(PieConstants.POWER_MENU_BUTTON)) {
                try {
                    windowManagerService.toggleGlobalMenu();
                } catch (RemoteException e) {
                }
                return;
            } else if (action.equals(PieConstants.SCREEN_OFF_BUTTON)) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                pm.goToSleep(SystemClock.uptimeMillis());
                return;
            } else if (action.equals(PieConstants.SLIMPIE_BUTTON)) {
                boolean pieState = isPieEnabled(context);
                if (pieState && !isNavBarEnabled(context) && isNavBarDefault(context)) {
                    Toast.makeText(context,
                            com.android.internal.R.string.disable_pie_navigation_error,
                            Toast.LENGTH_LONG).show();
                    return;
                }
                Settings.System.putIntForUser(
                        context.getContentResolver(),
                        Settings.System.PIE_CONTROLS,
                        pieState ? 0 : 1, UserHandle.USER_CURRENT);
                return;
            } else if (action.equals(PieConstants.NAVBAR_BUTTON)) {
                boolean navBarState = isNavBarEnabled(context);
                if (navBarState && !isPieEnabled(context) && isNavBarDefault(context)) {
                    Toast.makeText(context,
                            com.android.internal.R.string.disable_navigation_pie_error,
                            Toast.LENGTH_LONG).show();
                    return;
                }
                Settings.System.putIntForUser(
                        context.getContentResolver(),
                        Settings.System.NAVBAR_FORCE_ENABLE,
                        navBarState ? 0 : 1, UserHandle.USER_CURRENT);
                return;
            } else if (action.equals(PieConstants.APP_CIRCLE_BAR_BUTTON)) {
                boolean circleBarState = isCircleBarEnabled(context);
                if (circleBarState && !isNavBarEnabled(context) && isNavBarDefault(context)) {
                    Toast.makeText(context,
                            com.android.internal.R.string.disable_pie_navigation_error,
                            Toast.LENGTH_LONG).show();
                    return;
                }
                Settings.System.putIntForUser(
                        context.getContentResolver(),
                        Settings.System.ENABLE_APP_CIRCLE_BAR,
                        circleBarState ? 0 : 1, UserHandle.USER_CURRENT);
                return;
            } else if (action.equals(PieConstants.APP_SIDEBAR_BUTTON)) {
                boolean sideBarState = isSideBarEnabled(context);
                if (sideBarState && !isNavBarEnabled(context) && isNavBarDefault(context)) {
                    Toast.makeText(context,
                            com.android.internal.R.string.disable_pie_navigation_error,
                            Toast.LENGTH_LONG).show();
                    return;
                }
                Settings.System.putIntForUser(
                        context.getContentResolver(),
                        Settings.System.APP_SIDEBAR_ENABLED,
                        sideBarState ? 0 : 1, UserHandle.USER_CURRENT);
                return;
            } else if (action.equals(PieConstants.GESTURE_ANYWHERE_BUTTON)) {
                boolean gestureAnywhereState = isGestureAnywhereEnabled(context);
                if (gestureAnywhereState && !isNavBarEnabled(context) && isNavBarDefault(context)) {
                    Toast.makeText(context,
                            com.android.internal.R.string.disable_pie_navigation_error,
                            Toast.LENGTH_LONG).show();
                    return;
                }
                Settings.System.putIntForUser(
                        context.getContentResolver(),
                        Settings.System.GESTURE_ANYWHERE_ENABLED,
                        gestureAnywhereState ? 0 : 1, UserHandle.USER_CURRENT);
                return;
            } else if (action.equals(PieConstants.HWKEYS_BUTTON)) {
                boolean hWKeysState = isHWKeysEnabled(context);
                if (hWKeysState && !isNavBarEnabled(context) && isNavBarDefault(context)) {
                    Toast.makeText(context,
                            com.android.internal.R.string.disable_pie_navigation_error,
                            Toast.LENGTH_LONG).show();
                    return;
                }
                Settings.System.putIntForUser(
                        context.getContentResolver(),
                        Settings.System.ENABLE_HW_KEYS,
                        hWKeysState ? 0 : 1, UserHandle.USER_CURRENT);
                return;
            } else if (action.equals(PieConstants.HEADS_UP_BUTTON)) {
                boolean headsUpState = isHeadsUpEnabled(context);
                if (headsUpState && !isNavBarEnabled(context) && isNavBarDefault(context)) {
                    Toast.makeText(context,
                            com.android.internal.R.string.disable_pie_navigation_error,
                            Toast.LENGTH_LONG).show();
                    return;
                }
                Settings.System.putIntForUser(
                        context.getContentResolver(),
                        Settings.System.HEADS_UP_USER_ENABLED,
                        headsUpState ? 0 : 1, UserHandle.USER_CURRENT);
                return;
            } else if (action.equals(PieConstants.AMBIENT_DISPLAY_BUTTON)) {
                boolean ambientDisplayState = isAmbientDisplayEnabled(context);
                if (ambientDisplayState && !isNavBarEnabled(context) && isNavBarDefault(context)) {
                    Toast.makeText(context,
                            com.android.internal.R.string.disable_pie_navigation_error,
                            Toast.LENGTH_LONG).show();
                    return;
                }
                Settings.Secure.putIntForUser(
                        context.getContentResolver(),
                        Settings.Secure.DOZE_ENABLED,
                        ambientDisplayState ? 0 : 1, UserHandle.USER_CURRENT);
                return;
            } else if (action.equals(PieConstants.FLOATING_WINDOWS_BUTTON)) {
                boolean floatingWindowsState = isFloatingWindowsEnabled(context);
                if (floatingWindowsState && !isNavBarEnabled(context) && isNavBarDefault(context)) {
                    Toast.makeText(context,
                            com.android.internal.R.string.disable_pie_navigation_error,
                            Toast.LENGTH_LONG).show();
                    return;
                }
                Settings.System.putIntForUser(
                        context.getContentResolver(),
                        Settings.System.FLOATING_WINDOW_MODE,
                        floatingWindowsState ? 0 : 1, UserHandle.USER_CURRENT);
                return;
            } else if (action.equals(PieConstants.KILL_TASK_BUTTON)) {
                if (isKeyguardShowing) {
                    return;
                }
                try {
                    barService.toggleKillApp();
                } catch (RemoteException e) {
                }
                return;
            } else if (action.equals(PieConstants.NOTIFICATIONS_BUTTON)) {
                if (isKeyguardShowing && isKeyguardSecure) {
                    return;
                }
                try {
                    barService.expandNotificationsPanel();
                } catch (RemoteException e) {
                }
                return;
            } else if (action.equals(ActionConstants.ACTION_RESTARTUI)) {
                if (isKeyguardShowing && isKeyguardSecure) {
                    return;
                }
                Helpers.restartSystemUI();
            } else if (action.equals(PieConstants.SETTINGS_PANEL_BUTTON)) {
                if (isKeyguardShowing && isKeyguardSecure) {
                    return;
                }
                try {
                    barService.expandSettingsPanel();
                } catch (RemoteException e) {}
            } else if (action.equals(PieConstants.LAST_APP_BUTTON)) {
                if (isKeyguardShowing) {
                    return;
                }
                try {
                    barService.toggleLastApp();
                } catch (RemoteException e) {
                }
                return;
            } else if (action.equals(PieConstants.RECENT_BUTTON)) {
                if (isKeyguardShowing) {
                    return;
                }
                try {
                    barService.toggleRecentApps();
                } catch (RemoteException e) {
                }
                return;
            } else if (action.equals(PieConstants.SCREENSHOT_BUTTON)) {
                try {
                    barService.toggleScreenshot();
                } catch (RemoteException e) {
                }
                return;
            } else if (action.equals(PieConstants.EXPANDED_DESKTOP_BUTTON)) {
                ContentResolver cr = context.getContentResolver();
                String value = Settings.Global.getString(cr, Settings.Global.POLICY_CONTROL);
                boolean isExpanded = "immersive.full=*".equals(value);
                Settings.Global.putString(cr, Settings.Global.POLICY_CONTROL,
                        isExpanded ? "" : "immersive.full=*");
                if (isExpanded)
                    WindowManagerPolicyControl.reloadFromSetting(context);
                return;
            } else if (action.equals(PieConstants.THEME_SWITCH_BUTTON)) {
                boolean autoLightMode = Settings.Secure.getIntForUser(
                        context.getContentResolver(),
                        Settings.Secure.UI_THEME_AUTO_MODE, 0,
                        UserHandle.USER_CURRENT) == 1;
                boolean state = context.getResources().getConfiguration().uiThemeMode
                        == Configuration.UI_THEME_MODE_HOLO_DARK;
                if (autoLightMode) {
                    try {
                        barService.collapsePanels();
                    } catch (RemoteException ex) {
                    }
                    Toast.makeText(context,
                            com.android.internal.R.string.theme_auto_switch_mode_error,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                // Handle a switch change
                // we currently switch between darktheme and lighttheme till either
                // theme engine is ready or lighttheme is ready. Currently due of
                // missing light themeing lighttheme = system base theme
                final IUiModeManager uiModeManagerService = IUiModeManager.Stub.asInterface(
                        ServiceManager.getService(Context.UI_MODE_SERVICE));
                try {
                    uiModeManagerService.setUiThemeMode(state
                            ? Configuration.UI_THEME_MODE_HOLO_LIGHT
                            : Configuration.UI_THEME_MODE_HOLO_DARK);
                } catch (RemoteException e) {
                }
                return;
            } else if (action.equals(PieConstants.TORCH_BUTTON)) {
                // toggle torch the new way
                TorchManager torchManager =
                        (TorchManager) context.getSystemService(Context.TORCH_SERVICE);
                if (!torchManager.isTorchOn()) {
                    torchManager.setTorchEnabled(true);
                } else {
                    torchManager.setTorchEnabled(false);
                }
                return;
            } else {
                // we must have a custom uri
                Intent intent = null;
                try {
                    intent = Intent.parseUri(action, 0);
                } catch (URISyntaxException e) {
                    Log.e("PieAction:", "URISyntaxException: [" + action + "]");
                    return;
                }
                startActivity(context, intent, barService, isKeyguardShowing);
                return;
            }

    }
    
    public static boolean isPieEnabled(Context context) {
        return Settings.System.getIntForUser(context.getContentResolver(),
                Settings.System.PIE_CONTROLS,
                0, UserHandle.USER_CURRENT) == 1;
    }

    public static boolean isNavBarEnabled(Context context) {
        return Settings.System.getIntForUser(context.getContentResolver(),
                Settings.System.NAVBAR_FORCE_ENABLE,
                isNavBarDefault(context) ? 1 : 0, UserHandle.USER_CURRENT) == 1;
    }
    
    public static boolean isCircleBarEnabled(Context context) {
        return Settings.System.getIntForUser(context.getContentResolver(),
                Settings.System.ENABLE_APP_CIRCLE_BAR,
                0, UserHandle.USER_CURRENT) == 1;
    }

    public static boolean isSideBarEnabled(Context context) {
        return Settings.System.getIntForUser(context.getContentResolver(),
                Settings.System.APP_SIDEBAR_ENABLED,
                0, UserHandle.USER_CURRENT) == 1;
    }

    public static boolean isGestureAnywhereEnabled(Context context) {
        return Settings.System.getIntForUser(context.getContentResolver(),
                Settings.System.GESTURE_ANYWHERE_ENABLED,
                0, UserHandle.USER_CURRENT) == 1;
    }

    public static boolean isHWKeysEnabled(Context context) {
        return Settings.System.getIntForUser(context.getContentResolver(),
                Settings.System.ENABLE_HW_KEYS,
                0, UserHandle.USER_CURRENT) == 1;
    }

    public static boolean isHeadsUpEnabled(Context context) {
        return Settings.System.getIntForUser(context.getContentResolver(),
                Settings.System.HEADS_UP_USER_ENABLED,
                0, UserHandle.USER_CURRENT) == 1;
    }

    public static boolean isAmbientDisplayEnabled(Context context) {
        return Settings.Secure.getIntForUser(context.getContentResolver(),
                Settings.Secure.DOZE_ENABLED,
                0, UserHandle.USER_CURRENT) == 1;
    }

    public static boolean isFloatingWindowsEnabled(Context context) {
        return Settings.System.getIntForUser(context.getContentResolver(),
                Settings.System.FLOATING_WINDOW_MODE,
                0, UserHandle.USER_CURRENT) == 1;
    }

    public static boolean isNavBarDefault(Context context) {
        return context.getResources().getBoolean(
                com.android.internal.R.bool.config_showNavigationBar);
    }

    public static boolean isActionKeyEvent(String action) {
        if (action.equals(PieConstants.HOME_BUTTON)
                || action.equals(PieConstants.BACK_BUTTON)
                || action.equals(PieConstants.MENU_BUTTON)
                || action.equals(PieConstants.MENU_BIG_BUTTON)
                || action.equals(PieConstants.NULL_BUTTON)) {
            return true;
        }
        return false;
    }

    private static void startActivity(Context context, Intent intent,
            IStatusBarService barService, boolean isKeyguardShowing) {
        if (intent == null) {
            return;
        }
        if (isKeyguardShowing) {
            // Have keyguard show the bouncer and launch the activity if the user succeeds.
            try {
                barService.showCustomIntentAfterKeyguard(intent);
            } catch (RemoteException e) {
                Log.w("Action", "Error starting custom intent on keyguard", e);
            }
        } else {
            // otherwise let us do it here
            try {
                WindowManagerGlobal.getWindowManagerService().dismissKeyguard();
            } catch (RemoteException e) {
                Log.w("Action", "Error dismissing keyguard", e);
            }
            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivityAsUser(intent,
                    new UserHandle(UserHandle.USER_CURRENT));
        }
    }
    
    public static void triggerVirtualKeypress(final int keyCode, boolean longpress) {
        InputManager im = InputManager.getInstance();
        long now = SystemClock.uptimeMillis();
        int downflags = 0;
        int upflags = 0;
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
            || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
            || keyCode == KeyEvent.KEYCODE_DPAD_UP
            || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            downflags = upflags = KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE;
        } else {
            downflags = upflags = KeyEvent.FLAG_FROM_SYSTEM | KeyEvent.FLAG_VIRTUAL_HARD_KEY;
        }
        if (longpress) {
            downflags |= KeyEvent.FLAG_LONG_PRESS;
        }

        final KeyEvent downEvent = new KeyEvent(now, now, KeyEvent.ACTION_DOWN,
                keyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                downflags,
                InputDevice.SOURCE_KEYBOARD);
        im.injectInputEvent(downEvent, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);

        final KeyEvent upEvent = new KeyEvent(now, now, KeyEvent.ACTION_UP,
                keyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                upflags,
                InputDevice.SOURCE_KEYBOARD);
        im.injectInputEvent(upEvent, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
    }

}
