package com.hzontal.tella_locking_ui.ui;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import org.hzontal.shared_ui.utils.DialogUtils;
import org.hzontal.tella.keys.config.IUnlockRegistryHolder;
import org.hzontal.tella.keys.config.UnlockRegistry;
import org.hzontal.tella.keys.config.UnlockResult;

import timber.log.Timber;

public class DeviceCredentialsUnlockActivity extends AppCompatActivity {
    private static final int SYSTEM_UNLOCK_CODE = 1000;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        showSystemPrompt();
    }

    @Override
    @SuppressLint("MissingSuperCall")
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (requestCode) {
            case SYSTEM_UNLOCK_CODE:
                if (resultCode == RESULT_OK) {
                    handleSuccessfulUnlock();
                }

                break;
        }

        handleUnsuccessfulUnlock();
    }

    @SuppressLint("NewApi")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private boolean showSystemPrompt() {
        /*
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
         */
        return maybeShowBiometricPrompt() ||
                maybeShowCredentialsPrompt();
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private boolean maybeShowBiometricPrompt() {
        BiometricManager biometricManager = BiometricManager.from(this);

        switch (biometricManager.canAuthenticate()) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                Timber.d("App can authenticate using biometrics.");

                BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Title")
                        .setSubtitle("Subtitle")
                        .setNegativeButtonText("Negative button text")
                        .build();

                BiometricPrompt biometricPrompt = new BiometricPrompt(
                        this,
                        ContextCompat.getMainExecutor(this),
                        getAuthenticationCallback());

                biometricPrompt.authenticate(promptInfo);

                return true;

            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Timber.e("No biometric features available on this device.");
                return false;

            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Timber.e("Biometric features are currently unavailable.");
                return false;

            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Timber.e("The user hasn't associated any biometric credentials with their account.");
                return false;
        }

        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private boolean maybeShowCredentialsPrompt() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        Intent intent = keyguardManager.createConfirmDeviceCredentialIntent("Unlock Tella2", "");
        startActivityForResult(intent, SYSTEM_UNLOCK_CODE);

        return true;
    }

    private CancellationSignal getCancellationSignal() {
        CancellationSignal cancellationSignal = new CancellationSignal();
        cancellationSignal.setOnCancelListener(() -> Timber.d("Cancelled via signal"));
        return cancellationSignal;
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private BiometricPrompt.AuthenticationCallback getAuthenticationCallback() {
        return new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                DialogUtils.showBottomMessage(
                        DeviceCredentialsUnlockActivity.this,
                        "Authentication error: " + errString,
                        true
                );
                handleUnsuccessfulUnlock();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                DialogUtils.showBottomMessage(
                        DeviceCredentialsUnlockActivity.this,
                        "Authentication succeeded!",
                        false
                );
                handleSuccessfulUnlock();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                DialogUtils.showBottomMessage(
                        DeviceCredentialsUnlockActivity.this,
                        "Authentication failed",
                        true
                );
                handleUnsuccessfulUnlock();
            }
        };
    }

    private void handleSuccessfulUnlock() {
        IUnlockRegistryHolder holder = (IUnlockRegistryHolder) getApplicationContext();
        UnlockRegistry registry = holder.getUnlockRegistry();

        registry.putResult(DeviceCredentialsUnlockActivity.class.getName(),
                new UnlockResult(null));

        setResult(RESULT_OK);
        finish();
    }

    private void handleUnsuccessfulUnlock() {
        setResult(RESULT_CANCELED);
        finish();
    }
}
