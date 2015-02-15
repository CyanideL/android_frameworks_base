/*
 * Copyright (C) 2015 Exodus/Vanir
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

package com.android.internal.util.cyanide;

/* Master list of all assignable actions for the hardware keys */
public class HardwareButtonConstants {
    // Available custom actions to perform on a key press.
    public static final int KEY_ACTION_NOTHING = 0;
    public static final int KEY_ACTION_MENU = 1;
    public static final int KEY_ACTION_APP_SWITCH = 2;
    public static final int KEY_ACTION_SEARCH = 3;
    public static final int KEY_ACTION_VOICE_SEARCH = 4;
    public static final int KEY_ACTION_IN_APP_SEARCH = 5;
    public static final int KEY_ACTION_LAUNCH_CAMERA = 6;
    public static final int KEY_ACTION_SLEEP = 7;
    public static final int KEY_ACTION_LAST_APP = 8;
    public static final int KEY_ACTION_KILL = 9;
    public static final int KEY_ACTION_IME = 10;
    public static final int KEY_ACTION_POWERMENU = 11;
    public static final int KEY_ACTION_HOME = 12;
    public static final int KEY_ACTION_BACK = 13;

    // Masks for checking presence of hardware keys.
    // Must match values in core/res/res/values/config.xml
    public static final int KEY_MASK_HOME = 0x01;
    public static final int KEY_MASK_BACK = 0x02;
    public static final int KEY_MASK_MENU = 0x04;
    public static final int KEY_MASK_ASSIST = 0x08;
    public static final int KEY_MASK_APP_SWITCH = 0x10;
    public static final int KEY_MASK_CAMERA = 0x20;
    public static final int KEY_MASK_VOLUME = 0x40;

    // Defaults - single press...long press...double tap
    public static final int[] APPSWITCH_DEFAULTS = new int[] { KEY_ACTION_APP_SWITCH, KEY_ACTION_NOTHING, KEY_ACTION_NOTHING};
    public static final int[] ASSIST_DEFAULTS = new int[] { KEY_ACTION_SEARCH,  KEY_ACTION_NOTHING,    KEY_ACTION_NOTHING };
    public static final int[] HOME_DEFAULTS = new int[]   { KEY_ACTION_HOME,    KEY_ACTION_SEARCH,    KEY_ACTION_NOTHING };
    public static final int[] MENU_DEFAULTS = new int[]   { KEY_ACTION_MENU,    KEY_ACTION_APP_SWITCH, KEY_ACTION_NOTHING };
    public static final int[] BACK_DEFAULTS = new int[]   { KEY_ACTION_BACK,    KEY_ACTION_LAST_APP,   KEY_ACTION_NOTHING };
}
