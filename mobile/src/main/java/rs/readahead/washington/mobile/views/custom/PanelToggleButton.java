package rs.readahead.washington.mobile.views.custom;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatButton;

import rs.readahead.washington.mobile.R;

public class PanelToggleButton extends AppCompatButton {
    private boolean open = false;
    private OnStateChangedListener listener;

    public interface OnStateChangedListener {
        void stateChanged(boolean open);
    }

    public PanelToggleButton(Context context) {
        super(context);
        initialize();
    }

    public PanelToggleButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public PanelToggleButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    public void setOnStateChangedListener(OnStateChangedListener listener) {
        this.listener = listener;
    }

    public boolean isOpen() {
        return open;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.listener = null;
    }

    private void initialize() {
        setOnClickListener(v -> toggleState());
        updateState();
    }

    private void toggleState() {
        open = !open;

        updateState();

        if (listener != null) {
            listener.stateChanged(open);
        }
    }

    public void setOpen() {
        open = true;

        updateState();

        if (listener != null) {
            listener.stateChanged(open);
        }
    }

    private void updateState() {
        setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                open ? R.drawable.ic_arrow_drop_up_24 : R.drawable.ic_arrow_drop_down_24,
                0);
    }
}
