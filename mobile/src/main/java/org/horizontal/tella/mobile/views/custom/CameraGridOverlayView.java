package org.horizontal.tella.mobile.views.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;


public class CameraGridOverlayView extends View {

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public CameraGridOverlayView(Context context) {
        this(context, null);
    }

    public CameraGridOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(getResources().getDisplayMetrics().density);
        linePaint.setColor(0x80FFFFFF);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        if (width <= 0f || height <= 0f) {
            return;
        }
        float thirdW = width / 3f;
        float thirdH = height / 3f;
        canvas.drawLine(thirdW, 0f, thirdW, height, linePaint);
        canvas.drawLine(2f * thirdW, 0f, 2f * thirdW, height, linePaint);
        canvas.drawLine(0f, thirdH, width, thirdH, linePaint);
        canvas.drawLine(0f, 2f * thirdH, width, 2f * thirdH, linePaint);
    }
}
