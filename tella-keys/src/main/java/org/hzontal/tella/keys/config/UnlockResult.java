package org.hzontal.tella.keys.config;

import java.security.spec.KeySpec;

public class UnlockResult {
    private final KeySpec keySpec;

    public UnlockResult(KeySpec keySpec) {
        this.keySpec = keySpec;
    }

    public KeySpec getKeySpec() {
        return keySpec;
    }
}
