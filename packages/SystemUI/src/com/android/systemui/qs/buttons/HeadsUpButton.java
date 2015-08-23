/*
 * Copyright (C) 2015 CyanideL && Fusion
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
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.provider.Settings;

import com.android.systemui.R;
import com.android.systemui.qs.QSBar;

public class HeadsUpButton extends QSButton {

    private final ContentObserver mHeadsUpObserver;
    private final ContentResolver mResolver;

    private boolean mEnabled;

    public HeadsUpButton(Context context, QSBar qsBar, Drawable iconEnabled,
            Drawable iconDisabled) {
        super(context, qsBar, iconEnabled, iconDisabled);

        mHeadsUpObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                mEnabled = Settings.System.getInt(mContext.getContentResolver(),
                        Settings.System.HEADS_UP_USER_ENABLED, 0) == 1;
                updateState(mEnabled);
            }
        };
        mResolver = mContext.getContentResolver();
        mEnabled = Settings.System.getInt(mResolver,
                Settings.System.HEADS_UP_USER_ENABLED, 0) == 1;
        updateState(mEnabled);
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            mResolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.HEADS_UP_USER_ENABLED),
                    false, mHeadsUpObserver);
        } else {
            mResolver.unregisterContentObserver(mHeadsUpObserver);
        }
    }

    @Override
    public void handleClick() {
        Settings.System.putInt(mResolver,
                Settings.System.HEADS_UP_USER_ENABLED, mEnabled ? 0 : 1);
    }

    @Override
    public void handleLongClick() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName("com.android.settings",
            "com.android.settings.Settings$HeadsUpSettingsActivity");
        mQSBar.startSettingsActivity(intent);
    }
}
