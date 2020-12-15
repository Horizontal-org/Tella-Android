package rs.readahead.washington.mobile.views.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.lzyzsd.circleprogress.DonutProgress;

import rs.readahead.washington.mobile.R;

public class StopResumeUploadButton extends FrameLayout {
    public DonutProgress donutProgress;
    public ImageView button;

    public StopResumeUploadButton(@NonNull Context context) {
        super(context);
    }

    public StopResumeUploadButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        inflateView();
    }

    public StopResumeUploadButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView();
    }

    private void inflateView() {
        inflate(getContext(), R.layout.stop_resume_upload_button_layout, this);
        donutProgress = findViewById(R.id.donut_progress);
        button = findViewById(R.id.uploadIndicator);
    }

    public void setUploaded() {
        button.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_check_circle_green));
        button.setPadding(0, 0, 0, 0);
        donutProgress.setVisibility(View.GONE);
    }

    public void setProgress(int progress) {
        donutProgress.setVisibility(View.VISIBLE);
        donutProgress.setProgress(progress);
        button.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_stop_black_24dp));
        button.setPadding(20, 20, 20, 20);
    }

    public void setStopped() {
        button.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_refresh_black_24dp));
        button.setPadding(0, 0, 0, 0);
        donutProgress.setVisibility(View.GONE);
    }
}
