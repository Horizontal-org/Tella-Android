package org.hzontal.tella.keys.key;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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

    private MainKey mainKey;
    private CurrentState state;
    private long timeout;

    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> scheduledFuture;

    public LifecycleMainKey(Lifecycle lifecycle, long timeout) {
        this.timeout = timeout;
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.state = new CurrentState(State.UNKNOWN);

        lifecycle.addObserver(this);
    }

    public synchronized boolean exists() {
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

        return true;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public synchronized void activate() {
        Timber.d("*** LifecycleMainKey.activate");

        cancelScheduledClear();
        maybeClearInactiveMainKey();

        state = new CurrentState(State.ACTIVE);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public synchronized void deactivate() {
        Timber.d("*** LifecycleMainKey.deactivate");

        scheduleClear();

        state = new CurrentState(State.INACTIVE);
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

    private synchronized boolean maybeClearInactiveMainKey() {
        if (state.getState() != State.INACTIVE || timeout == NO_TIMEOUT) {
            return false;
        }

        if (state.isExpired(timeout)) {
            return clear();
        }

        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    private boolean scheduleClear() {
        if (timeout == NO_TIMEOUT) {
            return false;
        }

        if (timeout == 0) {
            return clear();
        }

        scheduledFuture = executor.schedule(this::clear, timeout, TimeUnit.MILLISECONDS);

        return true;
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
        private final long start;

        public CurrentState(State state) {
            this.state = state;
            this.start = System.currentTimeMillis();
        }

        public State getState() {
            return state;
        }

        public boolean isExpired(long timeout) {
            return timeout != NO_TIMEOUT && System.currentTimeMillis() - start > timeout;
        }
    }
}
