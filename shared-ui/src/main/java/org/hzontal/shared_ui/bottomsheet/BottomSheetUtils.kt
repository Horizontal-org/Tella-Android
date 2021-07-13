package org.hzontal.shared_ui.bottomsheet

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.fragment.app.FragmentManager
import org.hzontal.shared_ui.R

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

    interface LockOptionConsumer {
        fun accept(option: Long)
    }

    fun showRadioListSheet(
            fragmentManager: FragmentManager,
            context: Context,
            currentTimeout: Long,
            radioList: LinkedHashMap<Long, Int>,
            titleText: String?,
            descriptionText: String?,
            actionButtonLabel: String? = null,
            cancelButtonLabel: String? = null,
            consumer: LockOptionConsumer
    ) {

        val customSheetFragment = CustomBottomSheetFragment.with(fragmentManager)
            .page(R.layout.radio_list_sheet_layout)
            .cancellable(true)
        customSheetFragment.holder(RadioListSheetHolder(), object :
            CustomBottomSheetFragment.Binder<RadioListSheetHolder> {
            override fun onBind(holder: RadioListSheetHolder) {
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
                        val radioButton: AppCompatRadioButton = radioGroup.findViewById(radioGroup.checkedRadioButtonId)
                        val option = radioButton.tag as Long
                        consumer.accept(option)
                        customSheetFragment.dismiss()
                    }

                    cancelButton.setOnClickListener {
                        customSheetFragment.dismiss()
                    }

                    for (option in radioList) {
                        val inflater = LayoutInflater.from(context)
                        val button = inflater.inflate(R.layout.radio_list_item_layut, null) as RadioButton
                        button.tag = option.key
                        button.setText(option.value)
                        radioGroup.addView(button)
                        if (option.key == currentTimeout) {
                            button.isChecked = true
                        }
                    }

                    actionButton.visibility =
                        if (actionButtonLabel.isNullOrEmpty()) View.GONE else View.VISIBLE

                }
            }
        })

        customSheetFragment.transparentBackground()
        customSheetFragment.launch()
    }

    class RadioListSheetHolder : CustomBottomSheetFragment.PageHolder() {
        lateinit var actionButton: TextView
        lateinit var cancelButton: TextView
        lateinit var title: TextView
        lateinit var description: TextView
        lateinit var radioGroup: RadioGroup

        override fun bindView(view: View) {
            actionButton = view.findViewById(R.id.standard_sheet_confirm_btn)
            cancelButton = view.findViewById(R.id.standard_sheet_cancel_btn)
            title = view.findViewById(R.id.standard_sheet_title)
            description = view.findViewById(R.id.standard_sheet_content)
            radioGroup = view.findViewById(R.id.radio_list)
        }
    }

}
