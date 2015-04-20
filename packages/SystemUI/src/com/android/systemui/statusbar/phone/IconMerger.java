/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.systemui.statusbar.phone;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.PorterDuff.Mode;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.Clock;

public class IconMerger extends LinearLayout {
    private static final String TAG = "IconMerger";
    private static final boolean DEBUG = false;

    private int mIconWidth;
    private int mClockAndDateWidth;
    private boolean mCenterClock;
    protected View mMoreView;

    public IconMerger(Context context, AttributeSet attrs) {
        super(context, attrs);

        mIconWidth = calculateIconWidth(context);

        if (DEBUG) {
            setBackgroundColor(0x800099FF);
        }
    }

    /**
     * Considering the padding, this method calculates the effective icon width
     * of the notification icons.
     *
     * @param context
     * @return The effective icon width which is expected by the {@link IconMerger}.
     */
    public static int calculateIconWidth(final Context context) {
        int iconSize = context.getResources().getDimensionPixelSize(
                R.dimen.status_bar_icon_size);
        int iconHPadding = context.getResources().getDimensionPixelSize(
                R.dimen.status_bar_icon_padding);
        return iconSize + 2 * iconHPadding;
    }

    public void setOverflowIndicator(View v) {
        mMoreView = v;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // we need to constrain this to an integral multiple of our children
        int width = getMeasuredWidth();
        if (mCenterClock) {
            final int totalWidth = mContext.getResources().getDisplayMetrics().widthPixels;
            final int usableWidth = (totalWidth - mClockAndDateWidth - 2 * mIconWidth) / 2;
            if (width > usableWidth) {
                width = usableWidth;
            }
        }
        setMeasuredDimension(width - (width % mIconWidth), getMeasuredHeight());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        checkOverflow(r - l);
    }

    private void checkOverflow(int width) {
        if (mMoreView == null) return;

        final int N = getChildCount();
        int visibleChildren = 0;
        for (int i=0; i<N; i++) {
            if (getChildAt(i).getVisibility() != GONE) visibleChildren++;
        }
        final boolean overflowShown = (mMoreView.getVisibility() == View.VISIBLE);
        // let's assume we have one more slot if the more icon is already showing
        if (!mCenterClock && overflowShown) visibleChildren --;
        final boolean moreRequired = visibleChildren * mIconWidth > width;
        if (moreRequired != overflowShown) {
            post(new Runnable() {
                @Override
                public void run() {
                    mMoreView.setVisibility(moreRequired ? View.VISIBLE : View.GONE);
                }
            });
        }
    }

    public void setClockAndDateStatus(int width, int mode, boolean enabled) {
        mClockAndDateWidth = width;
        mCenterClock = mode == Clock.STYLE_CLOCK_CENTER && enabled;
    }

    public void setMoreIconColor() {
        ContentResolver resolver = mContext.getContentResolver();

        // The more icon is always a greyscale icon,
        // so only check if the icon should be colorized at all
        boolean colorizeIcon = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_NOTIF_SYSTEM_ICONS_COLOR_MODE, 1) != 0;
        int iconColor = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_NOTIF_SYSTEM_ICON_COLOR,
                0xffffffff);

        if (colorizeIcon) {
            ((ImageView) mMoreView).setColorFilter(iconColor, Mode.MULTIPLY);
        } else {
            ((ImageView) mMoreView).setColorFilter(null);
        }
    }
}
