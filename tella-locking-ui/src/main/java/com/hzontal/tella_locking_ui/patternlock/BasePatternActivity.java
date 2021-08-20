/*
 * Copyright (c) 2015 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package com.hzontal.tella_locking_ui.patternlock;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.hzontal.tella_locking_ui.common.BaseActivity;
import com.hzontal.tella_locking_ui.R;

public class BasePatternActivity extends BaseActivity {

    private static final int CLEAR_PATTERN_DELAY_MILLI = 2000;

    protected TextView mMessageText;
    protected PatternView mPatternView;
    protected TextView mLeftButton;
    protected TextView mRightButton;
    protected ImageView mTopImageView;
    protected ConstraintLayout root;

    private final Runnable clearPatternRunnable = new Runnable() {
        public void run() {
            // clearPattern() resets display mode to DisplayMode.Correct.
            mPatternView.clearPattern();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.pl_base_pattern_activity);
        mMessageText = findViewById(R.id.pl_message_text);
        mPatternView = findViewById(R.id.pl_pattern);
        mLeftButton = findViewById(R.id.pl_left_button);
        mRightButton = findViewById(R.id.pl_right_button);
        mTopImageView = findViewById(R.id.pl_patternImg);
        root = findViewById(R.id.rootPattern);
        root.getRootView().setBackgroundColor(ContextCompat.getColor(this, R.color.space_cadet));
        mLeftButton.setText(isFromSettings() ? getString(R.string.pl_cancel) : getString(R.string.pl_back));
    }


    protected void removeClearPatternRunnable() {
        mPatternView.removeCallbacks(clearPatternRunnable);
    }

    protected void postClearPatternRunnable() {
        removeClearPatternRunnable();
        mPatternView.postDelayed(clearPatternRunnable, CLEAR_PATTERN_DELAY_MILLI);
    }
}
