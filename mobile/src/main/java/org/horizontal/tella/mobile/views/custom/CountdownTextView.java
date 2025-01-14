package org.horizontal.tella.mobile.views.custom;

import android.content.Context;
import android.os.CountDownTimer;
import android.util.AttributeSet;

import org.horizontal.tella.mobile.R;

public class CountdownTextView extends androidx.appcompat.widget.AppCompatTextView {

    private int currentNumber = -1;
    private CountDownTimer timer;

    public CountdownTextView(Context context) {
        super(context);
        init();
    }


    public CountdownTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public CountdownTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setCountdownNumber(int number) {
        if (number == currentNumber) {
            return;
        }
        currentNumber = number;
        setText(new StringBuilder().append("").append(currentNumber).toString());
    }

    public void start(int start, final CountdownImageView.IFinishHandler handler) {
        cancel();
        setCountdownNumber(start);

        timer = new CountDownTimer(start * 1000L, 200L) {
            public void onTick(long millisUntilFinished) {
                setCountdownNumber(Math.round(millisUntilFinished * 0.001f));
            }

            public void onFinish() {
                handler.onFinish();
            }
        }.start();
    }

    public void cancel() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public boolean isCounting() {
        return timer != null;
    }

    protected void init() {
        if (isInEditMode()) return;
    }

    public interface IFinishHandler {
        void onFinish();
    }

}

