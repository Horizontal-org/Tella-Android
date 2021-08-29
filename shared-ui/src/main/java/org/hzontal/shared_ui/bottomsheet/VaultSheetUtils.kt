package org.hzontal.shared_ui.bottomsheet

import android.app.Activity
import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import org.hzontal.shared_ui.R
import org.hzontal.shared_ui.utils.DialogUtils

object VaultSheetUtils {
    interface IVaultActions {
        fun upload()
        fun share()
        fun move()
        fun rename()
        fun save()
        fun info()
        fun delete()
    }
    @JvmStatic
    fun showVaultActionsSheet(
        fragmentManager: FragmentManager,
        titleText: String?,
        uploadLabel: String,
        shareLabel: String,
        moveLabel: String,
        renameLabel: String,
        saveLabel: String,
        infoLabel: String,
        deleteLabel: String,
        action: IVaultActions
    ) {
        val vaultActionSheet = CustomBottomSheetFragment.with(fragmentManager)
            .page(R.layout.vault_actions_sheet_layout)
            .cancellable(true)
        vaultActionSheet.holder(VaultActionsSheetHolder(), object :
            CustomBottomSheetFragment.Binder<VaultActionsSheetHolder> {
            override fun onBind(holder: VaultActionsSheetHolder) {
                with(holder) {
                    title.text = titleText
                    //Rename action
                    actionRename.text = renameLabel
                    actionRename.setOnClickListener {
                        vaultActionSheet.dismiss()
                        action.rename()
                    }
                    //Delete action
                    actionDelete.text = deleteLabel
                    actionDelete.setOnClickListener {
                        vaultActionSheet.dismiss()
                        action.delete()
                    }
                    //Upload action
                    actionUpload.text = uploadLabel
                    actionUpload.setOnClickListener {
                        vaultActionSheet.dismiss()
                        action.upload()
                    }
                    //Share action
                    actionShare.text = shareLabel
                    actionShare.setOnClickListener {
                        vaultActionSheet.dismiss()
                        action.share()
                    }
                    //Move action
                    actionMove.text = moveLabel
                    actionMove.setOnClickListener {
                        vaultActionSheet.dismiss()
                        action.move()
                    }
                    //Save action
                    actionSave.text = saveLabel
                    actionSave.setOnClickListener {
                        vaultActionSheet.dismiss()
                        action.save()
                    }
                    //Info action
                    actionInfo.text = infoLabel
                    actionInfo.setOnClickListener {
                        vaultActionSheet.dismiss()
                        action.info()
                    }
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

    @JvmStatic
    fun showVaultRenameSheet(
        fragmentManager: FragmentManager,
        titleText: String?,
        cancelLabel: String,
        confirmLabel: String,
        context: Activity,
        fileName : String,
        onConfirmClick: ((String) -> Unit)? = null
    ) {
        val vaultActionSheet = CustomBottomSheetFragment.with(fragmentManager)
            .page(R.layout.sheet_rename)
            .screenTag("vaultActionSheet")
            .cancellable(true)
        vaultActionSheet.holder(VaultRenameSheetHolder(), object :
            CustomBottomSheetFragment.Binder<VaultRenameSheetHolder> {
            override fun onBind(holder: VaultRenameSheetHolder) {
                with(holder) {
                    title.text = titleText
                    renameEditText.setText(fileName)
                    //Cancel action
                    actionCancel.text = cancelLabel
                    actionCancel.setOnClickListener { vaultActionSheet.dismiss() }

                    //Rename action
                    actionRename.text = confirmLabel
                    actionRename.setOnClickListener {
                        if (!renameEditText.text.isNullOrEmpty()) {
                            vaultActionSheet.dismiss()
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
        vaultActionSheet.transparentBackground()
        vaultActionSheet.launch()
    }

    class VaultRenameSheetHolder : CustomBottomSheetFragment.PageHolder() {
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



    interface IVaultSortActions {
        fun onSortDateASC()
        fun onSortDateDESC()
        fun onSortNameDESC()
        fun onSortNameASC()
    }
    @JvmStatic
    fun showVaultSortSheet(
        fragmentManager: FragmentManager,
        titleText: String?,
        filterNameAZ: String?,
        filterNameZA: String,
        filterASC: String,
        filterDESC: String,
        sort: IVaultSortActions
    ) {
        val vaultSortSheet = CustomBottomSheetFragment.with(fragmentManager)
            .page(R.layout.layout_sort_vault)
            .screenTag("vaultSortSheet")
            .cancellable(true)
        vaultSortSheet.holder(VaultSortSheetHolder(), object :
            CustomBottomSheetFragment.Binder<VaultSortSheetHolder> {
            override fun onBind(holder: VaultSortSheetHolder) {
                with(holder) {
                    title.text = titleText
                    //Sort NAME A-Z action
                    radioBtnNameAZ.text = filterNameAZ
                    //Sort NAME Z-A action
                    radioBtnNameZA.text = filterNameZA
                    //Sort DATE ASC
                    radioBtnASC.text = filterASC
                    //Sort DATE DESC
                    radioBtnDESC.text = filterDESC

                    radioGroup.setOnCheckedChangeListener { radioGroup, checkedId ->
                        when (radioGroup.checkedRadioButtonId) {
                            R.id.radioBtnNameAZ -> {
                                vaultSortSheet.dismiss()
                                sort.onSortNameASC()
                            }
                            R.id.radioBtnNameZA -> {
                                vaultSortSheet.dismiss()
                                sort.onSortNameDESC()
                            }
                            R.id.radioBtnDESC -> {
                                vaultSortSheet.dismiss()
                                sort.onSortDateDESC()
                            }
                            R.id.radioBtnASC -> {
                                vaultSortSheet.dismiss()
                                sort.onSortDateASC()
                            }
                        }
                    }
                }
            }
        })
        vaultSortSheet.transparentBackground()
        vaultSortSheet.launch()
    }
    class VaultSortSheetHolder : CustomBottomSheetFragment.PageHolder() {
        lateinit var title: TextView
        lateinit var radioBtnNameAZ : RadioButton
        lateinit var radioBtnNameZA : RadioButton
        lateinit var radioBtnASC : RadioButton
        lateinit var radioBtnDESC : RadioButton
        lateinit var radioGroup: RadioGroup

        override fun bindView(view: View) {
            radioBtnNameAZ = view.findViewById(R.id.radioBtnNameAZ)
            radioBtnNameZA = view.findViewById(R.id.radioBtnNameZA)
            radioBtnASC = view.findViewById(R.id.radioBtnASC)
            radioBtnDESC = view.findViewById(R.id.radioBtnDESC)
            radioGroup = view.findViewById(R.id.radio_list)
            title = view.findViewById(R.id.standard_sheet_title)
        }
    }

}