package org.horizontal.tella.mobile.views.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageButton;

import org.horizontal.tella.mobile.R;
import org.horizontal.tella.mobile.util.ViewUtil;

public class CameraGridButton extends AppCompatImageButton implements View.OnTouchListener {

    public CameraGridButton(Context context) {
        this(context, null);
    }

    public CameraGridButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraGridButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOnTouchListener(this);
    }

    public void displayGridOn() {
        setImageResource(R.drawable.ic_grid_on_white_24dp);
    }

    public void displayGridOff() {
        setImageResource(R.drawable.ic_grid_off_white_24dp);
    }

    public void rotateView(int angle) { animate().rotation(angle).start(); }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        if (enabled) {
            setAlpha(1f);
        } else {
            setAlpha(0.5f);
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        ViewUtil.animateTouchWithAlpha(view, motionEvent);
        return false;
    }
}