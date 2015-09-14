/*
 * Copyright (C) 2015 DarkKat
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

package com.android.systemui.qs;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.ContentObserver;
import android.graphics.drawable.RippleDrawable;
import android.graphics.PorterDuff.Mode;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.internal.util.cyanide.QSBarConstants;
import com.android.internal.util.cyanide.QSBarHelper;
import com.android.internal.util.cyanide.QSColorHelper;
import com.android.internal.util.cyanide.ActionConfig;

import com.android.systemui.R;
import com.android.systemui.qs.buttons.AirplaneModeButton;
import com.android.systemui.qs.buttons.AppCircleBarButton;
import com.android.systemui.qs.buttons.AppSideBarButton;
import com.android.systemui.qs.buttons.AmbientButton;
import com.android.systemui.qs.buttons.BluetoothButton;
import com.android.systemui.qs.buttons.ColorInversionButton;
import com.android.systemui.qs.buttons.CyanideButton;
import com.android.systemui.qs.buttons.DataButton;
import com.android.systemui.qs.buttons.FloatingButton;
import com.android.systemui.qs.buttons.GestureAnywhereButton;
import com.android.systemui.qs.buttons.HeadsUpButton;
import com.android.systemui.qs.buttons.HotspotButton;
import com.android.systemui.qs.buttons.HWKeysButton;
import com.android.systemui.qs.buttons.LocationButton;
import com.android.systemui.qs.buttons.LteButton;
import com.android.systemui.qs.buttons.NavBarButton;
import com.android.systemui.qs.buttons.NfcButton;
import com.android.systemui.qs.buttons.QSButton;
import com.android.systemui.qs.buttons.PieControlButton;
import com.android.systemui.qs.buttons.PowerMenuButton;
import com.android.systemui.qs.buttons.RestartUIButton;
import com.android.systemui.qs.buttons.RotationLockButton;
import com.android.systemui.qs.buttons.ScreenOffButton;
import com.android.systemui.qs.buttons.SlimPieButton;
import com.android.systemui.qs.buttons.SlimFloatsButton;
import com.android.systemui.qs.buttons.ThemesButton;
import com.android.systemui.qs.buttons.TorchButton;
import com.android.systemui.qs.buttons.WifiButton;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.HotspotController;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.RotationLockController;
import com.android.systemui.statusbar.policy.LocationController;

import java.util.ArrayList;

public class QSBar extends LinearLayout {

    private final Context mContext;
    private final Handler mHandler;
    private final SettingsObserver mSettingsObserver;
    private PhoneStatusBar mStatusBar;
    private BluetoothController mBluetoothController;
    private NetworkController mNetworkController;
    private RotationLockController mRotationLockController;
    private LocationController mLocationController;
    private HotspotController mHotspotController;
    private ArrayList<QSButton> mButtons;
    private boolean mListening = false;

    public QSBar(Context context) {
        this(context, null);
    }

    public QSBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
        mHandler = new Handler();
        mSettingsObserver = new SettingsObserver(mHandler);
        mButtons = new ArrayList<QSButton>();

        boolean showBar = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.QS_TYPE, 0) == 1;
        setVisibility(showBar ? View.INVISIBLE : View.GONE);
    }

    public void setUp(PhoneStatusBar statusBar, BluetoothController bluetooth, NetworkController network,
            RotationLockController rotationLock, LocationController location, HotspotController hotspot) {
        mStatusBar = statusBar;
        mBluetoothController = bluetooth;
        mNetworkController = network;
        mRotationLockController = rotationLock;
        mLocationController = location;
        mHotspotController = hotspot;
        createBarButtons();
        mSettingsObserver.observe();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    private void createBarButtons() {
        ArrayList<ActionConfig> actionConfigs =
                QSBarHelper.getQSBarConfig(mContext);
        ActionConfig actionConfig;

        if (mButtons.size() != 0) {
            mButtons.clear();
        }

        for (int i = 0; i < actionConfigs.size(); i++) {
            actionConfig = actionConfigs.get(i);
            final String button = actionConfig.getClickAction();
            QSButton qsButton = createButton(button);
            setIconColor(qsButton);
            setRippleColor(qsButton);
            addView(qsButton);
            mButtons.add(qsButton);
        }
    }

    public void setListening(boolean listening) {
        if (mButtons.size() == 0 || mListening == listening) return;
        mListening = listening;
        for (int i = 0; i < mButtons.size(); i++) {
            QSButton button = mButtons.get(i);
            button.setListening(mListening);
        }
    }

    private QSButton createButton(String action) {
        QSButton button = null;

        if (action.equals(QSBarConstants.BUTTON_AIRPLANE)) {
            button = new AirplaneModeButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_airplane),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_airplane_off));
        } else if (action.equals(QSBarConstants.BUTTON_APPCIRCLEBAR)) {
            button = new AppCircleBarButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_appcirclebar_on),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_appcirclebar_off));
        } else if (action.equals(QSBarConstants.BUTTON_APPSIDEBAR)) {
            button = new AppSideBarButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_sidebar_on),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_sidebar_off));
        } else if (action.equals(QSBarConstants.BUTTON_AMBIENT)) {
            button = new AmbientButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_doze),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_doze_off));
        } else if (action.equals(QSBarConstants.BUTTON_BLUETOOTH)) {
            button = new BluetoothButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_bt),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_bt_off));
        } else if (action.equals(QSBarConstants.BUTTON_CYANIDE)) {
            button = new CyanideButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_cyanide_on),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_cyanide_on));
        } else if (action.equals(QSBarConstants.BUTTON_DATA)) {
            button = new DataButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_data),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_data_off));
        } else if (action.equals(QSBarConstants.BUTTON_FLASHLIGHT)) {
            button = new TorchButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_torch),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_torch_off));
        } else if (action.equals(QSBarConstants.BUTTON_FLOATING)) {
            button = new FloatingButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_floating_on),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_floating_off));
        } else if (action.equals(QSBarConstants.BUTTON_GESTUREANYWHERE)) {
            button = new GestureAnywhereButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_gestures_on),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_gestures_off));
        } else if (action.equals(QSBarConstants.BUTTON_HOTSPOT)) {
            button = new HotspotButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_hotspot),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_hotspot_off));
        } else if (action.equals(QSBarConstants.BUTTON_HEADSUP)) {
            button = new HeadsUpButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_headsup_toggle_on),
                    mContext.getResources().getDrawable(R.drawable.ic_headsup_toggle_off));
        } else if (action.equals(QSBarConstants.BUTTON_HWKEYS)) {
            button = new HWKeysButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_buttons_on),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_buttons_off));
        } else if (action.equals(QSBarConstants.BUTTON_INVERSION)) {
            button = new ColorInversionButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_inversion),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_inversion_off));
        } else if (action.equals(QSBarConstants.BUTTON_LOCATION)) {
            button = new LocationButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_location),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_location_off));
        } else if (action.equals(QSBarConstants.BUTTON_LTE)) {
            button = new LteButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_lte),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_lte_off));
        } else if (action.equals(QSBarConstants.BUTTON_NAVBAR)) {
            button = new NavBarButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_navbar_on),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_navbar_off));
        } else if (action.equals(QSBarConstants.BUTTON_NFC)) {
            button = new NfcButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_nfc),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_nfc_off));
        } else if (action.equals(QSBarConstants.BUTTON_PIE_CONTROL)) {
            button = new PieControlButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_pie_global_on),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_pie_global_off));
        } else if (action.equals(QSBarConstants.BUTTON_POWER_MENU)) {
            button = new PowerMenuButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_sysbar_power),
                    mContext.getResources().getDrawable(R.drawable.ic_sysbar_power));
        } else if (action.equals(QSBarConstants.BUTTON_RESTARTUI)) {
            button = new RestartUIButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_systemui_restart),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_systemui_restart));
        } else if (action.equals(QSBarConstants.BUTTON_ROTATION)) {
            button = new RotationLockButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_rotation),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_rotation_off));
        } else if (action.equals(QSBarConstants.BUTTON_SCREENOFF)) {
            button = new ScreenOffButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_sysbar_power),
                    mContext.getResources().getDrawable(R.drawable.ic_sysbar_power));
        } else if (action.equals(QSBarConstants.BUTTON_SLIMPIE)) {
            button = new SlimPieButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_pie_on),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_pie_off));
        } else if (action.equals(QSBarConstants.BUTTON_SLIM_FLOATS)) {
            button = new SlimFloatsButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_floating_on),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_floating_off));
        } else if (action.equals(QSBarConstants.BUTTON_THEMES)) {
            button = new ThemesButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_themes),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_themes));
        } else if (action.equals(QSBarConstants.BUTTON_WIFI)) {
            button = new WifiButton(mContext, this,
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_wifi),
                    mContext.getResources().getDrawable(R.drawable.ic_qs_button_wifi_off));
        }

        int dimens = mContext.getResources().getDimensionPixelSize(R.dimen.qs_button_size);
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(dimens, dimens);
        button.setLayoutParams(lp);
        button.setScaleType(ImageView.ScaleType.CENTER);
        button.setClickable(true);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doHapticKeyClick(HapticFeedbackConstants.VIRTUAL_KEY);
                ((QSButton) v).handleClick();
            }
        });
        if (!(button instanceof TorchButton)) {
            button.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    doHapticKeyClick(HapticFeedbackConstants.LONG_PRESS);
                    ((QSButton) v).handleLongClick();
                    return true;
                }
            });
        }
        return button;
    }

    public BluetoothController getBluetoothController() {
        return mBluetoothController;
    }

    public HotspotController getHotspotController() {
        return mHotspotController;
    }

    public LocationController getLocationController() {
        return mLocationController;
    }

    public NetworkController getNetworkController() {
        return mNetworkController;
    }

    public RotationLockController getRotationLockController() {
        return mRotationLockController;
    }

    public void doHapticKeyClick(int type) {
        performHapticFeedback(type,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                | HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
    }

    public void setColors() {
        removeAllViews();
        createBarButtons();
    }

    private void setIconColor(ImageView iv) {
        iv.setColorFilter(QSColorHelper.getIconColor(mContext), Mode.MULTIPLY);
    }

    private void setRippleColor(ImageView iv) {
        RippleDrawable rd = (RippleDrawable) mContext.getDrawable(R.drawable.ripple_drawable_oval);
        final int rippleColor = QSColorHelper.getRippleColor(mContext);

        int states[][] = new int[][] {
            new int[] {
                com.android.internal.R.attr.state_enabled
            }
        };
        int colors[] = new int[] {
            rippleColor
        };
        ColorStateList color = new ColorStateList(states, colors);

        rd.setColor(color);
        iv.setBackground(rd);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(Settings.System.QS_BUTTONS),
                    false, this, UserHandle.USER_ALL);
            mContext.getContentResolver().registerContentObserver(
                    Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED),
                    false, this, UserHandle.USER_ALL);
            mContext.getContentResolver().registerContentObserver(
                    Settings.Secure.getUriFor(Settings.Secure.ADVANCED_MODE),
                    false, this, UserHandle.USER_ALL);
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(Settings.System.ENABLE_APP_CIRCLE_BAR),
                    false, this, UserHandle.USER_ALL);
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(Settings.System.APP_SIDEBAR_ENABLED),
                    false, this, UserHandle.USER_ALL);
            mContext.getContentResolver().registerContentObserver(
                    Settings.Secure.getUriFor(Settings.Secure.DOZE_ENABLED),
                    false, this, UserHandle.USER_ALL);
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(Settings.System.FLOATING_WINDOW_MODE),
                    false, this, UserHandle.USER_ALL);
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(Settings.System.GESTURE_ANYWHERE_ENABLED),
                    false, this, UserHandle.USER_ALL);
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(Settings.System.NAVBAR_FORCE_ENABLE),
                    false, this, UserHandle.USER_ALL);
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(Settings.System.PIE_CONTROLS),
                    false, this, UserHandle.USER_ALL);
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(Settings.System.PA_PIE_STATE),
                    false, this, UserHandle.USER_ALL);
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(Settings.System.HEADS_UP_USER_ENABLED),
                    false, this, UserHandle.USER_ALL);
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(Settings.System.SLIM_ACTION_FLOATS),
                    false, this, UserHandle.USER_ALL);
            update();
        }

        void unobserve() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            update();
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            update();
        }

        public void update() {
            removeAllViews();
            createBarButtons();
        }
    }

    public void startSettingsActivity(final Intent intent) {
        mStatusBar.postStartSettingsActivity(intent, 0);
    }
}
