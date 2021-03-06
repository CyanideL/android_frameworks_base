/*
 * Copyright (C) 2016 DarkKat
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

package com.android.systemui.cyanide;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.Typeface;
import android.graphics.PorterDuff.Mode;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.util.cyanide.WeatherServiceController;

import com.android.keyguard.AlphaOptimizedLinearLayout;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;

import com.android.internal.util.cyanide.FontHelper;

public class StatusBarWeather extends AlphaOptimizedLinearLayout implements
        WeatherServiceController.Callback {

    private static final int WEATHER_TYPE_TEXT      = 0;
    private static final int WEATHER_TYPE_ICON      = 1;
    private static final int WEATHER_TYPE_TEXT_ICON = 2;

    private WeatherServiceController mWeatherController;

    private TextView mTextView;
    private ImageView mIconView;

    private boolean mShow = false;
    public static Typeface mFontStyle;

    public StatusBarWeather(Context context) {
        this(context, null);
    }

    public StatusBarWeather(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StatusBarWeather(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mTextView = (TextView) findViewById(R.id.status_bar_weather_text);
        mIconView = (ImageView) findViewById(R.id.status_bar_weather_icon);
        mTextView.setTypeface(mFontStyle);
        updateFontSize();
        updateFontStyle();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (mWeatherController != null) {
            mWeatherController.addCallback(this);
        }
    }

    public void setListening(boolean listening) {
        if (mWeatherController == null) {
            return;
        }
        if (listening) {
            mWeatherController.addCallback(this);
        } else {
            mWeatherController.removeCallback(this);
        }
    }

    public void setWeatherController(WeatherServiceController wc) {
        mWeatherController = wc;
    }

    public void onDensityOrFontScaleChanged() {
        int startPadding = getContext().getResources().getDimensionPixelSize(
                R.dimen.weather_layout_start_padding);
        int iconSize = getContext().getResources().getDimensionPixelSize(
                com.android.internal.R.dimen.status_bar_icon_size);
        int iconPadding = getContext().getResources().getDimensionPixelSize(
                R.dimen.status_bar_icon_padding);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, iconSize);
        setPaddingRelative(startPadding, 0, 0, 0);
        FontSizeUtils.updateFontSize(mTextView, R.dimen.status_bar_clock_size);
        mIconView.setLayoutParams(lp);
        mIconView.setPadding(0, iconPadding, 0, iconPadding);
    }

    @Override
    public void onWeatherChanged(WeatherServiceController.WeatherInfo info) {
        if (info.temp != null && info.condition != null) {
            if (mShow) {
                if (getVisibility() != View.VISIBLE) {
                    setVisibility(View.VISIBLE);
                }
            }
            mTextView.setText(info.temp);
            Drawable icon = info.conditionDrawableMonochrome.getConstantState().newDrawable();
            mIconView.setImageDrawable(icon);
        } else {
            if (getVisibility() != View.GONE) {
                setVisibility(View.GONE);
            }
            mTextView.setText("");
            mIconView.setImageDrawable(null);
        }

    }

    public void setShow(boolean show) {
        mShow = show;
        if (mShow) {
            if (getVisibility() != View.VISIBLE) {
                setVisibility(View.VISIBLE);
            }
            if (mWeatherController != null) {
                mWeatherController.addCallback(this);
            }
        } else {
            if (getVisibility() != View.GONE) {
                setVisibility(View.GONE);
            }
            if (mWeatherController != null) {
                mWeatherController.removeCallback(this);
            }
        }
    }

    public void setType(int type) {
        final boolean showText = mShow && type == WEATHER_TYPE_TEXT || type == WEATHER_TYPE_TEXT_ICON;
        final boolean showIcon = mShow && type == WEATHER_TYPE_ICON || type == WEATHER_TYPE_TEXT_ICON;

        mTextView.setVisibility(showText ? View.VISIBLE : View.GONE);
        mIconView.setVisibility(showIcon ? View.VISIBLE : View.GONE);
    }

    public void setTextColor(int color) {
        if (mTextView != null) {
            mTextView.setTextColor(color);
        }
    }

    public void setIconColor(int color) {
        if (mIconView != null) {
            mIconView.setColorFilter(color, Mode.MULTIPLY);
        }
    }

    public void setTypeface(Typeface tf) {
        mTextView.setTypeface(tf);
    }

    public boolean shouldShow() {
        return mShow;
    }

    public void updateFontStyle() {
        final int mWeatherFontStyle = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_WEATHER_FONT_STYLES, FontHelper.FONT_COMINGSOON);

        getFontStyle(mWeatherFontStyle);
    }

    public void getFontStyle(int font) {
        switch (font) {
            case FontHelper.FONT_NORMAL:
            default:
                mFontStyle = FontHelper.NORMAL;
                break;
            case FontHelper.FONT_ITALIC:
                mFontStyle = FontHelper.ITALIC;
                break;
            case FontHelper.FONT_BOLD:
                mFontStyle = FontHelper.BOLD;
                break;
            case FontHelper.FONT_BOLD_ITALIC:
                mFontStyle = FontHelper.BOLD_ITALIC;
                break;
            case FontHelper.FONT_LIGHT:
                mFontStyle = FontHelper.LIGHT;
                break;
            case FontHelper.FONT_LIGHT_ITALIC:
                mFontStyle = FontHelper.LIGHT_ITALIC;
                break;
            case FontHelper.FONT_THIN:
                mFontStyle = FontHelper.THIN;
                break;
            case FontHelper.FONT_THIN_ITALIC:
                mFontStyle = FontHelper.THIN_ITALIC;
                break;
            case FontHelper.FONT_CONDENSED:
                mFontStyle = FontHelper.CONDENSED;
                break;
            case FontHelper.FONT_CONDENSED_ITALIC:
                mFontStyle = FontHelper.CONDENSED_ITALIC;
                break;
            case FontHelper.FONT_CONDENSED_LIGHT:
                mFontStyle = FontHelper.CONDENSED_LIGHT;
                break;
            case FontHelper.FONT_CONDENSED_LIGHT_ITALIC:
                mFontStyle = FontHelper.CONDENSED_LIGHT_ITALIC;
                break;
            case FontHelper.FONT_CONDENSED_BOLD:
                mFontStyle = FontHelper.CONDENSED_BOLD;
                break;
            case FontHelper.FONT_CONDENSED_BOLD_ITALIC:
                mFontStyle = FontHelper.CONDENSED_BOLD_ITALIC;
                break;
            case FontHelper.FONT_MEDIUM:
                mFontStyle = FontHelper.MEDIUM;
                break;
            case FontHelper.FONT_MEDIUM_ITALIC:
                mFontStyle = FontHelper.MEDIUM_ITALIC;
                break;
            case FontHelper.FONT_BLACK:
                mFontStyle = FontHelper.BLACK;
                break;
            case FontHelper.FONT_BLACK_ITALIC:
                mFontStyle = FontHelper.BLACK_ITALIC;
                break;
            case FontHelper.FONT_DANCINGSCRIPT:
                mFontStyle = FontHelper.DANCINGSCRIPT;
                break;
            case FontHelper.FONT_DANCINGSCRIPT_BOLD:
                mFontStyle = FontHelper.DANCINGSCRIPT_BOLD;
                break;
            case FontHelper.FONT_COMINGSOON:
                mFontStyle = FontHelper.COMINGSOON;
                break;
            case FontHelper.FONT_NOTOSERIF:
                mFontStyle = FontHelper.NOTOSERIF;
                break;
            case FontHelper.FONT_NOTOSERIF_ITALIC:
                mFontStyle = FontHelper.NOTOSERIF_ITALIC;
                break;
            case FontHelper.FONT_NOTOSERIF_BOLD:
                mFontStyle = FontHelper.NOTOSERIF_BOLD;
                break;
            case FontHelper.FONT_NOTOSERIF_BOLD_ITALIC:
                mFontStyle = FontHelper.NOTOSERIF_BOLD_ITALIC;
                break;
        }
        mTextView.setTypeface(mFontStyle);
    }

    public void updateFontSize() {
         final int mFontSize = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_WEATHER_FONT_SIZE, 18);
        mTextView.setTextSize(mFontSize);
    }
}
