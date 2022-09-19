package org.hzontal.tella.keys.config;

import android.content.Context;

import javax.crypto.spec.PBEKeySpec;

public class UnencryptedUnlocker implements IUnlocker {
    private static final String UNENCRYPTED_PASSPHRASE = "unencrypted";

    @Override
    public void unlock(Context context, IUnlockerCallback callback) {
        callback.onUnlocked(new PBEKeySpec(UNENCRYPTED_PASSPHRASE.toCharArray()));
    }
}
