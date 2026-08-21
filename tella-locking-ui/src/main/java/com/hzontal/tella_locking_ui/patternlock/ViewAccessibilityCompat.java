/*
 * Copyright (c) 2015 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package com.hzontal.tella_locking_ui.patternlock;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

class ViewAccessibilityCompat {

    private ViewAccessibilityCompat() {}

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static void announceForAccessibility(View view, CharSequence announcement) {
        view.announceForAccessibility(announcement);
    }
}
