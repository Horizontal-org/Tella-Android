package org.hzontal.tella.keys.util;

import java.util.Arrays;

import androidx.annotation.Nullable;

public class Wiper {
    public static void wipe(@Nullable byte[] bytes) {
        if (bytes == null) {
            return;
        }

        Arrays.fill(bytes, (byte) 0);
    }
}
