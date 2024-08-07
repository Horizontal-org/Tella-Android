/*
 * Copyright (c) 2015 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package com.hzontal.tella_locking_ui.patternlock;

import android.os.Bundle;
import android.view.WindowManager;

import com.hzontal.tella_locking_ui.R;

import org.jetbrains.annotations.NotNull;

import java.util.List;

// For AOSP implementations, see:
// https://android.googlesource.com/platform/packages/apps/Settings/+/master/src/com/android/settings/ConfirmLockPattern.java
// https://android.googlesource.com/platform/frameworks/base/+/43d8451/policy/src/com/android/internal/policy/impl/keyguard/KeyguardPatternView.java
// https://android.googlesource.com/platform/frameworks/base/+/master/packages/Keyguard/src/com/android/keyguard/KeyguardPatternView.java
public class ConfirmPatternActivity extends BasePatternActivity
        implements PatternView.OnPatternListener {

    public static final String KEY_NUM_FAILED_ATTEMPTS = "num_failed_attempts";

    public static final int RESULT_FORGOT_PASSWORD = RESULT_FIRST_USER;

    protected int mNumFailedAttempts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMessageText.setText(R.string.UnlockPattern_DrawToUnlock);
        mPatternView.setInStealthMode(isStealthModeEnabled());
        mPatternView.setOnPatternListener(this);
        //mLeftButton.setText(R.string.pl_cancel);
        mLeftButton.setOnClickListener(v -> onCancel());
       // mRightButton.setText(R.string.pl_forgot_pattern);
        mRightButton.setOnClickListener(v -> onForgotPassword());
        ViewAccessibilityCompat.announceForAccessibility(mMessageText, mMessageText.getText());

        if (savedInstanceState == null) {
            mNumFailedAttempts = 0;
        } else {
            mNumFailedAttempts = savedInstanceState.getInt(KEY_NUM_FAILED_ATTEMPTS);
        }
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_NUM_FAILED_ATTEMPTS, mNumFailedAttempts);
    }

    @Override
    public void onPatternStart() {

        removeClearPatternRunnable();

        // Set display mode to correct to ensure that pattern can be in stealth mode.
        mPatternView.setDisplayMode(PatternView.DisplayMode.Correct);
    }

    @Override
    public void onPatternCellAdded(List<PatternView.Cell> pattern) {}

    @Override
    public void onPatternDetected(List<PatternView.Cell> pattern) {
        if (isPatternCorrect(pattern)) {
            onConfirmed();
        } else {
            mMessageText.setText(R.string.LockPatternConfirm_Message_WrongPattern);
            mPatternView.setDisplayMode(PatternView.DisplayMode.Wrong);
            postClearPatternRunnable();
            ViewAccessibilityCompat.announceForAccessibility(mMessageText, mMessageText.getText());
            onWrongPattern();
        }
    }

    @Override
    public void onPatternCleared() {
        removeClearPatternRunnable();
    }

    protected boolean isStealthModeEnabled() {
        return false;
    }

    protected boolean isPatternCorrect(List<PatternView.Cell> pattern) {
        return true;
    }

    protected void onConfirmed() {
        setResult(RESULT_OK);
        finish();
        overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
    }

    protected void onWrongPattern() {
        ++mNumFailedAttempts;
    }

    protected void onCancel() {
        setResult(RESULT_CANCELED);
        finish();
        overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
    }

    protected void onForgotPassword() {
        setResult(RESULT_FORGOT_PASSWORD);
        finish();
        overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
    }
}
