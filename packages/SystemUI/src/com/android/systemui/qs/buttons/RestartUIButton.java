/*
 * Copyright (C) 2015 CyanideL & Fusion
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

package com.android.systemui.qs.buttons;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.UserHandle;

import com.android.systemui.R;
import com.android.systemui.qs.QSBar;

import com.cyanide.util.Helpers;

public class RestartUIButton extends QSButton {

    private final ContentObserver mRestartUIObserver;
    private final ContentResolver mResolver;

    private boolean mEnabled;

    public RestartUIButton(Context context, QSBar qsBar, Drawable iconEnabled,
            Drawable iconDisabled) {
        super(context, qsBar, iconEnabled, iconDisabled);

        mRestartUIObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                mEnabled = true;
                updateState(mEnabled);
            }
        };
        mResolver = mContext.getContentResolver();
        mEnabled = true;
        updateState(mEnabled);
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            mEnabled = true;
        } else {
            mResolver.unregisterContentObserver(mRestartUIObserver);
        }
    }

    @Override
    public void handleClick() {
        Helpers.restartSystemUI();
    }

    @Override
    public void handleLongClick() {
    }
}
