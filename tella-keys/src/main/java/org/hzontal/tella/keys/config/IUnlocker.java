package org.hzontal.tella.keys.config;

import android.content.Context;

import java.security.spec.KeySpec;

public interface IUnlocker {
    void unlock(Context context, IUnlockerCallback callback);

    interface IUnlockerCallback {
        void onUnlocked(KeySpec keySpec);
        void onError(Throwable throwable);
        void onCancel();
    }
}
