package org.hzontal.shared_ui.bottomsheet;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import java.util.Objects;

public class KeyboardUtil {

    private final View contentView;
    private final ViewGroup.LayoutParams params;
    private final int initialHeight;
    private boolean isKeyboardOpen = false;

    private static final double KEYBOARD_HEIGHT_THRESHOLD_RATIO = 0.15;

    public KeyboardUtil(final View contentView) {
        this.contentView = Objects.requireNonNull(contentView);
        this.params = contentView.getLayoutParams();
        this.initialHeight = params.height;

        contentView.getViewTreeObserver().addOnPreDrawListener(() -> {
            Rect r = new Rect();
            contentView.getWindowVisibleDisplayFrame(r);

            int screenHeight = contentView.getContext().getResources().getDisplayMetrics().heightPixels;

            int keypadHeight = screenHeight - r.bottom;

            if (keypadHeight > screenHeight * KEYBOARD_HEIGHT_THRESHOLD_RATIO) {
                if (!isKeyboardOpen) {
                    isKeyboardOpen = true;
                    adjustViewForKeyboard(true, 0);
                }
            } else {
                if (isKeyboardOpen) {
                    isKeyboardOpen = false;
                    adjustViewForKeyboard(false, 0);
                }
            }

            return true;
        });
    }

    private void adjustViewForKeyboard(boolean keyboardOpen, int keypadHeight) {
        if (keyboardOpen) {
            params.height = initialHeight + keypadHeight;
        } else {
            params.height = initialHeight;
        }
        contentView.setLayoutParams(params);
    }

    public static void hideKeyboard(Activity activity, View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}