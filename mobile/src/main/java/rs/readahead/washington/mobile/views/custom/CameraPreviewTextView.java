package rs.readahead.washington.mobile.views.custom;

import android.Manifest;
import android.content.Context;
import androidx.appcompat.widget.AppCompatTextView;
import android.util.AttributeSet;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.util.PermissionUtil;


public class CameraPreviewTextView extends AppCompatTextView {
    public CameraPreviewTextView(Context context) {
        this(context, null);
    }

    public CameraPreviewTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraPreviewTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setText(R.string.home_action_expl_camera);
    }
}
