package org.hzontal.shared_ui.bottomsheet

import android.content.Context
import android.content.DialogInterface
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

    interface ServerTypeConsumer {
        fun accept(isCollectServer: Boolean)
    }

    @JvmStatic
    fun showDualChoiceTypeSheet(
        fragmentManager: FragmentManager,
        titleText: String?,
        descriptionText: String?,
        buttonOneLabel: String? = null,
        buttonTwoLabel: String? = null,
        consumer: ServerTypeConsumer? = null
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

    interface IVaultActions{
        fun upload()
        fun share()
        fun move()
        fun rename()
        fun save()
        fun info()
        fun delete()
    }
    fun showVaultActionsSheet(
        fragmentManager: FragmentManager,
        titleText: String?,
        uploadLabel : String,
        shareLabel: String,
        moveLabel: String,
        renameLabel: String,
        saveLabel: String,
        infoLabel: String,
        deleteLabel: String,
        action : IVaultActions
    ){
        val vaultActionSheet = CustomBottomSheetFragment.with(fragmentManager)
            .page(R.layout.vault_actions_sheet_layout)
            .cancellable(true)
        vaultActionSheet.holder(VaultActionsSheetHolder(), object :
            CustomBottomSheetFragment.Binder<VaultActionsSheetHolder> {
            override fun onBind(holder: VaultActionsSheetHolder) {
                with(holder){
                    title.text = titleText
                    //Rename action
                    actionRename.text = renameLabel
                    actionRename.setOnClickListener { action.rename() }
                    //Delete action
                    actionDelete.text = deleteLabel
                    actionDelete.setOnClickListener { action.delete() }
                    //Upload action
                    actionUpload.text = uploadLabel
                    actionUpload.setOnClickListener { action.upload() }
                    //Share action
                    actionShare.text = shareLabel
                    actionShare.setOnClickListener { action.share() }
                    //Move action
                    actionMove.text = moveLabel
                    actionMove.setOnClickListener { action.move() }
                    //Save action
                    actionSave.text = saveLabel
                    actionSave.setOnClickListener { action.save() }
                    //Info action
                    actionInfo.text = infoLabel
                    actionInfo.setOnClickListener { action.info() }
                }
            }
            })
        vaultActionSheet.transparentBackground()
        vaultActionSheet.launch()
    }

    class VaultActionsSheetHolder : CustomBottomSheetFragment.PageHolder() {
        lateinit var actionRename: TextView
        lateinit var actionDelete: TextView
        lateinit var actionUpload: TextView
        lateinit var actionShare: TextView
        lateinit var actionMove: TextView
        lateinit var actionInfo: TextView
        lateinit var actionSave: TextView
        lateinit var title: TextView

        override fun bindView(view: View) {
            actionRename = view.findViewById(R.id.renameActionTV)
            actionDelete = view.findViewById(R.id.deleteActionTV)
            actionUpload = view.findViewById(R.id.uploadActionTV)
            actionShare = view.findViewById(R.id.shareActionTV)
            actionMove = view.findViewById(R.id.moveActionTV)
            actionInfo = view.findViewById(R.id.infoActionTV)
            actionSave = view.findViewById(R.id.saveActionTV)
            title = view.findViewById(R.id.sheetTitleTv)
        }
    }
}
