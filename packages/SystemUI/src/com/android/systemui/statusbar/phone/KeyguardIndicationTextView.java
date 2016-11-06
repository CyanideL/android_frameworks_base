/*
 * Copyright (C) 2014 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

/**
 * A view to show hints on Keyguard ("Swipe up to unlock", "Tap again to open").
 */
public class KeyguardIndicationTextView extends TextView {

    public KeyguardIndicationTextView(Context context) {
        super(context);
    }

    public KeyguardIndicationTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public KeyguardIndicationTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public KeyguardIndicationTextView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Changes the text with an animation and makes sure a single indication is shown long enough.
     *
     * @param text The text to show.
     */
    public void switchIndication(CharSequence text, String customText, boolean useCustomText) {

        /*final boolean mUseCustomText = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.KEYGUARD_INDICATOR_TEXT_CUSTOM, 0) == 1;
        final String mCustomText = Settings.System.getString(mContext.getContentResolver(),
                Settings.System.KEYGUARD_INDICATOR_CUSTOM_TEXT);*/

        // TODO: Animation, make sure that we will show one indication long enough.
        if (TextUtils.isEmpty(text)) {
            setVisibility(View.INVISIBLE);
        } else {
            setVisibility(View.VISIBLE);
            if (useCustomText) {
                setText(customText);
            } else {
                setText(text);
            }
        }
    }

    /**
     * See {@link #switchIndication}.
     */
    public void switchIndication(int textResId, String customText, boolean useCustomText) {
        switchIndication(getResources().getText(textResId), customText, useCustomText);
    }
}
