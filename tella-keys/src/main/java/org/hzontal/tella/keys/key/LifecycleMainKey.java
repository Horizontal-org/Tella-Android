package org.hzontal.tella.keys.key;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.os.SystemClock;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import timber.log.Timber;

/**
 * Service that can cache MainKey with TTL that gets activated
 * when lifecycle is in ON_PAUSE state and reset on ON_RESUME.
 */
public class LifecycleMainKey implements LifecycleObserver {
    public static final long NO_TIMEOUT = -1;
    private static final long IMMEDIATE_GRACE_MS = 700L;

    private MainKey mainKey;
    private CurrentState state;
    private long timeout;
    private long pausedAtElapsed;

    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> scheduledFuture;
    private Runnable onCleared;

    /**
     * Runs after the in-memory key is wiped, including lock timeout.
     */
    public void setOnCleared(Runnable onCleared) {
        this.onCleared = onCleared;
    }

    public LifecycleMainKey(Lifecycle lifecycle, long timeout) {
        this.timeout = timeout;
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.state = new CurrentState(State.UNKNOWN);

        lifecycle.addObserver(this);
    }

    public synchronized boolean exists() {
        maybeClearInactiveMainKey();
        return mainKey != null;
    }

    public synchronized MainKey get() throws MainKeyUnavailableException {
        maybeClearInactiveMainKey();

        if (mainKey == null) {
            throw new MainKeyUnavailableException();
        }

        return mainKey;
    }

    public synchronized void set(MainKey mainKey) {
        if (maybeClearInactiveMainKey()) {
            return;
        }

        this.mainKey = mainKey;
    }

    public synchronized boolean clear() {
        Timber.d("*** LifecycleMainKey.clear");

        if (mainKey == null) {
            return false;
        }

        mainKey.wipe();
        mainKey = null;
        if (onCleared != null) {
            onCleared.run();
        }

        return true;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public synchronized void activate() {
        Timber.d("*** LifecycleMainKey.activate");

        cancelScheduledClear();
        maybeClearInactiveMainKey();

        markForeground();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public synchronized void deactivate() {
        Timber.d("*** LifecycleMainKey.deactivate");

        noteAppPaused();
        state = new CurrentState(State.INACTIVE);
        scheduleClear();
    }

    public synchronized void noteAppPaused() {
        if (pausedAtElapsed == 0L) {
            pausedAtElapsed = SystemClock.elapsedRealtime();
            Timber.d("*** LifecycleMainKey.pause timeout=%d", timeout);
        }
    }

    public synchronized void noteAppResumed() {
        maybeClearInactiveMainKey();
        cancelScheduledClear();
        markForeground();
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        if (timeout < 0 && timeout != NO_TIMEOUT) {
            throw new IllegalArgumentException();
        }

        this.timeout = timeout;
    }

    public static class MainKeyUnavailableException extends Exception {
    }

    private void markForeground() {
        pausedAtElapsed = 0L;
        state = new CurrentState(State.ACTIVE);
    }

    private synchronized boolean maybeClearInactiveMainKey() {
        if (timeout == NO_TIMEOUT) {
            return false;
        }

        long elapsed = inactiveElapsed();
        if (elapsed < 0L) {
            return false;
        }

        long limit = timeout == 0L ? IMMEDIATE_GRACE_MS : timeout;
        if (state.getState() == State.INACTIVE && timeout == 0L) {
            limit = 0L;
        }
        if (elapsed >= limit) {
            Timber.d("*** LifecycleMainKey.expired elapsed=%d limit=%d", elapsed, limit);
            return clear();
        }

        return false;
    }

    private long inactiveElapsed() {
        long now = SystemClock.elapsedRealtime();
        long elapsed = -1L;
        if (pausedAtElapsed != 0L) {
            elapsed = now - pausedAtElapsed;
        }
        if (state.getState() == State.INACTIVE) {
            elapsed = Math.max(elapsed, now - state.startedAtElapsed);
        }
        return elapsed;
    }

    @SuppressWarnings("UnusedReturnValue")
    private boolean scheduleClear() {
        if (timeout == NO_TIMEOUT) {
            return false;
        }

        if (timeout == 0) {
            return clear();
        }

        cancelScheduledClear();
        long delay = timeout;
        if (pausedAtElapsed != 0L) {
            delay = timeout - (SystemClock.elapsedRealtime() - pausedAtElapsed);
            if (delay <= 0L) {
                return clear();
            }
        }

        scheduledFuture = executor.schedule(this::onScheduledClear, delay, TimeUnit.MILLISECONDS);

        return true;
    }

    private void onScheduledClear() {
        synchronized (this) {
            if (pausedAtElapsed == 0L && state.getState() != State.INACTIVE) {
                return;
            }
            maybeClearInactiveMainKey();
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    private boolean cancelScheduledClear() {
        boolean cancelled = false;

        if (scheduledFuture != null) {
            cancelled = scheduledFuture.cancel(false); // todo: leak?
            Timber.d("*** LifecycleMainKey.activate scheduledFuture.cancel = %s", cancelled);
            //executor.shutdown();
            scheduledFuture = null;
        }

        return cancelled;
    }

    private enum State {
        UNKNOWN,
        ACTIVE,
        INACTIVE,
    }

    private static class CurrentState {
        private final State state;
        private final long startedAtElapsed;

        public CurrentState(State state) {
            this.state = state;
            this.startedAtElapsed = SystemClock.elapsedRealtime();
        }

        public State getState() {
            return state;
        }
    }
}
