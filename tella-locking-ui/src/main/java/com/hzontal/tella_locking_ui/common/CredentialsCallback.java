package com.hzontal.tella_locking_ui.common;

import android.content.Context;

public interface CredentialsCallback {
    void onSuccessfulUnlock(Context context);

    void onUnSuccessfulUnlock();

    void onLockConfirmed(Context context);
}