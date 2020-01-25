package rs.readahead.washington.mobile.views.custom;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.CountDownTimer;
import androidx.appcompat.widget.AppCompatImageView;
import android.util.AttributeSet;

import rs.readahead.washington.mobile.R;


public class CountdownImageView extends AppCompatImageView {
    private int currentNumber = -1;
    private TypedArray drawables;
    private CountDownTimer timer;

    public interface IFinishHandler {
        void onFinish();
    }


    public CountdownImageView(Context context) {
        super(context);
        init();
    }

    public CountdownImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public CountdownImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setCountdownNumber(int number) {
        if (number == currentNumber) {
            return;
        }
        
        setImageDrawable(drawables.getDrawable(currentNumber = number));
    }

    public void start(int start, final IFinishHandler handler) {
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

        drawables = getResources().obtainTypedArray(R.array.countdown_array);
    }

}
