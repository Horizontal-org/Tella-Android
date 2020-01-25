package rs.readahead.washington.mobile.views.custom;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import rs.readahead.washington.mobile.R;


public class FormSubmitButtonView extends FrameLayout {
    public FormSubmitButtonView(@NonNull Context context) {
        this(context, null);
    }

    public FormSubmitButtonView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FormSubmitButtonView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView();
    }

    private void inflateView() {
        inflate(getContext(), R.layout.form_submit_end_button, this);
    }

    public void setOffline(boolean offline) {
        TextView textView = findViewById(R.id.submit_button_text);
        ImageView imageView = findViewById(R.id.submit_button_image);

        textView.setText(offline ? R.string.ra_save_form : R.string.ra_submit);
        imageView.setImageResource(offline ? R.drawable.ic_watch_later_black_24dp : R.drawable.ic_send_black_24dp);
    }
}
