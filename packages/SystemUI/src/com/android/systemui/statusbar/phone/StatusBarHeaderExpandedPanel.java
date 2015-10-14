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
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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
import com.android.systemui.statusbar.policy.StatusBarHeaderTraffic;

import java.net.URISyntaxException;
import java.text.NumberFormat;
import java.util.Date;

public class StatusBarHeaderExpandedPanel extends RelativeLayout implements
        NetworkController.NetworkSignalChangedCallback,
        WeatherController.Callback {

    private static final String SETTINGS_METADATA_NAME = "com.android.settings";

    private final Context mContext;
    private final Handler mHandler;

    private ActivityStarter mActivityStarter;
    private NetworkController mNetworkController;
    private WeatherController mWeatherController;
    private WifiManager mWifiManager;
 
    private View mButtonPanel;
    private View mDevicePanel;
    private View mWeatherPanel;

    private View mMobileNetworkTextLayout;
    private View mWifiNetworkTextLayout;
    private View mBatteryTextLayout;

    private TextView mCarrierTextView;
    private TextView mNetworkTypeTextView;
    private ImageView mMobileSignalIconView;
    private Drawable mMobileSignalIcon;

    private TextView mWifiNameTextView;
    private TextView mWifiLinkSpeedTextView;
    private ImageView mWifiSignalIconView;
    private Drawable mWifiSignalIcon;

    private TextView mBatteryPercentageTextView;
    private TextView mBatteryStatusTextView;
    private BatteryMeterView mBatteryMeterView;
    private StatusBarHeaderTraffic mTraffic;
    private ImageView mActivityIconView;
    private TextView mWeatherPanelNoWeatherText;
    private View mWeatherPanelLayout;
    private TextView mWeatherPanelCity;
    private TextView mWeatherPanelWind;
    private ImageView mWeatherPanelConditionImage;
    private TextView mWeatherPanelTempCondition;
    private TextView mWeatherPanelHumidity;
    private TextView mWeatherPanelTimestamp;

    private boolean mPanelButtonsVisible = false;
    private boolean mPanelDeviceInfoVisible = false;
    private boolean mPanelWeatherInfoVisible = false;

    private ImageView mQsSettingsButton;
    private ImageView mQsTorchButton;
    private ImageView mCyanideButton;
    private ImageView mCameraButton;

    private boolean mSupportsMobileData = true;
    private boolean mMobileNetworkEnabled = false;

    private boolean mWifiEnabled = false;
    private boolean mWifiConnected = false;

    private boolean mWeatherAvailable = false;

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
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        mBatteryMeterView.setBatteryController(battery);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mButtonPanel = findViewById(R.id.expanded_panel_button_panel);
        mDevicePanel = findViewById(R.id.expanded_panel_device_panel);
        mWeatherPanel = findViewById(R.id.expanded_panel_weather_panel);

        mMobileNetworkTextLayout = findViewById(R.id.mobile_network_text_layout);
        mWifiNetworkTextLayout = findViewById(R.id.wifi_network_text_layout);
        mBatteryTextLayout = findViewById(R.id.battery_text_layout);

        mCarrierTextView = (TextView) findViewById(R.id.mobile_network_carrier_text_view);
        mNetworkTypeTextView = (TextView) findViewById(R.id.mobile_network_type_text_view);

        mMobileSignalIconView = (ImageView) findViewById(R.id.expanded_panel_mobile_signal_icon);
        mWifiNameTextView = (TextView) findViewById(R.id.wifi_network_wifi_name_text_view);
        mWifiLinkSpeedTextView = (TextView) findViewById(R.id.wifi_network_wifi_link_speed_text_view);
        mWifiSignalIconView = (ImageView) findViewById(R.id.expanded_panel_wifi_signal_icon);
        mBatteryPercentageTextView = (TextView) findViewById(R.id.battery_percentage_text_view);
        mBatteryStatusTextView = (TextView) findViewById(R.id.battery_status_text_view);
        mBatteryMeterView = (BatteryMeterView) findViewById(R.id.expanded_panel_battery_icon);

        mQsSettingsButton = (ImageView) findViewById(R.id.qs_settings_button);
        mCyanideButton = (ImageView) findViewById(R.id.cyanide_button);
        mCameraButton = (ImageView) findViewById(R.id.camera_button);
        mQsTorchButton = (ImageView) findViewById(R.id.qs_torch_button);
        if (mQsTorchButton != null) mQsTorchButton.setAlpha(128);

        mTraffic = (StatusBarHeaderTraffic) findViewById(R.id.expanded_panel_traffic_layout);

        mWeatherPanelNoWeatherText = (TextView) findViewById(R.id.weather_panel_no_weather_text);
        mWeatherPanelLayout = findViewById(R.id.weather_panel_layout);
        mWeatherPanelCity = (TextView) findViewById(R.id.weather_panel_city);
        mWeatherPanelWind = (TextView) findViewById(R.id.weather_panel_wind);
        mWeatherPanelConditionImage = (ImageView) findViewById(R.id.weather_panel_weather_image);
        mWeatherPanelTempCondition = (TextView) findViewById(R.id.weather_panel_temp_condition);
        mWeatherPanelHumidity = (TextView) findViewById(R.id.weather_panel_humidity);
        mWeatherPanelTimestamp = (TextView) findViewById(R.id.weather_panel_timestamp);

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

        mCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doHapticKeyClick(HapticFeedbackConstants.VIRTUAL_KEY);
                startCameraActivity();
            }
        });

        mCameraButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                doHapticKeyClick(HapticFeedbackConstants.VIRTUAL_KEY);
                startCameraLongActivity();
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

        mWeatherPanelLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doHapticKeyClick(HapticFeedbackConstants.VIRTUAL_KEY);
                startForecastActivity();
            }
        });

        mWeatherPanelLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                doHapticKeyClick(HapticFeedbackConstants.VIRTUAL_KEY);
                startWeatherActivity();
            return true;
            }
        });
    
        updateTrafficLayout();
        swapPanels(false);
    }

    private void updateTrafficLayout() {
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mTraffic.getLayoutParams();
        lp.removeRule(RelativeLayout.ALIGN_TOP);
        lp.removeRule(RelativeLayout.START_OF);
        lp.removeRule(RelativeLayout.END_OF);
        int ruleText = mWifiNetworkTextLayout.getId();
        int ruleIcon = mWifiSignalIconView.getId();
        if (mSupportsMobileData && !mWifiConnected) {
            ruleText = mMobileNetworkTextLayout.getId();
            ruleIcon = mMobileSignalIconView.getId();
        }
        if (mSupportsMobileData && !mPanelButtonsVisible) {
            ruleText = mCameraButton.getId();
            ruleIcon = mCyanideButton.getId();
        }
        lp.addRule(RelativeLayout.ALIGN_TOP, ruleText);
        lp.addRule(RelativeLayout.START_OF, ruleIcon);
        lp.addRule(RelativeLayout.END_OF, ruleText);
        if (!mSupportsMobileData) {
            mMobileNetworkTextLayout.setVisibility(View.GONE);
            mMobileSignalIconView.setVisibility(View.GONE);
        }
    }

    public void swapPanels(boolean animate) {
        if (!mPanelDeviceInfoVisible) {
            mButtonPanel.setVisibility(View.GONE);
            mWeatherPanel.setVisibility(View.INVISIBLE);
            mDevicePanel.setVisibility(View.VISIBLE);
            if (animate) {
                mDevicePanel.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.push_left_in));
                mButtonPanel.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.push_right_out));
             }
             mPanelWeatherInfoVisible = false;
             mPanelDeviceInfoVisible = true;
        } else if (!mPanelWeatherInfoVisible) {
            mButtonPanel.setVisibility(View.INVISIBLE);
            mDevicePanel.setVisibility(View.GONE);
            mWeatherPanel.setVisibility(View.VISIBLE);
            if (animate) {
                mWeatherPanel.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.push_left_in));
                mDevicePanel.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.push_right_out));
             }
             mPanelButtonsVisible = false;
             mPanelWeatherInfoVisible = true;
         } else if (!mPanelButtonsVisible) {
            mDevicePanel.setVisibility(View.INVISIBLE);
            mWeatherPanel.setVisibility(View.GONE);
            mButtonPanel.setVisibility(View.VISIBLE);
            if (animate) {
                mButtonPanel.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.push_left_in));
                mWeatherPanel.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.push_right_out));
             }
             mPanelDeviceInfoVisible = false;
             mPanelButtonsVisible = true;
         }
     }

    public void setListening(boolean listening) {
        if (listening) {
            mContext.registerReceiver(mBatteryInfoReceiver,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (mNetworkController != null) {
                mNetworkController.addNetworkSignalChangedCallback(this);
            }
            mWeatherController.addCallback(this);
        } else {
            mContext.unregisterReceiver(mBatteryInfoReceiver);
            mNetworkController.removeNetworkSignalChangedCallback(this);
            mWeatherController.removeCallback(this);
        }
        mTraffic.setListening(listening);
    }

    public void updateClickTargets(boolean clickable) {
        mWeatherPanelLayout.setClickable(clickable);
        mQsSettingsButton.setClickable(clickable);
        mQsTorchButton.setClickable(clickable);
        mBatteryMeterView.setClickable(clickable);
        mWifiSignalIconView.setClickable(clickable);
        mMobileSignalIconView.setClickable(clickable);
        mCameraButton.setClickable(clickable);
        mCyanideButton.setClickable(clickable);
    }

    @Override
    public void onWifiSignalChanged(boolean enabled, boolean connected,
                int wifiSignalIconId, boolean activityIn, boolean activityOut,
                String wifiSignalContentDescriptionId, String description) {
        Drawable signalIcon = mContext.getResources().getDrawable(wifiSignalIconId);
        if (mWifiSignalIcon == null) {
            mWifiSignalIcon = signalIcon;
            mWifiSignalIconView.setImageDrawable(mWifiSignalIcon);
        } else if (mWifiSignalIcon != signalIcon) {
            mWifiSignalIcon = signalIcon;
            mWifiSignalIconView.setImageDrawable(mWifiSignalIcon);
        }
        mWifiEnabled = enabled;
        mWifiConnected = connected;
        updateWifiText(description);
        if (mSupportsMobileData) {
            updateTrafficLayout();
        }
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
            if (mMobileSignalIcon == null) {
                mMobileSignalIcon = signalIcon;
                mMobileSignalIconView.setImageDrawable(mMobileSignalIcon);
            } else if (mMobileSignalIcon != signalIcon) {
                mMobileSignalIcon = signalIcon;
                mMobileSignalIconView.setImageDrawable(mMobileSignalIcon);
            }
            updateCarrierlabel(description, dataTypeContentDescriptionId);
        }
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
        String wifiLinkSpeed = "";
        if (mWifiEnabled && mWifiConnected) {
            WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                wifiLinkSpeed = "  (" + wifiInfo.getLinkSpeed() + WifiInfo.LINK_SPEED_UNITS + ")";
            }
        }
        if (!mWifiEnabled) {
            wifiName = mContext.getResources().getString(
                    R.string.accessibility_wifi_off);
        } else if (!mWifiConnected) {
            wifiName = mContext.getResources().getString(
                    R.string.accessibility_no_wifi);
        } else if (wifiName == null) {
            wifiName = "--";
        }
        mWifiNameTextView.setText(wifiName);
        mWifiLinkSpeedTextView.setText(wifiLinkSpeed);
    }

    private void updateCarrierlabel(String description, String dataTypeContentDescription) {
        String carrierLabel = removeDoubleQuotes(description);
        String networkType = "  (" + dataTypeContentDescription + ")";
        if (!mMobileNetworkEnabled) {
            carrierLabel = mContext.getResources().getString(
                    R.string.quick_settings_rssi_emergency_only);
        } else if (carrierLabel == null) {
            carrierLabel = "--";
        }
        mCarrierTextView.setText(carrierLabel);
        mNetworkTypeTextView.setText(networkType);
    }

    private void updateBatteryInfo(Intent intent) {
        String batterylevel = getBatteryPercentageLevel(intent);
        String batteryStatus = getBatteryStatus(intent);
        mBatteryPercentageTextView.setText(batterylevel);
        mBatteryStatusTextView.setText((batteryStatus.isEmpty() ? "" : " (" + batteryStatus + ")"));
    }

    private void updateWeather(WeatherController.WeatherInfo info) {
        String weatherInfo = null;
        if (!mWeatherAvailable) {
            weatherInfo = mContext.getString(R.string.weather_info_not_available);
            mWeatherPanelNoWeatherText.setVisibility(View.VISIBLE);
            mWeatherPanelLayout.setVisibility(View.GONE);
            mWeatherPanelTimestamp.setVisibility(View.GONE);
        } else {
            mWeatherPanelNoWeatherText.setVisibility(View.GONE);
            mWeatherPanelLayout.setVisibility(View.VISIBLE);
            mWeatherPanelCity.setVisibility(showWeatherLocation() ? View.VISIBLE : View.GONE);
            mWeatherPanelTimestamp.setVisibility(View.VISIBLE);
            mWeatherPanelCity.setText(info.city);
            mWeatherPanelWind.setText(info.wind);
            mWeatherPanelConditionImage.setImageDrawable(info.conditionDrawableMonochrome);
            mWeatherPanelTempCondition.setText(info.temp + ", " + info.condition);
            mWeatherPanelHumidity.setText(info.humidity);
            mWeatherPanelTimestamp.setText(getCurrentDate());
        }
    }

    private String getCurrentDate() {
        Date now = new Date();
        long nowMillis = now.getTime();
        StringBuilder sb = new StringBuilder();
        sb.append(DateFormat.format("E", nowMillis));
        sb.append(" ");
        sb.append(DateFormat.getTimeFormat(getContext()).format(nowMillis));
        return sb.toString();
    }

    public void setIconColor(int color) {
        if (mSupportsMobileData) {
            mMobileSignalIconView.setColorFilter(color, Mode.MULTIPLY);
        }
        mWifiSignalIconView.setColorFilter(color, Mode.MULTIPLY);
        mTraffic.updateIconColor(color);
        mWeatherPanelConditionImage.setColorFilter(color, Mode.MULTIPLY);
        mQsSettingsButton.setColorFilter(color, Mode.MULTIPLY);
        mQsTorchButton.setColorFilter(color, Mode.MULTIPLY);
        mCyanideButton.setColorFilter(color, Mode.MULTIPLY);
        mCameraButton.setColorFilter(color, Mode.MULTIPLY);
    }

    public void setCameraBackground(RippleDrawable background) {
        mCameraButton.setBackground(background);
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

    public void setQsSettingsBackground(RippleDrawable background) {
        mQsSettingsButton.setBackground(background);
    }

    public void setQsTorchBackground(RippleDrawable background) {
        mQsTorchButton.setBackground(background);
    }

    public void setTextColor(int color, boolean isOpaque) {
        if (isOpaque) {
            if (mSupportsMobileData) {
                mCarrierTextView.setTextColor(color);
            }
            mWifiNameTextView.setTextColor(color);
            mBatteryPercentageTextView.setTextColor(color);
            mTraffic.updateTextColor(color, true);
            mWeatherPanelNoWeatherText.setTextColor(color);
            mWeatherPanelCity.setTextColor(color);
            mWeatherPanelTempCondition.setTextColor(color);
        } else {
            if (mSupportsMobileData) {
                mNetworkTypeTextView.setTextColor(color);
            }
            mWifiLinkSpeedTextView.setTextColor(color);
            mBatteryStatusTextView.setTextColor(color);
            mTraffic.updateTextColor(color, false);
            mWeatherPanelHumidity.setTextColor(color);
            mWeatherPanelWind.setTextColor(color);
            mWeatherPanelTimestamp.setTextColor(color);
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

    private void startCameraActivity() {
        Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA, null);
        mActivityStarter.startActivity(intent, true /* dismissShade */);
    }

    private void startCameraLongActivity() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName("com.android.gallery3d",
            "com.android.gallery3d.app.GalleryActivity");
        mActivityStarter.startActivity(intent, true /* dismissShade */);
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
