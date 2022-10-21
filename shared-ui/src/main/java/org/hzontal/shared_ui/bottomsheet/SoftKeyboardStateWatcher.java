package org.hzontal.shared_ui.bottomsheet;

import android.graphics.Rect;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;

import java.util.LinkedList;
import java.util.List;

public class SoftKeyboardStateWatcher implements ViewTreeObserver.OnGlobalLayoutListener {

    private static float KEYBOARD_HEIGHT = 100;
    private final List<SoftKeyboardStateListener> listeners = new LinkedList<>();
    private final View activityRootView;
    private int lastSoftKeyboardHeightInPx;
    private boolean isSoftKeyboardOpened;

    public SoftKeyboardStateWatcher(View activityRootView) {
        this(activityRootView, false);
    }

    public SoftKeyboardStateWatcher(View activityRootView, boolean isSoftKeyboardOpened) {
        this.activityRootView = activityRootView;
        if (activityRootView != null)
            KEYBOARD_HEIGHT = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, activityRootView.getResources().getDisplayMetrics());
        this.isSoftKeyboardOpened = isSoftKeyboardOpened;
        assert activityRootView != null;
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Deprecated
    @Override
    public void onGlobalLayout() {
        final Rect r = new Rect();
        //r will be populated with the coordinates of your view that area still visible.
        activityRootView.getWindowVisibleDisplayFrame(r);

        final int height = activityRootView.getRootView().getHeight();
        final int heightDiff = height - (r.bottom);
        final int visibleArea = r.bottom;
        if (!isSoftKeyboardOpened && heightDiff > KEYBOARD_HEIGHT) { // if more than 100 pixels, its probably a keyboard...
            isSoftKeyboardOpened = true;
            notifyOnSoftKeyboardOpened(heightDiff, height, visibleArea);
        } else if (isSoftKeyboardOpened && heightDiff < KEYBOARD_HEIGHT) {
            isSoftKeyboardOpened = false;
            notifyOnSoftKeyboardClosed();
        }
    }

    public boolean isSoftKeyboardOpened() {
        return isSoftKeyboardOpened;
    }

    /**
     * Default value is zero {@code 0}.
     *
     * @return last saved keyboard height in px
     */
    public int getLastSoftKeyboardHeightInPx() {
        return lastSoftKeyboardHeightInPx;
    }

    public void addSoftKeyboardStateListener(SoftKeyboardStateListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    public void removeSoftKeyboardStateListener(SoftKeyboardStateListener listener) {
        listeners.remove(listener);
    }

    private void notifyOnSoftKeyboardOpened(int keyboardHeightInPx, int screenSize, int visibleScreenArea) {
        this.lastSoftKeyboardHeightInPx = keyboardHeightInPx;

        for (SoftKeyboardStateListener listener : listeners) {
            if (listener != null) {
                listener.onSoftKeyboardOpened(keyboardHeightInPx, screenSize, visibleScreenArea);
            }
        }
    }

    private void notifyOnSoftKeyboardClosed() {
        for (SoftKeyboardStateListener listener : listeners) {
            if (listener != null) {
                listener.onSoftKeyboardClosed(lastSoftKeyboardHeightInPx);
            }
        }
    }

    public interface SoftKeyboardStateListener {
        void onSoftKeyboardOpened(int keyboardHeightInPx, int screenSize, int visibleScreenArea);

        void onSoftKeyboardClosed(int keyboardHeightInPx);
    }
}
