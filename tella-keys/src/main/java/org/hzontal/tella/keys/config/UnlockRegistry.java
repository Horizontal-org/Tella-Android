package org.hzontal.tella.keys.config;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;

import org.hzontal.tella.keys.key.MainKey;
import org.hzontal.tella.keys.util.Preferences;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;

public class UnlockRegistry {
    private final Map<Method, UnlockConfig> configs = new HashMap<>();
    private final Map<String, UnlockResult> results = new HashMap<>();

    public void putResult(String name, UnlockResult result) {
        results.put(name, result);
    }

    public UnlockResult getResult(String name) {
        return results.get(name);
    }

    public void unregister(String name) {
        // todo: wipe here?
        results.remove(name);
    }

    public void registerConfig(Method method, UnlockConfig config) {
        configs.put(method, config);
    }

    public Set<Method> getRegisteredMethods() {
        return configs.keySet();
    }

    public Set<Method> getSupportedMethods() {
        return Collections.emptySet();
    }

    // capabilities
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isDeviceCredentialsAvailable() {
        // todo: check why exactly we do not go for 21 (something about changing the pass removes the keys)
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    public UnlockConfig getActiveConfig(Context context) {
        /*
         * Now we go only for device capabilities, to "fancy" options:
         *
         * Starting from 21:
         * KeyguardManager.createConfirmDeviceCredentialIntent - for credentials.
         *   [isDeviceSecure()]
         * FingerprintManager API - for fingertips (biometrics)
         *   [isHardwareDetected() && hasEnrolledFingerprints()]
         *
         * Starting from 28:
         * BiometricManager - for all biometrics
         *   [canAuthenticate()]
         *
         * Right one is dependant on user choice + our choice of best option on it.
         */

        return configs.get(getActiveMethod(context));
    }

    public boolean isDeviceCredentialsEnabled(@NonNull Context context) {
        if (!isDeviceCredentialsAvailable()) {
            return false;
        }

        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);

        return keyguardManager.isDeviceSecure();
    }

    public boolean isBiometricsAvailable(@NonNull Context context) {
        if (!isDeviceCredentialsAvailable()) {
            return false;
        }

        BiometricManager biometricManager = BiometricManager.from(context);
        int result = biometricManager.canAuthenticate();

        return result == BiometricManager.BIOMETRIC_SUCCESS ||
                result == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED;
    }

    public boolean isBiometricsEnabled(@NonNull Context context) {
        if (!isDeviceCredentialsAvailable()) {
            return false;
        }

        BiometricManager biometricManager = BiometricManager.from(context);

        return biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public Method getActiveMethod(Context context) {
        String name = Preferences.load(context, getPreferenceKey(), "");

        return Method.valueOf(name);
    }

    public void setActiveMethod(Context context, Method activeMethod) {
        Preferences.store(context, getPreferenceKey(), activeMethod.name());
    }

    public enum Method {
        DISABLED,
        TELLA_PATTERN,
        TELLA_PIN,
        TELLA_PASSWORD,
        DEVICE_CREDENTIALS, // credentials (ie. password, pin, pattern)
        DEVICE_CREDENTIALS_BIOMETRICS; // credentials with biometrics allowed (fingerprint, face, iris)
    }

    private String getPreferenceKey() {
        return MainKey.class.getName() + "_activeMethod";
    }
}
