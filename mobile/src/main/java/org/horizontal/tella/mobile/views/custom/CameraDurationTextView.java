package org.horizontal.tella.mobile.views.custom;

import android.content.Context;
import androidx.appcompat.widget.AppCompatTextView;
import android.util.AttributeSet;

import java.util.Timer;
import java.util.TimerTask;

import org.horizontal.tella.mobile.util.Util;
import org.horizontal.tella.mobile.util.ThreadUtil;


public class CameraDurationTextView extends AppCompatTextView {
    private long start;
    private Timer timer;

    public CameraDurationTextView(Context context) {
        this(context, null);
    }

    public CameraDurationTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraDurationTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
    }

    public void rotateView(int angle){
        animate().rotation(angle).start();
    }

    public void start() {
        setVisibility(VISIBLE);

        start = Util.currentTimestamp();

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                final int duration = (int) ((Util.currentTimestamp() - start) / 1000);
                ThreadUtil.runOnMain(new Runnable() {
                    @Override
                    public void run() {
                        setText(Util.getVideoDuration(duration));
                    }
                });

            }
        },0,1000);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelTimer();
    }

    public void stop() {
        setVisibility(GONE);
        cancelTimer();
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
