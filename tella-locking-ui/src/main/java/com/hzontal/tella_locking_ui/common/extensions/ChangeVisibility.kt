package com.hzontal.tella_locking_ui.common.extensions

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View

fun View.toggleVisibility(show: Boolean) {
    var alpha = 0.0f
    var mVisibility = View.INVISIBLE

    if (show) {
        alpha = 1.0f
        mVisibility = View.VISIBLE
    }

    animate()
        .setDuration(300)
        .alpha(alpha)
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                super.onAnimationStart(animation)
                if (show) visibility = mVisibility
            }

            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                visibility = mVisibility
            }
        })
}