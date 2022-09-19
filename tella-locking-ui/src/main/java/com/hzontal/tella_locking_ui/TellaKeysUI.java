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


    public static void initialize(MainKeyStore mainKeyStore, LifecycleMainKey mainKeyHolder, UnlockRegistry unlockRegistry, CredentialsCallback credentialsCallback) {
        if (initialized) {
            return;
        }
        mMainKeyStore = mainKeyStore;
        mMainKeyHolder = mainKeyHolder;
        mUnlockRegistry = unlockRegistry;
        mCredentialsCallback = credentialsCallback;
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
}
