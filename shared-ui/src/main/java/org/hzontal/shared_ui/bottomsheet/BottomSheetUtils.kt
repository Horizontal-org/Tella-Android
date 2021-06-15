package org.hzontal.shared_ui.bottomsheet

import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.hzontal.shared_ui.R

object BottomSheetUtils {

    fun showStandardSheet(
            fragmentManager: FragmentManager,
            titleText: String?,
            descriptionText: String?,
            actionButtonLabel: String? = null,
            cancelButtonLabel: String? = null,
            onConfirmClick: (() -> Unit)? = null,
            onCancelClick: (() -> Unit)? = null
    ) {

        val customSheetFragment = CustomBottomSheetFragment.with(fragmentManager)
                .page(R.layout.standar_sheet_layout)
                .cancellable(true)
        customSheetFragment.holder(GenericSheetHolder(), object :
                CustomBottomSheetFragment.Binder<GenericSheetHolder> {
            override fun onBind(holder: GenericSheetHolder) {
                with(holder) {
                    title.text = titleText
                    description.text = descriptionText
                    actionButtonLabel?.let {
                        actionButton.text = it
                    }
                    cancelButtonLabel?.let {
                        cancelButton.text = it
                    }

                    actionButton.setOnClickListener {
                        onConfirmClick?.invoke()
                        customSheetFragment.dismiss()
                    }

                    cancelButton.setOnClickListener {
                        onCancelClick?.invoke()
                        customSheetFragment.dismiss()
                    }

                    actionButton.visibility =
                            if (actionButtonLabel.isNullOrEmpty()) View.GONE else View.VISIBLE

                }
            }
        })

        customSheetFragment.transparentBackground()
        customSheetFragment.launch()
    }

    class GenericSheetHolder : CustomBottomSheetFragment.PageHolder() {
        lateinit var actionButton: TextView
        lateinit var cancelButton: TextView
        lateinit var title: TextView
        lateinit var description: TextView

        override fun bindView(view: View) {
            actionButton = view.findViewById(R.id.standard_sheet_confirm_btn)
            cancelButton = view.findViewById(R.id.standard_sheet_cancel_btn)
            title = view.findViewById(R.id.standard_sheet_title)
            description = view.findViewById(R.id.standard_sheet_content)
        }
    }


}
