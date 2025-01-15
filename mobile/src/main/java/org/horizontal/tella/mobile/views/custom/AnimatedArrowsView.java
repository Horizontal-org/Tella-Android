package org.horizontal.tella.mobile.views.custom;

import android.animation.AnimatorInflater;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;

import org.horizontal.tella.mobile.R;
import org.horizontal.tella.mobile.databinding.HomePanicButtonLayoutBinding;

public class AnimatedArrowsView extends RelativeLayout {
    private final HomePanicButtonLayoutBinding binding;

    public AnimatedArrowsView(Context context, HomePanicButtonLayoutBinding binding) {
        super(context);
        this.binding = HomePanicButtonLayoutBinding.inflate(LayoutInflater.from(context), this, true);
        animateArrows(context);
    }

    public AnimatedArrowsView(Context context, AttributeSet attrs, HomePanicButtonLayoutBinding binding) {
        super(context, attrs);
        this.binding = HomePanicButtonLayoutBinding.inflate(LayoutInflater.from(context), this, true);
        animateArrows(context);
    }

    public AnimatedArrowsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        animateArrows(context);
        this.binding = HomePanicButtonLayoutBinding.inflate(LayoutInflater.from(context), this, true);
    }

    public void animateArrows(Context context) {
        inflate(getContext(), R.layout.home_panic_button_layout, this);

        ObjectAnimator animator1 = (ObjectAnimator) AnimatorInflater.loadAnimator(context, R.animator.fade_in);
        animator1.setTarget(binding.arrow1);
        ObjectAnimator animator2 = (ObjectAnimator) AnimatorInflater.loadAnimator(context, R.animator.fade_in);
        animator2.setTarget(binding.arrow2);
        ObjectAnimator animator3 = (ObjectAnimator) AnimatorInflater.loadAnimator(context, R.animator.fade_in);
        animator3.setTarget(binding.arrow3);

        animator2.setStartDelay(250);
        animator3.setStartDelay(500);

        animator1.start();
        animator2.start();
        animator3.start();
    }
}
