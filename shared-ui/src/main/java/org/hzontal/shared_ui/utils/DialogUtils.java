package org.hzontal.shared_ui.utils;

import android.app.Activity;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorRes;

import org.hzontal.shared_ui.R;


public class DialogUtils {

    public static void showBottomMessage(Activity context, String msg, Boolean isError) {
        showBottomMessage(context, msg, isError ? R.color.wa_red_error : R.color.wa_orange);
    }

    private static void showBottomMessage(Activity context, String msg, @ColorRes int colorRes) {
        ViewGroup container = context.findViewById(android.R.id.content);
        View view = LayoutInflater.from(context).inflate(R.layout.layout_bottom_message, container, false);

        TextView txv_msg = view.findViewById(R.id.txv_msg);
        txv_msg.setText(msg);
        container.addView(view);
        view.setAlpha(0f);
        view.animate().alphaBy(1f).setDuration(500).withEndAction(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (view.isAttachedToWindow()) {
                    view.animate().alpha(0).setStartDelay(2000).setDuration(500);
                }
            }
        });
    }

}