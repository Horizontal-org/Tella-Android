package org.horizontal.tella.mobile.media;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

final class VerificationShareCacheCleaner implements Application.ActivityLifecycleCallbacks {
    private static final long CLEAR_DELAY_MS = 1000L;
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    @Nullable
    private static VerificationShareCacheCleaner active;

    private final Application application;
    private final Class<? extends Activity> watchedClass;
    private WeakReference<Activity> watched;
    private final Uri zipUri;
    private boolean leftForSheet;
    private boolean awaitingReplacement;
    private boolean finished;
    private final Runnable clear = this::clearNow;

    static VerificationShareCacheCleaner arm(Activity activity, Uri zipUri) {
        discardActive();
        VerificationShareCacheCleaner cleaner = new VerificationShareCacheCleaner(activity, zipUri);
        active = cleaner;
        activity.getApplication().registerActivityLifecycleCallbacks(cleaner);
        return cleaner;
    }

    static void discardActive() {
        if (active != null) {
            active.discard();
        }
    }

    private VerificationShareCacheCleaner(Activity activity, Uri zipUri) {
        application = activity.getApplication();
        watchedClass = activity.getClass();
        watched = new WeakReference<>(activity);
        this.zipUri = zipUri;
    }

    private boolean isNotWatched(@NonNull Activity activity) {
        return activity != watched.get();
    }

    void clearNow() {
        if (finished) {
            return;
        }
        finished = true;
        MAIN_HANDLER.removeCallbacks(clear);
        application.unregisterActivityLifecycleCallbacks(this);
        if (active == this) {
            active = null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            application.revokeUriPermission(
                    MediaFileHandler.SIGNAL_PACKAGE,
                    zipUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } else {
            application.revokeUriPermission(zipUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        MediaFileHandler.deleteShareCache(application);
        watched.clear();
    }

    void discard() {
        if (finished) {
            return;
        }
        finished = true;
        MAIN_HANDLER.removeCallbacks(clear);
        application.unregisterActivityLifecycleCallbacks(this);
        if (active == this) {
            active = null;
        }
        watched.clear();
    }

    @Override
    public void onActivityCreated(@NonNull Activity created, @Nullable Bundle savedInstanceState) {
        if (awaitingReplacement && created.getClass() == watchedClass) {
            watched = new WeakReference<>(created);
            awaitingReplacement = false;
        }
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
    }

    @Override
    public void onActivityResumed(@NonNull Activity resumed) {
        if (isNotWatched(resumed) || !leftForSheet || finished) {
            return;
        }
        MAIN_HANDLER.removeCallbacks(clear);
        MAIN_HANDLER.postDelayed(clear, CLEAR_DELAY_MS);
    }

    @Override
    public void onActivityPaused(@NonNull Activity paused) {
        if (isNotWatched(paused) || finished) {
            return;
        }
        leftForSheet = true;
        MAIN_HANDLER.removeCallbacks(clear);
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity destroyed) {
        if (isNotWatched(destroyed) || finished) {
            return;
        }
        if (destroyed.isChangingConfigurations()) {
            awaitingReplacement = true;
            return;
        }
        clearNow();
    }
}
