package org.hzontal.shared_ui.bottomsheet

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import org.hzontal.shared_ui.R

class CustomTopSheetFragment : CustomBottomSheetFragment() {


    override fun onStart() {
        if (dialog != null && dialog!!.window != null) {
            dialog?.let {
                it.window?.let { window ->
                    if (animationStyle != null) window.attributes.windowAnimations =
                        animationStyle!!
                    if (isTransparent) window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) else {
                        if (isFullscreen) window.setBackgroundDrawable(
                            ColorDrawable(
                                resources.getColor(
                                    R.color.dark_purple
                                )
                            )
                        )
                        // For top sheet effect, set a custom animation or remove animation.
                        // Example: window.attributes.windowAnimations = R.style.TopSheetAnimation
                        // Or: window.attributes.windowAnimations = 0
                    }
                    // Set gravity to show the sheet at the top of the screen.
                    window.setGravity(Gravity.TOP)
                }
            }
        }
        super.onStart()

        if (isFullscreen) {
            val sheetContainer = requireView().parent as? ViewGroup ?: return
            sheetContainer.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        // Adjust dialog window to appear at the top of the screen.
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(dialog.window?.attributes)
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        layoutParams.gravity = Gravity.TOP
        dialog.window?.attributes = layoutParams

        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog.setOnShowListener {
            lifecycleScope.launch {
                // Expand the top sheet.
                val bottomSheet =
                    (dialog as? BottomSheetDialog)?.findViewById<View>(R.id.design_bottom_sheet) as? FrameLayout
                bottomSheet?.let {
                    BottomSheetBehavior.from(it).state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }

        return dialog
    }


    companion object {
        /**
         * Called to init CustomBottomSheetFragment object with fragmentManager.
         * Mandatory
         *
         * @param fragmentManager Object used to launch CustomBottomSheetFragment
         * @return Instantiated CustomBottomSheetFragment object
         */
        fun with(fragmentManager: FragmentManager): CustomTopSheetFragment {
            val process = CustomTopSheetFragment()
            process.manager = fragmentManager
            return process
        }
    }

}


