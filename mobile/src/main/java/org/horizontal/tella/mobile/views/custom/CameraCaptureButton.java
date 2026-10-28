package org.horizontal.tella.mobile.views.custom;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

import org.horizontal.tella.mobile.R;
import org.horizontal.tella.mobile.util.ViewUtil;


public class
CameraCaptureButton extends AppCompatImageButton implements View.OnTouchListener{
    private enum Appearance { PHOTO, VIDEO, STOP }

    private final Paint photoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Appearance appearance = Appearance.PHOTO;
    private float photoDiskScale = 1f;
    private ValueAnimator photoAnimator;

    public CameraCaptureButton(Context context) {
        this(context, null);
    }

    public CameraCaptureButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraCaptureButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOnTouchListener(this);
        setBackgroundColor(Color.TRANSPARENT);
        setImageResource(android.R.color.transparent);
    }

    public void displayPhotoButton() {
        display(Appearance.PHOTO);
    }

    public void displayVideoButton() {
        display(Appearance.VIDEO);
    }

    public void displayStopVideo() {
        display(Appearance.STOP);
    }

    private void display(Appearance nextAppearance) {
        if (appearance == nextAppearance) return;
        resetPhotoAnimation();
        appearance = nextAppearance;
        invalidate();
    }

    public void animatePhotoCapture() {
        if (appearance != Appearance.PHOTO) return;
        resetPhotoAnimation();
        photoAnimator = ValueAnimator.ofFloat(1f, 0.78f, 1f);
        photoAnimator.setDuration(200L);
        photoAnimator.setInterpolator(new LinearInterpolator());
        photoAnimator.addUpdateListener(animation -> {
            photoDiskScale = (float) animation.getAnimatedValue();
            invalidate();
        });
        photoAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float density = getResources().getDisplayMetrics().density;
        float strokeWidth = 2.5f * density;
        float width = getWidth() - getPaddingLeft() - getPaddingRight();
        float height = getHeight() - getPaddingTop() - getPaddingBottom();
        float centerX = getPaddingLeft() + width / 2f;
        float centerY = getPaddingTop() + height / 2f;
        float radius = Math.max(0f, Math.min(width, height) / 2f - strokeWidth / 2f);

        photoPaint.setStyle(Paint.Style.STROKE);
        photoPaint.setStrokeWidth(strokeWidth);
        photoPaint.setColor(ContextCompat.getColor(getContext(), R.color.camera_design_ring));
        canvas.drawCircle(centerX, centerY, radius, photoPaint);

        photoPaint.setStyle(Paint.Style.FILL);
        if (appearance == Appearance.STOP) {
            photoPaint.setColor(ContextCompat.getColor(getContext(), R.color.camera_recording_stop_red));
            float halfSize = radius * 0.48f;
            float cornerRadius = 4f * density;
            canvas.drawRoundRect(centerX - halfSize, centerY - halfSize,
                    centerX + halfSize, centerY + halfSize, cornerRadius, cornerRadius, photoPaint);
        } else {
            boolean photo = appearance == Appearance.PHOTO;
            photoPaint.setColor(photo ? Color.WHITE :
                    ContextCompat.getColor(getContext(), R.color.camera_recording_stop_red));
            // Idle video uses a red disk with a clear gap inside the same ring as photo.
            float diskRadius = photo ? Math.max(0f, radius - 6f * density) * photoDiskScale
                    : radius * 0.76f;
            canvas.drawCircle(centerX, centerY, diskRadius, photoPaint);
        }
    }

    private void resetPhotoAnimation() {
        if (photoAnimator != null) {
            photoAnimator.cancel();
            photoAnimator = null;
        }
        photoDiskScale = 1f;
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        resetPhotoAnimation();
        super.onDetachedFromWindow();
    }

    public void rotateView(int angle){
        animate().rotation(angle).start();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        if (enabled) {
            setAlpha(1f);
        } else {
            setAlpha(0.5f);
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (appearance != Appearance.PHOTO) {
            ViewUtil.animateTouchWithAlpha(view, motionEvent);
        }
        return false;
    }
}
