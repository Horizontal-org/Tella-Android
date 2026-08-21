package com.hzontal.tella_locking_ui;

import android.annotation.SuppressLint;

import com.hzontal.tella_locking_ui.common.CredentialsCallback;

import org.hzontal.tella.keys.MainKeyStore;
import org.hzontal.tella.keys.config.UnlockRegistry;
import org.hzontal.tella.keys.key.LifecycleMainKey;

public class TellaKeysUI {

    private static boolean initialized = false;
    @SuppressLint("StaticFieldLeak")
    private static MainKeyStore mMainKeyStore;
    private static LifecycleMainKey mMainKeyHolder;
    private static UnlockRegistry mUnlockRegistry;
    private static CredentialsCallback mCredentialsCallback;

    private static long mNumFailedAttempts;
    private static long mRemainingAttempts;

    private static boolean misShowRemainingAttempts;

    private TellaKeysUI() {
        // Private constructor to prevent instantiation
    }

    public static void initialize(MainKeyStore mainKeyStore, LifecycleMainKey mainKeyHolder,
                                  UnlockRegistry unlockRegistry, CredentialsCallback credentialsCallback,
                                  long numFailedAttempts,long remainingAttempts, boolean isShowRemainingAttempts) {
        if (initialized) {
            return;
        }
        mMainKeyStore = mainKeyStore;
        mMainKeyHolder = mainKeyHolder;
        mUnlockRegistry = unlockRegistry;
        mCredentialsCallback = credentialsCallback;
        mNumFailedAttempts = numFailedAttempts;
        mRemainingAttempts = remainingAttempts;
        misShowRemainingAttempts = isShowRemainingAttempts;
        initialized = true;
    }

    public static LifecycleMainKey getMainKeyHolder() {
        return mMainKeyHolder;
    }

    public static UnlockRegistry getUnlockRegistry() {
        return mUnlockRegistry;
    }

    public static MainKeyStore getMainKeyStore() {
        return mMainKeyStore;
    }

    public static CredentialsCallback getCredentialsCallback() {
        return mCredentialsCallback;
    }

    public static long getNumFailedAttempts() {
        return mNumFailedAttempts;
    }

    public static long setNumFailedAttempts(long numFailedAttempts) {
        return mNumFailedAttempts = numFailedAttempts;
    }

    public static void setRemainingAttempts(long remainingAttempts) {
        mRemainingAttempts = remainingAttempts;
    }

    public static long getRemainingAttempts() {
        return mRemainingAttempts;
    }

    public static boolean isShowRemainingAttempts() {
        return misShowRemainingAttempts;
    }

    public static void setIsShowRemainingAttempts(boolean isShowRemainingAttempts) {
        TellaKeysUI.misShowRemainingAttempts = isShowRemainingAttempts;
    }
}
