package org.hzontal.shared_ui.bottomsheet

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.fragment.app.FragmentManager
import org.hzontal.shared_ui.R
import org.hzontal.shared_ui.utils.DialogUtils

object
BottomSheetUtils {

    @JvmStatic
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
                        val radioButton: AppCompatRadioButton =
                            radioGroup.findViewById(radioGroup.checkedRadioButtonId)
                        val option = radioButton.tag as Long
                        consumer.accept(option)
                        customSheetFragment.dismiss()
                    }

                    cancelButton.setOnClickListener {
                        customSheetFragment.dismiss()
                    }

                    for (option in radioList) {
                        val inflater = LayoutInflater.from(context)
                        val button =
                            inflater.inflate(R.layout.radio_list_item_layout, null) as RadioButton
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

    class CamouflageSheetHolder : CustomBottomSheetFragment.PageHolder() {
        lateinit var sheetTitle: TextView
        lateinit var sheetsubTitle: TextView
        lateinit var cancelButton: ImageView
        lateinit var buttonOneTitle: TextView
        lateinit var buttonOneSubtitle: TextView
        lateinit var title: TextView
        lateinit var buttonTwoTitle: TextView
        lateinit var buttonTwoSubtitle: TextView
        lateinit var buttonOne: View
        lateinit var buttonTwo: View

        override fun bindView(view: View) {
            title = view.findViewById(R.id.dialog_title)
            cancelButton = view.findViewById(R.id.standard_sheet_cancel_btn)
            buttonOneTitle = view.findViewById(R.id.title_btn_one)
            buttonOneSubtitle = view.findViewById(R.id.subtitle_btn_one)
            sheetTitle = view.findViewById(R.id.sheet_title)
            sheetsubTitle = view.findViewById(R.id.sheet_subtitle)
            buttonOne = view.findViewById(R.id.sheet_one_btn)
            buttonTwo = view.findViewById(R.id.sheet_two_btn)
            buttonTwoTitle = view.findViewById(R.id.title_btn_two)
            buttonTwoSubtitle = view.findViewById(R.id.subtitle_btn_two)
        }
    }


    @JvmStatic
    fun showChangeCamouflageSheet(
        fragmentManager: FragmentManager,
        titleText: String?,
        dialogTitle: String?,
        dialogSubtitle: String?,
        titleOne: String?,
        subtitleOne: String?,
        titleTwo: String?,
        subtitleTwo: String?,
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
                    sheetTitle.text = dialogTitle
                    sheetsubTitle.text = dialogSubtitle
                    buttonTwoTitle.text = titleTwo
                    buttonTwoSubtitle.text = subtitleTwo

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

    interface ActionConfirmed {
        fun accept(isConfirmed: Boolean)
    }

    @JvmStatic
    fun showConfirmSheet(
        fragmentManager: FragmentManager,
        titleText: String?,
        descriptionText: String?,
        actionButtonLabel: String? = null,
        cancelButtonLabel: String? = null,
        consumer: ActionConfirmed
    ) {

        val customSheetFragment = CustomBottomSheetFragment.with(fragmentManager)
            .page(R.layout.standar_sheet_layout)
            .cancellable(true)
            .screenTag("ConfirmSheet")
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
                        consumer.accept(isConfirmed = true)
                        customSheetFragment.dismiss()
                    }

                    cancelButton.setOnClickListener {
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

    interface UploadServerConsumer {
        fun accept(serverId: Long)
    }

    @JvmStatic
    fun showChooseAutoUploadServerSheet(
        fragmentManager: FragmentManager,
        titleText: String?,
        descriptionText: String?,
        actionButtonLabel: String? = null,
        cancelButtonLabel: String? = null,
        radioList: LinkedHashMap<Long, String>,
        currentServerId: Long,
        context: Context,
        consumer: UploadServerConsumer
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
                        val radioButton: AppCompatRadioButton =
                            radioGroup.findViewById(radioGroup.checkedRadioButtonId)
                        val option = radioButton.tag as Long
                        consumer.accept(option)
                        customSheetFragment.dismiss()
                    }

                    cancelButton.setOnClickListener {
                        customSheetFragment.dismiss()
                    }

                    for (option in radioList) {
                        val inflater = LayoutInflater.from(context)
                        val button =
                            inflater.inflate(R.layout.radio_list_item_layout, null) as RadioButton
                        button.tag = option.key
                        button.setText(option.value)
                        radioGroup.addView(button)
                        if (option.key == currentServerId) {
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

    enum class Action {
        EDIT, DELETE
    }

    class ServerMenuSheetHolder : CustomBottomSheetFragment.PageHolder() {
        lateinit var actionEdit: TextView
        lateinit var actionDelete: TextView
        lateinit var title: TextView

        override fun bindView(view: View) {
            actionEdit = view.findViewById(R.id.action_edit)
            actionDelete = view.findViewById(R.id.action_delete)
            title = view.findViewById(R.id.standard_sheet_title)
        }
    }

    interface ActionSeleceted {
        fun accept(action: Action)
    }

    @JvmStatic
    fun showServerMenuSheet(
        fragmentManager: FragmentManager,
        titleText: String?,
        actionEditLabel: String? = null,
        actionDeleteLabel: String? = null,
        consumer: ActionSeleceted,
        titleText2: String?,
        descriptionText: String?,
        actionButtonLabel: String? = null,
        cancelButtonLabel: String? = null
    ) {

        val customSheetFragment2 = CustomBottomSheetFragment.with(fragmentManager)
            .page(R.layout.standar_sheet_layout)
            .cancellable(true)
        customSheetFragment2.holder(GenericSheetHolder(), object :
            CustomBottomSheetFragment.Binder<GenericSheetHolder> {
            override fun onBind(holder: GenericSheetHolder) {
                with(holder) {
                    title.text = titleText2
                    description.text = descriptionText
                    actionButtonLabel?.let {
                        actionButton.text = it
                    }
                    cancelButtonLabel?.let {
                        cancelButton.text = it
                    }

                    actionButton.setOnClickListener {
                        consumer.accept(action = Action.DELETE)
                        customSheetFragment2.dismiss()
                    }

                    cancelButton.setOnClickListener {
                        customSheetFragment2.dismiss()
                    }

                    actionButton.visibility =
                        if (actionButtonLabel.isNullOrEmpty()) View.GONE else View.VISIBLE

                }
            }
        })

        val customSheetFragment = CustomBottomSheetFragment.with(fragmentManager)
            .page(R.layout.server_menu_sheet_layout)
            .cancellable(true)
        customSheetFragment.holder(ServerMenuSheetHolder(), object :
            CustomBottomSheetFragment.Binder<ServerMenuSheetHolder> {
            override fun onBind(holder: ServerMenuSheetHolder) {
                with(holder) {
                    title.text = titleText
                    actionEditLabel?.let {
                        actionEdit.text = it
                    }
                    actionDeleteLabel?.let {
                        actionDelete.text = it
                    }

                    actionEdit.setOnClickListener {
                        consumer.accept(action = Action.EDIT)
                        customSheetFragment.dismiss()
                    }

                    actionDelete.setOnClickListener {
                        //consumer.accept(action = Action.DELETE)
                        fragmentManager.beginTransaction()
                            .add(customSheetFragment2, customSheetFragment2.tag)
                            .commit()
                        customSheetFragment.dismiss()
                    }
                }
            }
        })

        customSheetFragment.transparentBackground()
        customSheetFragment.launch()
    }

    class RenameFileSheetHolder : CustomBottomSheetFragment.PageHolder() {
        lateinit var actionCancel: TextView
        lateinit var actionRename: TextView
        lateinit var title: TextView
        lateinit var renameEditText: EditText

        override fun bindView(view: View) {
            actionRename = view.findViewById(R.id.standard_sheet_confirm_btn)
            actionCancel = view.findViewById(R.id.standard_sheet_cancel_btn)
            title = view.findViewById(R.id.standard_sheet_title)
            renameEditText = view.findViewById(R.id.renameEditText)
        }
    }

    @JvmStatic
    fun showFileRenameSheet(
        fragmentManager: FragmentManager,
        titleText: String?,
        cancelLabel: String,
        confirmLabel: String,
        context: Activity,
        fileName: String?,
        onConfirmClick: ((String) -> Unit)? = null
    ) {
        val renameFileSheet = CustomBottomSheetFragment.with(fragmentManager)
            .page(R.layout.sheet_rename)
            .screenTag("FileRenameSheet")
            .cancellable(true)
        renameFileSheet.holder(RenameFileSheetHolder(), object :
            CustomBottomSheetFragment.Binder<RenameFileSheetHolder> {
            override fun onBind(holder: RenameFileSheetHolder) {
                with(holder) {
                    title.text = titleText
                    renameEditText.setText(fileName)
                    //Cancel action
                    actionCancel.text = cancelLabel
                    actionCancel.setOnClickListener { renameFileSheet.dismiss() }

                    //Rename action
                    actionRename.text = confirmLabel
                    actionRename.setOnClickListener {
                        if (!renameEditText.text.isNullOrEmpty()) {
                            renameFileSheet.dismiss()
                            onConfirmClick?.invoke(renameEditText.text.toString())
                        } else {
                            DialogUtils.showBottomMessage(
                                context,
                                "Please fill in the new name",
                                true
                            )
                        }

                    }
                }
            }
        })
        renameFileSheet.transparentBackground()
        renameFileSheet.launch()
    }


    class EnterCodeSheetHolder : CustomBottomSheetFragment.PageHolder() {
        lateinit var title: TextView
        lateinit var subtitle: TextView
        lateinit var description: TextView
        lateinit var enterText: EditText
        lateinit var buttonNext: TextView
        lateinit var cancelButton: ImageView


        override fun bindView(view: View) {
            title = view.findViewById(R.id.sheet_title)
            subtitle = view.findViewById(R.id.sheet_subtitle)
            description = view.findViewById(R.id.sheet_description)
            enterText = view.findViewById(R.id.code_editText)
            buttonNext = view.findViewById(R.id.next_btn)
            cancelButton = view.findViewById(R.id.standard_sheet_cancel_btn)
        }
    }

    @JvmStatic
    fun showEnterCustomizationCodeSheet(
        fragmentManager: FragmentManager,
        titleText: String?,
        subTitle: String?,
        descriptionText: String?,
        nextButton: String?,
        consumer: StringConsumer? = null
    ) {

        val customSheetFragment = CustomBottomSheetFragment.with(fragmentManager)
            .page(R.layout.enter_string_bottomsheet_layout)
            .cancellable(true)
            .fullScreen()
            .statusBarColor(R.color.space_cadet)
        customSheetFragment.holder(EnterCodeSheetHolder(), object :
            CustomBottomSheetFragment.Binder<EnterCodeSheetHolder> {
            override fun onBind(holder: EnterCodeSheetHolder) {
                with(holder) {
                    title.text = titleText
                    subtitle.text = subTitle
                    description.text = descriptionText
                    buttonNext.text = nextButton
                    buttonNext.setOnClickListener {
                        if (enterText.text.isNotEmpty()) {
                            consumer?.accept(enterText.text.toString())
                            customSheetFragment.dismiss()
                        }
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

    interface StringConsumer {
        fun accept(code: String)
    }

}
