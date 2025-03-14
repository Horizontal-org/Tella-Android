package org.horizontal.tella.mobile.views.custom;

import android.content.Context;
import androidx.appcompat.widget.AppCompatImageButton;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.horizontal.tella.mobile.R;
import org.horizontal.tella.mobile.util.ViewUtil;


public class CameraResolutionButton extends AppCompatImageButton implements View.OnTouchListener {
    public CameraResolutionButton(Context context) {
        this(context, null);
    }

    public CameraResolutionButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraResolutionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOnTouchListener(this);
        setImageResource(R.drawable.ic_tune_white);
    }

    public void rotateView(int angle){
        animate().rotation(angle).start();
    }

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
