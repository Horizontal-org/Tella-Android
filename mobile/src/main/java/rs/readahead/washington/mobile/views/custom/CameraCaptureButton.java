package rs.readahead.washington.mobile.views.custom;

import android.content.Context;
import androidx.appcompat.widget.AppCompatImageButton;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.util.ViewUtil;


public class
CameraCaptureButton extends AppCompatImageButton implements View.OnTouchListener{
    public CameraCaptureButton(Context context) {
        this(context, null);
    }

    public CameraCaptureButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraCaptureButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOnTouchListener(this);
    }

    public void displayPhotoButton() {
        setBackground(getContext().getResources().getDrawable(R.drawable.capture_button_background));
        setImageResource(android.R.color.transparent);
    }

    public void displayVideoButton() {
        setBackground(getContext().getResources().getDrawable(R.drawable.capture_button_stop));
        setImageResource(android.R.color.transparent);
    }

    public void displayStopVideo() {
        setBackground(getContext().getResources().getDrawable(R.drawable.button_oval_black_background));
        setImageResource(R.drawable.ic_stop_red);
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
