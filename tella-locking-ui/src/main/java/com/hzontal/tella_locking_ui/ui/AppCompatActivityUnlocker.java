package com.hzontal.tella_locking_ui.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import org.hzontal.tella.keys.config.IUnlocker;
import org.hzontal.tella.keys.config.UnlockRegistry;
import org.hzontal.tella.keys.config.UnlockResult;

import java.security.spec.KeySpec;

public class AppCompatActivityUnlocker implements IUnlocker {
    private final UnlockRegistry unlockRegistry;
    private final Class<? extends Activity> activityClass;
    private ActivityResultLauncher<Intent> launcher;

    public AppCompatActivityUnlocker(UnlockRegistry unlockRegistry, Class<? extends Activity> activityClass) {
        this.unlockRegistry = unlockRegistry;
        this.activityClass = activityClass;
    }

    @Override
    public void unlock(Context context, IUnlockerCallback callback) {
        if (!(context instanceof AppCompatActivity)) {
            callback.onError(new IllegalArgumentException());
            return;
        }

        AppCompatActivity activity = (AppCompatActivity) context;

        launcher = activity.getActivityResultRegistry().register(
                AppCompatActivityUnlocker.class.getName(),
                new ActivityResultContracts.StartActivityForResult(),
                result -> handleResult(result, callback)
        );

        launcher.launch(createIntent(activity));
    }

    private void handleResult(ActivityResult activityResult, IUnlockerCallback callback) {
        launcher.unregister();

        UnlockResult unlockResult = unlockRegistry.getResult(activityClass.getName());

        unlockRegistry.unregister(activityClass.getName());

        if (activityResult.getResultCode() == Activity.RESULT_CANCELED) {
            callback.onCancel();
            return;
        }

        KeySpec keySpec = unlockResult.getKeySpec();
        callback.onUnlocked(keySpec);
    }

    private Intent createIntent(Activity activity) {
        Intent intent = new Intent(activity, activityClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        return intent;
    }
}
