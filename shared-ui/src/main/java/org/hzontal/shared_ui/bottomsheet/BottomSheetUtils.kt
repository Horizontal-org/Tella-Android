package org.hzontal.shared_ui.bottomsheet

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
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
                        val button = inflater.inflate(R.layout.radio_list_item_layout, null) as RadioButton
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

    class DualChoiceSheetHolder : CustomBottomSheetFragment.PageHolder() {
        lateinit var cancelButton: ImageView
        lateinit var buttonOne: TextView
        lateinit var buttonTwo: TextView
        lateinit var title: TextView
        lateinit var description: TextView

        override fun bindView(view: View) {
            cancelButton = view.findViewById(R.id.standard_sheet_cancel_btn)
            buttonOne = view.findViewById(R.id.sheet_one_btn)
            buttonTwo = view.findViewById(R.id.sheet_two_btn)
            title = view.findViewById(R.id.standard_sheet_title)
            description = view.findViewById(R.id.standard_sheet_content)
        }
    }

    interface DualChoiceConsumer {
        fun accept(option: Boolean)
    }

    @JvmStatic
    fun showDualChoiceTypeSheet(
        fragmentManager: FragmentManager,
        titleText: String?,
        descriptionText: String?,
        buttonOneLabel: String? = null,
        buttonTwoLabel: String? = null,
        consumer: DualChoiceConsumer? = null
    ) {

        val customSheetFragment = CustomBottomSheetFragment.with(fragmentManager)
            .page(R.layout.dual_choose_layout)
            .cancellable(true)
            .fullScreen()
            .statusBarColor(R.color.space_cadet)
        customSheetFragment.holder(DualChoiceSheetHolder(), object :
            CustomBottomSheetFragment.Binder<DualChoiceSheetHolder> {
            override fun onBind(holder: DualChoiceSheetHolder) {
                with(holder) {
                    title.text = titleText
                    description.text = descriptionText
                    buttonOneLabel?.let {
                        buttonOne.text = it
                    }
                    buttonTwoLabel?.let {
                        buttonTwo.text = it
                    }

                    buttonOne.setOnClickListener {
                        consumer?.accept(true)
                        customSheetFragment.dismiss()
                    }

                    buttonTwo.setOnClickListener {
                        consumer?.accept(false)
                        customSheetFragment.dismiss()
                    }

                    cancelButton.setOnClickListener {
                        customSheetFragment.dismiss()
                    }
                }
            }
        })

        customSheetFragment.transparentBackground()
        customSheetFragment.launch()
    }

    enum class CamouflageOption {
        CUSTOM, CALCULATOR, CHANGE_LOCK
    }

    class CamouflageSheetHolder : CustomBottomSheetFragment.PageHolder() {
        lateinit var sheetTitle: TextView
        lateinit var sheetsubTitle: TextView
        lateinit var cancelButton: ImageView
        lateinit var buttonOneTitle: TextView
        lateinit var buttonOneSubtitle: TextView
        lateinit var title: TextView
        //lateinit var description: TextView

        override fun bindView(view: View) {
            title = view.findViewById(R.id.dialog_title)
            cancelButton = view.findViewById(R.id.standard_sheet_cancel_btn)
            buttonOneTitle = view.findViewById(R.id.title_btn_one)
            buttonOneSubtitle = view.findViewById(R.id.subtitle_btn_one)
            sheetTitle = view.findViewById(R.id.sheet_title)
            sheetsubTitle = view.findViewById(R.id.sheet_subtitle)
           /* title = view.findViewById(R.id.standard_sheet_title)
            description = view.findViewById(R.id.standard_sheet_content)*/
        }
    }


    @JvmStatic
    fun showChangeCamouflageSheet(
        fragmentManager: FragmentManager,
        titleText: String?,
        sheetTitle: String?,
        sheetSubtitle: String?,
        titleOne: String?,
        subtitleOne: String?,
        consumer: DualChoiceConsumer? = null
    ) {

        val customSheetFragment = CustomBottomSheetFragment.with(fragmentManager)
            .page(R.layout.change_camouflage_layout)
            .cancellable(true)
            .fullScreen()
            .statusBarColor(R.color.space_cadet)
        customSheetFragment.holder(CamouflageSheetHolder(), object :
            CustomBottomSheetFragment.Binder<CamouflageSheetHolder> {
            override fun onBind(holder: CamouflageSheetHolder) {
                with(holder) {
                    title.text = titleText
                    buttonOneTitle.text = titleOne
                    buttonOneSubtitle.text = subtitleOne
                    sheetTitle.text = sheetTitle
                    sheetSubitle.text = sheetSubtitle
                    /*description.text = descriptionText
                    buttonOneLabel?.let {
                        buttonOne.text = it
                    }
                    buttonTwoLabel?.let {
                        buttonTwo.text = it
                    }

                    buttonOne.setOnClickListener {
                        consumer?.accept(true)
                        customSheetFragment.dismiss()
                    }

                    buttonTwo.setOnClickListener {
                        consumer?.accept(false)
                        customSheetFragment.dismiss()
                    }*/

                    cancelButton.setOnClickListener {
                        customSheetFragment.dismiss()
                    }
                }
            }
        })

        customSheetFragment.transparentBackground()
        customSheetFragment.launch()
    }

}
