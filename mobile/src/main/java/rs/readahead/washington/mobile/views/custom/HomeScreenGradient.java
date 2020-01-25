package rs.readahead.washington.mobile.views.custom;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;

import rs.readahead.washington.mobile.R;


public class HomeScreenGradient extends FrameLayout {
    private Animation fadeIn, fadeOut;

    public HomeScreenGradient(@NonNull Context context) {
        this(context, null);
    }

    public HomeScreenGradient(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HomeScreenGradient(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void animateFadeIn() {
        startAnimation(fadeIn);
    }

    public void animateFadeOut() {
        startAnimation(fadeOut);
    }

    private void init() {
        fadeIn = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in);
        fadeIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        fadeOut = AnimationUtils.loadAnimation(getContext(), R.anim.fade_out);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }
}
