package org.hzontal.shared_ui.bottomsheet

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.fragment.app.FragmentManager
import org.hzontal.shared_ui.R

class CustomTopSheetFragment : CustomBottomSheetFragment() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
      //  setStyle(STYLE_NORMAL, R.style.TopSheetTheme)

    }
    override fun onStart() {
        if (dialog != null && dialog!!.window != null) {
            dialog?.let {
                it.window?.let { window ->
                    if (animationStyle != null) window.attributes.windowAnimations =
                        animationStyle!!
                    window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                }
            }
        }
        super.onStart()

        val sheetContainer = requireView().parent as? ViewGroup ?: return
        val layoutParams = sheetContainer.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.topMargin = 0
        sheetContainer.layoutParams = layoutParams
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

    /**
     * Called to init LayoutRes with ID layout.
     * Mandatory
     *
     * @param layoutRes ID layout to setContentView on CustomBottomSheetFragment Activity
     * @return Instantiated CustomBottomSheetFragment object
     */
    override fun page(@LayoutRes layoutRes: Int): CustomTopSheetFragment {
        this.layoutRes = layoutRes
        return this
    }


}


