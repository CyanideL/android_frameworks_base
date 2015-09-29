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

package com.android.systemui.statusbar.phone;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.PorterDuff.Mode;
import android.hardware.TorchManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.systemui.R;

import com.android.internal.util.cm.WeatherController;
import com.android.internal.util.cm.WeatherControllerImpl;
import com.android.internal.util.cyanide.QsDeviceUtils;

import com.android.systemui.BatteryMeterView;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.NetworkController;

import java.net.URISyntaxException;
import java.text.NumberFormat;

public class StatusBarHeaderExpandedPanel extends RelativeLayout implements
        NetworkController.NetworkSignalChangedCallback,
        WeatherController.Callback {

    private static final String SETTINGS_METADATA_NAME = "com.android.settings";

    private final Context mContext;
    private final Handler mHandler;

    private ActivityStarter mActivityStarter;
    private NetworkController mNetworkController;
    private WeatherController mWeatherController;

    private ImageView mMobileSignalIconView;
    private ImageView mMobileDataTypeIconView;
    private ImageView mMobileActivityIconView;
    private Drawable mMobileSignalIcon;
    private Drawable mMobileDataTypeIcon;
    private Drawable mMobileActivityIcon;
    private TextView mCarrierText;

    private ImageView mWifiSignalIconView;
    private ImageView mWifiActivityIconView;
    private Drawable mWifiSignalIcon;
    private Drawable mWifiActivityIcon;
    private TextView mWifiText;

    private BatteryMeterView mBatteryMeterView;
    private TextView mBatteryPercentageText;
    private TextView mBatteryStatusText;

    private View mWeatherView;
    private TextView mWeatherText;

    private ImageView mQsSettingsButton;
    private ImageView mQsTorchButton;
    private ImageView mCyanideButton;

    private boolean mSupportsMobileData = true;
    private boolean mMobileNetworkEnabled = false;

    private boolean mWifiEnabled = false;
    private boolean mWifiConnected = false;

    private boolean mWeatherAvailable = false;

    private boolean mExpanded = false;

    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                updateBatteryInfo(intent);
            }
        }
    };

    public StatusBarHeaderExpandedPanel(Context context) {
        this(context, null);
    }

    public StatusBarHeaderExpandedPanel(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
        mHandler = new Handler();

        if (!QsDeviceUtils.deviceSupportsMobileData(mContext)) {
            mSupportsMobileData = false;
        }
    }

    public void setUp(ActivityStarter starter, BatteryController battery, NetworkController network,
            WeatherController weather) {
        mActivityStarter = starter;
        mNetworkController = network;
        mWeatherController = weather;

        mBatteryMeterView.setBatteryController(battery);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mWeatherView = findViewById(R.id.expanded_panel_weather);

        mMobileSignalIconView = (ImageView) findViewById(R.id.expanded_panel_mobile_signal_icon);
        mMobileDataTypeIconView = (ImageView) findViewById(R.id.expanded_panel_mobile_data_type_icon);
        mMobileActivityIconView = (ImageView) findViewById(R.id.expanded_panel_mobile_activity_icon);
        mWifiSignalIconView = (ImageView) findViewById(R.id.expanded_panel_wifi_signal_icon);
        mWifiActivityIconView = (ImageView) findViewById(R.id.expanded_panel_wifi_activity_icon);
        mBatteryMeterView = (BatteryMeterView) findViewById(R.id.expanded_panel_battery_icon);

        mCarrierText = (TextView) findViewById(R.id.expanded_panel_carrier_label);
        mWifiText = (TextView) findViewById(R.id.expanded_panel_wifi_label);
        mBatteryPercentageText = (TextView) findViewById(R.id.expanded_panel_battery_percentage);
        mBatteryStatusText = (TextView) findViewById(R.id.expanded_panel_battery_status);
        mWeatherText = (TextView) findViewById(R.id.expanded_panel_weather_text);

        mQsSettingsButton = (ImageView) findViewById(R.id.qs_settings_button);
        mCyanideButton = (ImageView) findViewById(R.id.cyanide_button);
        mQsTorchButton = (ImageView) findViewById(R.id.qs_torch_button);
        mQsTorchButton.setAlpha(128);

        mMobileSignalIconView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doHapticKeyClick(HapticFeedbackConstants.VIRTUAL_KEY);
                startCarrierActivity();
            }
        });

        mMobileSignalIconView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                doHapticKeyClick(HapticFeedbackConstants.VIRTUAL_KEY);
                startCarrierLongActivity();
            return true;
            }
        });

        mWifiSignalIconView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doHapticKeyClick(HapticFeedbackConstants.VIRTUAL_KEY);
                startWifiActivity();
            }
        });

        mWifiSignalIconView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                doHapticKeyClick(HapticFeedbackConstants.VIRTUAL_KEY);
                startWifiLongActivity();
            return true;
            }
        });

        mBatteryMeterView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doHapticKeyClick(HapticFeedbackConstants.VIRTUAL_KEY);
                startBatteryActivity();
            }
        });

        mBatteryMeterView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                doHapticKeyClick(HapticFeedbackConstants.VIRTUAL_KEY);
                startBatteryLongActivity();
            return true;
            }
        });

        mWeatherView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doHapticKeyClick(HapticFeedbackConstants.VIRTUAL_KEY);
                startForecastActivity();
            }
        });

        mWeatherView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                doHapticKeyClick(HapticFeedbackConstants.VIRTUAL_KEY);
                startWeatherActivity();
            return true;
            }
        });

        mCyanideButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doHapticKeyClick(HapticFeedbackConstants.VIRTUAL_KEY);
                startCyanideSettingsActivity();
            }
        });

        mCyanideButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                doHapticKeyClick(HapticFeedbackConstants.VIRTUAL_KEY);
                startCyanideLongSettingsActivity();
            return true;
            }
        });

        mQsSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doHapticKeyClick(HapticFeedbackConstants.VIRTUAL_KEY);
                startQsSettingsActivity();
            }
        });

        mQsSettingsButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                doHapticKeyClick(HapticFeedbackConstants.VIRTUAL_KEY);
                startHeaderSettingsActivity();
            return true;
            }
        });

        mQsTorchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doHapticKeyClick(HapticFeedbackConstants.VIRTUAL_KEY);
                startQsTorchActivity();
            }
        });
        updateLayouts();
    }

    private void updateLayouts() {
        if (!mSupportsMobileData) {
            findViewById(R.id.expanded_panel_mobile_network).setVisibility(View.GONE);
            LinearLayout wifiNetworkLayout = (LinearLayout) findViewById(R.id.expanded_panel_wifi_network);
            RelativeLayout.LayoutParams lp = (LayoutParams) wifiNetworkLayout.getLayoutParams();
            lp.removeRule(RelativeLayout.ALIGN_PARENT_END);
            lp.setMarginEnd(0);
            lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
            wifiNetworkLayout.setLayoutParams(lp);
        }
    }

    public void setListening(boolean listening) {
        if (listening) {
            mContext.registerReceiver(mBatteryInfoReceiver,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (mNetworkController != null) {
                mNetworkController.addNetworkSignalChangedCallback(this);
            }
            if (showWeather()) {
                mWeatherController.addCallback(this);
            }
        } else {
            mContext.unregisterReceiver(mBatteryInfoReceiver);
            mNetworkController.removeNetworkSignalChangedCallback(this);
            mWeatherController.removeCallback(this);
        }
    }

    public void updateVisibilities() {
        mWeatherView.setVisibility(showWeather() ? View.VISIBLE : View.INVISIBLE);
        mQsSettingsButton.setVisibility(showQsButton() ? View.VISIBLE : View.INVISIBLE);
        mQsTorchButton.setVisibility(showTorchButton() ? View.VISIBLE : View.INVISIBLE);
        mCyanideButton.setVisibility(showCyanideButton() ? View.VISIBLE : View.INVISIBLE);
    }

    public void updateClickTargets(boolean clickable) {
        mWeatherView.setClickable(showWeather() && clickable);
        mQsSettingsButton.setClickable(showQsButton() && clickable);
        mQsTorchButton.setClickable(showTorchButton() && clickable);
        mBatteryMeterView.setClickable(clickable);
        mWifiSignalIconView.setClickable(clickable);
        mMobileSignalIconView.setClickable(clickable);
        mCyanideButton.setClickable(showCyanideButton() && clickable);
    }

    private boolean showWeather() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_EXPANDED_HEADER_SHOW_WEATHER, 0) == 1;
    }

    private boolean showCyanideButton() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.SHOW_CYANIDE_BUTTON, 1) == 1;
    }

    private boolean showQsButton() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_EXPANDED_HEADER_SHOW_QS_BUTTON, 0) == 1;
    }

    private boolean showTorchButton() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_EXPANDED_HEADER_SHOW_TORCH_BUTTON, 0) == 1;
    }

    @Override
    public void onWifiSignalChanged(boolean enabled, boolean connected,
                int wifiSignalIconId, boolean activityIn, boolean activityOut,
                String wifiSignalContentDescriptionId, String description) {
        Drawable activityIcon = null;
        Drawable signalIcon = mContext.getResources().getDrawable(wifiSignalIconId);
        if (activityIn && !activityOut) {
            activityIcon = mContext.getResources().getDrawable(R.drawable.stat_sys_signal_in);
        } else if (!activityIn && activityOut) {
            activityIcon = mContext.getResources().getDrawable(R.drawable.stat_sys_signal_out);
        } else if (activityIn && activityOut) {
            activityIcon = mContext.getResources().getDrawable(R.drawable.stat_sys_signal_inout);
        }
        if (mWifiSignalIcon == null) {
            mWifiSignalIcon = signalIcon;
            mWifiSignalIconView.setImageDrawable(mWifiSignalIcon);
        } else if (mWifiSignalIcon != signalIcon) {
            mWifiSignalIcon = signalIcon;
            mWifiSignalIconView.setImageDrawable(mWifiSignalIcon);
        }
        if (mWifiActivityIcon == null) {
            mWifiActivityIcon = activityIcon;
            mWifiActivityIconView.setImageDrawable(mWifiActivityIcon);
        } else  if (mWifiActivityIcon != activityIcon) {
            mWifiActivityIcon = activityIcon;
            mWifiActivityIconView.setImageDrawable(mWifiActivityIcon);
        }
        mWifiEnabled = enabled;
        if (mWifiConnected != connected) {
            mWifiConnected = connected;
            mMobileActivityIconView.setVisibility(mWifiConnected ? View.INVISIBLE : View.VISIBLE);
        }
        updateWifiText(description);
    }

    @Override
    public void onMobileDataSignalChanged(boolean enabled, int mobileSignalIconId,
                String mobileSignalContentDescriptionId, int dataTypeIconId,
                boolean activityIn, boolean activityOut,
                String dataTypeContentDescriptionId, String description,
                boolean isDataTypeIconWide) {
        mMobileNetworkEnabled = enabled;
        if (mSupportsMobileData) {
            Drawable signalIcon = mContext.getResources().getDrawable(mobileSignalIconId);
            Drawable dataTypeIcon = null;
            Drawable activityIcon = null;
            if (dataTypeIconId > 0) {
                dataTypeIcon = mContext.getResources().getDrawable(dataTypeIconId);
            }
            if (activityIn && !activityOut) {
                activityIcon = mContext.getResources().getDrawable(R.drawable.stat_sys_signal_in);
            } else if (!activityIn && activityOut) {
                activityIcon = mContext.getResources().getDrawable(R.drawable.stat_sys_signal_out);
            } else if (activityIn && activityOut) {
                activityIcon = mContext.getResources().getDrawable(R.drawable.stat_sys_signal_inout);
            }
            if (mMobileSignalIcon == null) {
                mMobileSignalIcon = signalIcon;
                mMobileSignalIconView.setImageDrawable(mMobileSignalIcon);
            } else if (mMobileSignalIcon != signalIcon) {
                mMobileSignalIcon = signalIcon;
                mMobileSignalIconView.setImageDrawable(mMobileSignalIcon);
            }
            if (mMobileDataTypeIcon == null) {
                mMobileDataTypeIcon = dataTypeIcon;
                mMobileDataTypeIconView.setImageDrawable(mMobileDataTypeIcon);
            } else if (mMobileDataTypeIcon != dataTypeIcon) {
                mMobileDataTypeIcon = dataTypeIcon;
                mMobileDataTypeIconView.setImageDrawable(mMobileDataTypeIcon);
            }
            if (mMobileActivityIcon == null) {
                mMobileActivityIcon = activityIcon;
                mMobileActivityIconView.setImageDrawable(mMobileActivityIcon);
            } else if (mMobileActivityIcon != activityIcon) {
                mMobileActivityIcon = activityIcon;
                mMobileActivityIconView.setImageDrawable(mMobileActivityIcon);
            }
            setmMobileSignalIconPadding(mMobileDataTypeIcon, isDataTypeIconWide);
            updateCarrierlabel(description);
        }
    }

    private void setmMobileSignalIconPadding(Drawable icon, boolean isWide) {
        int padding = mContext.getResources().getDimensionPixelSize(
                R.dimen.mobil_data_type_icon_start_padding);
        int paddingToUse = 0;
        if (icon != null) {
            if (!isWide) {
                paddingToUse = padding;
            }
        }
        mMobileDataTypeIconView.setPaddingRelative(paddingToUse, 0, 0, 0);
    }

    @Override
    public  void onNoSimVisibleChanged(boolean visible) {
    }

    @Override
    public void onAirplaneModeChanged(boolean enabled) {
    }

    @Override
    public  void onMobileDataEnabled(boolean visible) {
    }

    @Override
    public void onWeatherChanged(WeatherController.WeatherInfo info) {
        if (info.temp != null && info.condition != null) {
            mWeatherAvailable = true;
        }
        updateWeather(info);
    }

    private void updateWifiText(String description) {
        String wifiName = removeDoubleQuotes(description);
        if (!mWifiEnabled) {
            wifiName = mContext.getResources().getString(
                    R.string.accessibility_wifi_off);
        } else if (!mWifiConnected) {
            wifiName = mContext.getResources().getString(
                    R.string.accessibility_no_wifi);
        } else if (wifiName == null) {
            wifiName = "--";
        }
        mWifiText.setText(wifiName);
    }

    private void updateCarrierlabel(String description) {
        String carrierLabel = removeDoubleQuotes(description);
        if (!mMobileNetworkEnabled) {
            carrierLabel = mContext.getResources().getString(
                    R.string.quick_settings_rssi_emergency_only);
        } else if (carrierLabel == null) {
            carrierLabel = "--";
        }
        mCarrierText.setText(carrierLabel);
    }

    private void updateBatteryInfo(Intent intent) {
        String batterylevel = getBatteryPercentageLevel(intent);
        String batteryStatus = getBatteryStatus(intent);
        mBatteryPercentageText.setText(batterylevel);
        mBatteryStatusText.setText((batteryStatus.isEmpty() ? "" : " (" + batteryStatus + ")"));
    }

    private void updateWeather(WeatherController.WeatherInfo info) {
        String weatherInfo = null;
        if (!mWeatherAvailable) {
            weatherInfo = mContext.getString(R.string.weather_info_not_available);
        } else {
            String city = info.city + ", ";
            String temp = info.temp;
            String condition = info.condition;
            weatherInfo = (showWeatherLocation() ? city : "") + temp + " - " + condition;
        }
        mWeatherText.setText(weatherInfo);
    }

    public void setIconColor(int color) {
        mMobileSignalIconView.setColorFilter(color, Mode.MULTIPLY);
        mMobileDataTypeIconView.setColorFilter(color, Mode.MULTIPLY);
        mMobileActivityIconView.setColorFilter(color, Mode.MULTIPLY);
        mWifiSignalIconView.setColorFilter(color, Mode.MULTIPLY);
        mWifiActivityIconView.setColorFilter(color, Mode.MULTIPLY);
        mQsSettingsButton.setColorFilter(color, Mode.MULTIPLY);
        mQsTorchButton.setColorFilter(color, Mode.MULTIPLY);
        mCyanideButton.setColorFilter(color, Mode.MULTIPLY);
    }

    public void setCyanideBackground(RippleDrawable background) {
        mCyanideButton.setBackground(background);
    }

    public void setCarrierBackground(RippleDrawable background) {
        mMobileSignalIconView.setBackground(background);
    }

    public void setWifiBackground(RippleDrawable background) {
        mWifiSignalIconView.setBackground(background);
    }

    public void setBatteryBackground(RippleDrawable background) {
        mBatteryMeterView.setBackground(background);
    }

    public void setWeatherViewBackground(RippleDrawable background) {
        mWeatherView.setBackground(background);
    }

    public void setQsSettingsBackground(RippleDrawable background) {
        mQsSettingsButton.setBackground(background);
    }

    public void setQsTorchBackground(RippleDrawable background) {
        mQsTorchButton.setBackground(background);
    }

    public void setTextColor(int color, boolean isOpaque) {
        if (isOpaque) {
            mCarrierText.setTextColor(color);
            mWifiText.setTextColor(color);
            mBatteryPercentageText.setTextColor(color);
            mWeatherText.setTextColor(color);
        } else {
            mBatteryStatusText.setTextColor(color);
        }
    }

    private static String removeDoubleQuotes(String string) {
        if (string == null) return null;
        final int length = string.length();
        if ((length > 1) && (string.charAt(0) == '"') && (string.charAt(length - 1) == '"')) {
            return string.substring(1, length - 1);
        }
        return string;
    }

    public static String removeTrailingPeriod(String string) {
        if (string == null) return null;
        final int length = string.length();
        if (string.endsWith(".")) {
            return string.substring(0, length - 1);
        }
        return string;
    }

    private String getBatteryPercentageLevel(Intent intent) {
        int level = (int)(100f
                * intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                / intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
        return NumberFormat.getPercentInstance().format((double) level / 100.0);
    }

    private String getBatteryStatus(Intent intent) {
        int plugType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                BatteryManager.BATTERY_STATUS_UNKNOWN);

        String emptyString = "";
        PackageManager pm = mContext.getPackageManager();
        if (pm == null) {
            return emptyString;
        }
        Resources settingsResources;
        try {
            settingsResources = pm.getResourcesForApplication(SETTINGS_METADATA_NAME);
        } catch (Exception e) {
            Log.e("StatusBarHeaderExpandedPanel:", "can't access settings resources",e);
            return emptyString;
        }

        int resId;
        if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
            if (plugType == BatteryManager.BATTERY_PLUGGED_AC) {
                resId = settingsResources.getIdentifier(
                    SETTINGS_METADATA_NAME + ":string/battery_info_status_charging_ac", null, null);
            } else if (plugType == BatteryManager.BATTERY_PLUGGED_USB) {
                resId = settingsResources.getIdentifier(
                    SETTINGS_METADATA_NAME + ":string/battery_info_status_charging_usb", null, null);
            } else if (plugType == BatteryManager.BATTERY_PLUGGED_WIRELESS) {
                resId = settingsResources.getIdentifier(
                    SETTINGS_METADATA_NAME + ":string/battery_info_status_charging_wireless", null, null);
            } else {
                resId = settingsResources.getIdentifier(
                    SETTINGS_METADATA_NAME + ":string/battery_info_status_charging", null, null);
            }
        } else if (status == BatteryManager.BATTERY_STATUS_DISCHARGING) {
            resId = settingsResources.getIdentifier(
                    SETTINGS_METADATA_NAME + ":string/battery_info_status_discharging", null, null);
        } else if (status == BatteryManager.BATTERY_STATUS_NOT_CHARGING) {
            resId = settingsResources.getIdentifier(
                    SETTINGS_METADATA_NAME + ":string/battery_info_status_not_charging", null, null);
        } else if (status == BatteryManager.BATTERY_STATUS_FULL) {
            resId = settingsResources.getIdentifier(
                    SETTINGS_METADATA_NAME + ":string/battery_info_status_full", null, null);
        } else {
            resId = settingsResources.getIdentifier(
                    SETTINGS_METADATA_NAME + ":string/battery_info_status_unknown", null, null);
        }
        return resId > 0 ? settingsResources.getString(resId) : emptyString;
    }

    private boolean showWeatherLocation() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_EXPANDED_HEADER_SHOW_WEATHER_LOCATION, 1) == 1;
    }

    private void startCarrierActivity() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName("com.android.settings",
            "com.android.settings.Settings$WirelessSettingsActivity");
        mActivityStarter.startActivity(intent, true /* dismissShade */);
    }

    private void startCarrierLongActivity() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName("com.android.settings",
            "com.android.settings.Settings$DataUsageSummaryActivity");
        mActivityStarter.startActivity(intent, true /* dismissShade */);
    }

    private void startWifiActivity() {
        mActivityStarter.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS),
        true /* dismissShade */);
    }

    private void startWifiLongActivity() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName("com.android.settings",
            "com.android.settings.Settings$TetherSettingsActivity");
        mActivityStarter.startActivity(intent, true /* dismissShade */);
    }

    private void startBatteryActivity() {
        mActivityStarter.startActivity(new Intent(Intent.ACTION_POWER_USAGE_SUMMARY),
        true /* dismissShade */);
    }

    private void startBatteryLongActivity() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName("com.android.settings",
            "com.android.settings.Settings$BatterySaverSettingsActivity");
        mActivityStarter.startActivity(intent, true /* dismissShade */);
    }

    private void startForecastActivity() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setComponent(WeatherControllerImpl.COMPONENT_WEATHER_FORECAST);
        mActivityStarter.startActivity(intent, true /* dismissShade */);
    }

    private void startWeatherActivity() {
        Intent weatherLongShortcutIntent = null;
        String weatherLongShortcutIntentUri = Settings.System.getStringForUser(
                mContext.getContentResolver(), Settings.System.WEATHER_LONG_SHORTCUT, UserHandle.USER_CURRENT);
        if(weatherLongShortcutIntentUri != null) {
            try {
                weatherLongShortcutIntent = Intent.parseUri(weatherLongShortcutIntentUri, 0);
            } catch (URISyntaxException e) {
                weatherLongShortcutIntent = null;
            }
        }

        if(weatherLongShortcutIntent != null) {
            mActivityStarter.startActivity(weatherLongShortcutIntent, true);
        } else {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName("com.cyanogenmod.lockclock",
                "com.cyanogenmod.lockclock.preference.Preferences");
            mActivityStarter.startActivity(intent, true /* dismissShade */);
        }
    }

    private void startCyanideSettingsActivity() {
        Intent cyanideShortcutIntent = null;
        String cyanideShortcutIntentUri = Settings.System.getStringForUser(
                mContext.getContentResolver(), Settings.System.CYANIDE_SHORTCUT, UserHandle.USER_CURRENT);
        if(cyanideShortcutIntentUri != null) {
            try {
                cyanideShortcutIntent = Intent.parseUri(cyanideShortcutIntentUri, 0);
            } catch (URISyntaxException e) {
                cyanideShortcutIntent = null;
            }
        }

        if(cyanideShortcutIntent != null) {
            mActivityStarter.startActivity(cyanideShortcutIntent, true);
        } else {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.android.settings",
                "com.android.settings.Settings$MainSettingsActivity"));
            mActivityStarter.startActivity(intent, true /* dismissShade */);
        }
    }

    private void startCyanideLongSettingsActivity() {
        Intent cyanideLongShortcutIntent = null;
        String cyanideLongShortcutIntentUri = Settings.System.getStringForUser(
                mContext.getContentResolver(), Settings.System.CYANIDE_LONG_SHORTCUT, UserHandle.USER_CURRENT);
        if(cyanideLongShortcutIntentUri != null) {
            try {
                cyanideLongShortcutIntent = Intent.parseUri(cyanideLongShortcutIntentUri, 0);
            } catch (URISyntaxException e) {
                cyanideLongShortcutIntent = null;
            }
        }

        if(cyanideLongShortcutIntent != null) {
            mActivityStarter.startActivity(cyanideLongShortcutIntent, true);
        } else {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.android.settings",
                "com.android.settings.Settings$CyanideCentralActivity"));
             mActivityStarter.startActivity(intent, true /* dismissShade */);
         }
     }

    private void startQsSettingsActivity() {
        Intent QSShortcutIntent = null;
        String QSShortcutIntentUri = Settings.System.getStringForUser(
                mContext.getContentResolver(), Settings.System.HEADER_BUTTON_SHORTCUT, UserHandle.USER_CURRENT);
        if(QSShortcutIntentUri != null) {
            try {
                QSShortcutIntent = Intent.parseUri(QSShortcutIntentUri, 0);
            } catch (URISyntaxException e) {
                QSShortcutIntent = null;
            }
        }

        if(QSShortcutIntent != null) {
            mActivityStarter.startActivity(QSShortcutIntent, true);
        } else {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.android.settings",
                "com.android.settings.Settings$NotificationDrawerSettings"));
            mActivityStarter.startActivity(intent, true /* dismissShade */);
        }
    }

    private void startHeaderSettingsActivity() {
        Intent QSLongShortcutIntent = null;
        String QSLongShortcutIntentUri = Settings.System.getStringForUser(
                mContext.getContentResolver(), Settings.System.HEADER_BUTTON_LONG_SHORTCUT, UserHandle.USER_CURRENT);
        if(QSLongShortcutIntentUri != null) {
            try {
                QSLongShortcutIntent = Intent.parseUri(QSLongShortcutIntentUri, 0);
            } catch (URISyntaxException e) {
                QSLongShortcutIntent = null;
            }
        }

        if(QSLongShortcutIntent != null) {
            mActivityStarter.startActivity(QSLongShortcutIntent, true);
        } else {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.android.settings",
                "com.android.settings.Settings$StatusBarExpandedHeaderSettingsActivity"));
            mActivityStarter.startActivity(intent, true /* dismissShade */);
        }
    }

    private void startQsTorchActivity() {
        Resources res = mContext.getResources();
        ImageView image = (ImageView)
                findViewById(R.id.qs_torch_button);

        TorchManager torchManager =
               (TorchManager) mContext.getSystemService(Context.TORCH_SERVICE);
       if (!torchManager.isTorchOn()) {
           torchManager.setTorchEnabled(true);
           image.setImageDrawable(
                    res.getDrawable(R.drawable.ic_qs_button_torch));
           mQsTorchButton.setAlpha(255);
       } else {
           torchManager.setTorchEnabled(false);
           image.setImageDrawable(res.getDrawable(
                R.drawable.ic_qs_button_torch_off));
           mQsTorchButton.setAlpha(128);
       }
       return;
    }

    private void doHapticKeyClick(int type) {
        performHapticFeedback(type,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                | HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
    }
}
