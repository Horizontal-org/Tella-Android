package rs.readahead.washington.mobile.views.custom;

import android.content.Context;
import androidx.appcompat.widget.AppCompatImageButton;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.otaliastudios.cameraview.controls.Facing;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.util.ViewUtil;


public class CameraSwitchButton extends AppCompatImageButton implements View.OnTouchListener {
    public CameraSwitchButton(Context context) {
        this(context, null);
    }

    public CameraSwitchButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraSwitchButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOnTouchListener(this);
    }

    public void displayCamera(Facing facing) {
        if (facing == Facing.BACK) {
            setImageResource(R.drawable.ic_camera_rear_white);
        } else {
            setImageResource(R.drawable.ic_camera_front_white);
        }
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
