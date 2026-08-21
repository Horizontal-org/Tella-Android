package com.hzontal.tella_locking_ui.common;

import android.app.Activity;
import android.content.Context;

public interface CredentialsCallback {
    void onSuccessfulUnlock(Context context);

    void onUnSuccessfulUnlock(String tag,Throwable throwable);

    void onLockConfirmed(Context context);

    void onUpdateUnlocking();

    void onLockUpdateSuccess(android.content.Context context);

    void onFailedAttempts(long num);

    void saveRemainingAttempts(long num);

    /**
     * Main key was cleared (e.g. lock timeout) while confirming a new lock — treat as full app lock.
     * No Settings / change-lock flow; user unlocks like a cold resume.
     */
    default void launchFullAppUnlock(Activity activity) {
    }
}