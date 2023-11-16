package org.hzontal.shared_ui.utils

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import org.hzontal.shared_ui.R


object DialogUtils {

    @JvmStatic
    fun showBottomMessage(context: Activity?, msg: String, isError: Boolean,duration: Long = 2000L) {
        context?.let { showBottomMessage(it, msg, if (isError) R.color.wa_red_error else R.color.tigers_eye,duration) }
    }

    @JvmStatic
    fun showBottomMessage(context: Activity?, msg: String, isError: Boolean) {
        context?.let { showBottomMessage(it, msg, if (isError) R.color.wa_red_error else R.color.tigers_eye,2000L) }
    }


    @JvmStatic
    private fun showBottomMessage(context: Activity, msg: String, colorRes: Int, duration: Long) {
        val container = context.findViewById<ViewGroup>(android.R.id.content)
        val view = LayoutInflater.from(context).inflate(R.layout.layout_bottom_message, container, false)
        val txvMsg: TextView = view.findViewById(R.id.txv_msg)
        txvMsg.text = msg
        container.addView(view)

        view.requestFocus()
        view.announceForAccessibility(msg)

        view.alpha = 0f
        view.animate().alphaBy(1f).setDuration(500).withEndAction {
            if (view.isAttachedToWindow) {
                view.animate().alpha(0f).setStartDelay(duration).duration = 500
            }
        }
    }

    @JvmStatic
    fun showBottomMessageWithButton(context: Activity, msg: String, onBtnClick: () -> Unit) {
        val container = context.findViewById<ViewGroup>(android.R.id.content)

        // Create a FrameLayout to hold both the overlay and the dialog content
        val frameLayout = FrameLayout(context)
        frameLayout.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        // Create a transparent background overlay
        val overlay = View(context)
        overlay.setBackgroundColor(ContextCompat.getColor(context, android.R.color.black))
        overlay.alpha = 0.5f // Adjust the alpha value as needed
        overlay.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        frameLayout.addView(overlay)

        // Create a transparent view to disable interaction with the views behind
        val disableView = View(context)
        disableView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
        disableView.isClickable = true
        disableView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        frameLayout.addView(disableView)


        val view = LayoutInflater.from(context).inflate(R.layout.layout_bottom_message_with_button, container, false)
        val txvMsg: TextView = view.findViewById(R.id.txv_msg)
        txvMsg.text = msg
        val btnOk: AppCompatButton = view.findViewById(R.id.btn_ok)
        frameLayout.addView(view)

        view.requestFocus()
        view.announceForAccessibility(msg)

        // Set up the "OK" button
        btnOk.setOnClickListener {
            // Dismiss the message when the button is clicked
            container.removeView(frameLayout)
            onBtnClick.invoke()
        }
        container.addView(frameLayout)
    }


}
