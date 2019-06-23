package rs.readahead.washington.mobile.views.custom;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.otaliastudios.cameraview.CameraView;

import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.event.CameraFlingUpEvent;


public class WaCameraView extends CameraView implements
        GestureDetector.OnGestureListener {
    private GestureDetectorCompat detectorCompat;
    private float shortFling;

    public WaCameraView(Context context) {
        super(context);
    }

    public WaCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        detectorCompat = new GestureDetectorCompat(context.getApplicationContext(), this);
        shortFling = (float) context.getResources().getInteger(R.integer.ra_config_short_fling);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return detectorCompat.onTouchEvent(event) || super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (e1.getY() - e2.getY() > shortFling) { // not too short fling..
            MyApplication.bus().post(new CameraFlingUpEvent());
        }
        return true;
    }
}
