package com.hzontal.tella_locking_ui.common;

import android.content.Context;

public interface CredentialsCallback {
    void onSuccessfulUnlock(Context context);

    void onUnSuccessfulUnlock(String tag,Throwable throwable);

    void onLockConfirmed(Context context);

    void onUpdateUnlocking();

    void onLockUpdateSuccess(android.content.Context context);

    void onFailedAttempts(long num);

    void saveRemainingAttempts(long num);
}