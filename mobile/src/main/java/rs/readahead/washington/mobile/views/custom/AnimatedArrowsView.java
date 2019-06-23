package rs.readahead.washington.mobile.views.custom;

import android.animation.AnimatorInflater;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import rs.readahead.washington.mobile.R;

public class AnimatedArrowsView extends RelativeLayout {
    @BindView(R.id.arrow_3)
    ImageView arrow_3;
    @BindView(R.id.arrow_2)
    ImageView arrow_2;
    @BindView(R.id.arrow_1)
    ImageView arrow_1;

    public AnimatedArrowsView(Context context) {
        super(context);
        animateArrows(context);
    }

    public AnimatedArrowsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        animateArrows(context);
    }

    public AnimatedArrowsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        animateArrows(context);
    }

    public void animateArrows(Context context) {
        inflate(getContext(), R.layout.home_panic_button_layout, this);
        ButterKnife.bind(this);
        ObjectAnimator animator1 = (ObjectAnimator) AnimatorInflater.loadAnimator(context, R.animator.fade_in);
        animator1.setTarget(arrow_1);
        ObjectAnimator animator2 = (ObjectAnimator) AnimatorInflater.loadAnimator(context, R.animator.fade_in);
        animator2.setTarget(arrow_2);
        ObjectAnimator animator3 = (ObjectAnimator) AnimatorInflater.loadAnimator(context, R.animator.fade_in);
        animator3.setTarget(arrow_3);

        animator2.setStartDelay(250);
        animator3.setStartDelay(500);

        animator1.start();
        animator2.start();
        animator3.start();
    }


}
