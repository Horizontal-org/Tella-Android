package org.hzontal.tella.keys;

import org.hzontal.tella.keys.util.PRNGFixes;

public final class TellaKeys {
    private static boolean initialized = false;

    public static void initialize() {
        if (initialized) {
            return;
        }

        PRNGFixes.apply();

        initialized = true;
    }
}
