package rs.readahead.washington.mobile.views.custom;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import rs.readahead.washington.mobile.R;


public class FileSendButtonView extends FrameLayout {
    public FileSendButtonView(@NonNull Context context) {
        this(context, null);
    }

    public FileSendButtonView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FileSendButtonView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView();
    }

    private void inflateView() {
        inflate(getContext(), R.layout.file_send_button, this);
        this.setText(R.string.send);
    }

    public void setText(int textId){
        TextView textView = findViewById(R.id.send_button_text);
        textView.setText(textId);
    }
}
