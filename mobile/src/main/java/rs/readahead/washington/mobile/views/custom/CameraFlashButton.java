package rs.readahead.washington.mobile.views.custom;

import android.content.Context;
import android.support.v7.widget.AppCompatImageButton;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.util.ViewUtil;


public class CameraFlashButton extends AppCompatImageButton implements View.OnTouchListener {
    public CameraFlashButton(Context context) {
        this(context, null);
    }

    public CameraFlashButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraFlashButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOnTouchListener(this);
    }

    public void displayFlashOn() {
        setImageResource(R.drawable.ic_flash_on_white);
    }

    public void displayFlashOff() {
        setImageResource(R.drawable.ic_flash_off_white);
    }

    public void displayFlashAuto() {
        setImageResource(R.drawable.ic_flash_auto_white);
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
