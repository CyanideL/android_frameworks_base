/*
 * Copyright (C) 2016 Cyanide Android (rogersb11)
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

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff.Mode;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.systemui.R;

import java.io.InputStream;
import com.android.internal.util.cyanide.FontHelper;

public class CustomLabel extends LinearLayout {

    private Context mContext;
    private ContentResolver mResolver;

    private TextView mTextView;

    private boolean mShowLabel = false;
    public static Typeface mFontStyle;

    public CustomLabel(Context context) {
        this(context, null);
    }

    public CustomLabel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomLabel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        mResolver = mContext.getContentResolver();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mTextView = (TextView) findViewById(R.id.status_bar_text);
    }

    public void setShow(boolean show) {
        mShowLabel = show;
        if (mShowLabel) {
            if (getVisibility() != View.VISIBLE) {
                setVisibility(View.VISIBLE);
            }
        } else {
            if (getVisibility() != View.GONE) {
                setVisibility(View.GONE);
            }
        }
    }

    public boolean shouldShow() {
        return mShowLabel;
    }

    public void setCustomText(String text) {
        mTextView.setText(text);
    }

    public void setTextColor(int color) {
        if (mTextView != null) {
            mTextView.setTextColor(color);
        }
    }

    public void updateFontStyle() {
        final int mViewFontStyle = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_CUSTOM_LABEL_FONT_STYLE, FontHelper.FONT_COMINGSOON);

        getFontStyle(mViewFontStyle);
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
                Settings.System.STATUS_BAR_CUSTOM_LABEL_FONT_SIZE, 16);
        mTextView.setTextSize(mFontSize);
    }
}
