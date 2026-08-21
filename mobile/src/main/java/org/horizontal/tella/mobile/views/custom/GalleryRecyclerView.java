package org.horizontal.tella.mobile.views.custom;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

import org.horizontal.tella.mobile.MyApplication;
import org.horizontal.tella.mobile.R;
import org.horizontal.tella.mobile.bus.event.GalleryFlingTopEvent;


public class GalleryRecyclerView extends RecyclerView implements
        GestureDetector.OnGestureListener {
    private GestureDetectorCompat detectorCompat;
    private int currentScrollPosition = 0;
    private float shortFling;


    public GalleryRecyclerView(Context context) {
        this(context, null);
    }

    public GalleryRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GalleryRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        detectorCompat = new GestureDetectorCompat(context.getApplicationContext(), this);
        addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                currentScrollPosition += dy;
            }
        });
        shortFling = (float) context.getResources().getInteger(R.integer.ra_config_short_fling);
    }

    @Override
    protected void onDetachedFromWindow() {
        clearOnScrollListeners();
        super.onDetachedFromWindow();
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
        if ((e1 == null || (e2.getY() - e1.getY() > shortFling)) && currentScrollPosition == 0) {
            MyApplication.bus().post(new GalleryFlingTopEvent());
        }

        return false;
    }
}
