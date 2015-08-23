/*
 * Copyright (C) 2015 CyanideL
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
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.provider.Settings;

import com.android.systemui.R;
import com.android.systemui.qs.QSBar;

public class CyanideButton extends QSButton {

    private final ContentObserver mCyanideObserver;
    private final ContentResolver mResolver;

    private boolean mEnabled;

    public CyanideButton(Context context, QSBar qsBar, Drawable iconEnabled,
            Drawable iconDisabled) {
        super(context, qsBar, iconEnabled, iconDisabled);

        mCyanideObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                mEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                        Settings.Secure.ADVANCED_MODE, 0) == 1;
                updateState(mEnabled);
            }
        };
        mResolver = mContext.getContentResolver();
        mEnabled = Settings.Secure.getInt(mResolver,
                Settings.Secure.ADVANCED_MODE, 0) == 1;
        updateState(mEnabled);
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            mResolver.registerContentObserver(
                    Settings.Secure.getUriFor(Settings.Secure.ADVANCED_MODE),
                    false, mCyanideObserver);
        } else {
            mResolver.unregisterContentObserver(mCyanideObserver);
        }
    }

    @Override
    public void handleClick() {
        if (mEnabled) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName("com.android.settings",
                "com.android.settings.Settings$MainSettingsActivity");
            mQSBar.startSettingsActivity(intent);
        }
    }

    @Override
    public void handleLongClick() {
        boolean mDisabled = Settings.Secure.getInt(mResolver,
            Settings.Secure.ADVANCED_MODE, 0) == 0;
        if (mDisabled) {
            Settings.Secure.putInt(mResolver,
                Settings.Secure.ADVANCED_MODE, mEnabled ? 0 : 1);
        }
        if (mEnabled) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName("com.android.settings",
                "com.android.settings.Settings$CyanideCentralActivity");
            mQSBar.startSettingsActivity(intent);
        }
    }
}
