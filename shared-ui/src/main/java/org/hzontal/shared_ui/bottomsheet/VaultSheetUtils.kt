package org.hzontal.shared_ui.bottomsheet

import android.app.Activity
import android.view.View
import android.widget.*
import androidx.core.view.isVisible
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
    fun showVaultActionsSheet(fragmentManager: FragmentManager, titleText: String?, uploadLabel: String, shareLabel: String, moveLabel: String, renameLabel: String, saveLabel: String, infoLabel: String, deleteLabel: String, isDirectory: Boolean = false, isMultipleFiles: Boolean = false, isUploadVisible: Boolean = false, isMoveVisible: Boolean = false, action: IVaultActions) {
        val vaultActionSheet = CustomBottomSheetFragment.with(fragmentManager).page(R.layout.vault_actions_sheet_layout).cancellable(true).screenTag("vaultActionSheet")
        vaultActionSheet.holder(VaultActionsSheetHolder(), object : CustomBottomSheetFragment.Binder<VaultActionsSheetHolder> {
            override fun onBind(holder: VaultActionsSheetHolder) {
                with(holder) {
                    title.text = titleText
                    //Actions visibility
                    if (isDirectory) {
                        seperator.visibility = View.GONE
                        actionShare.visibility = View.GONE
                        actionUpload.visibility = View.GONE
                    }
                    if (isMultipleFiles) {
                        actionRename.visibility = View.GONE
                        actionInfo.visibility = View.GONE
                    }
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
                    if (!isUploadVisible) {
                        actionUpload.visibility = View.GONE
                    }
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
                    actionMove.isVisible = isMoveVisible
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
        lateinit var seperator: View

        override fun bindView(view: View) {
            actionRename = view.findViewById(R.id.renameActionTV)
            actionDelete = view.findViewById(R.id.deleteActionTV)
            actionUpload = view.findViewById(R.id.uploadActionTV)
            actionShare = view.findViewById(R.id.shareActionTV)
            actionMove = view.findViewById(R.id.moveActionTV)
            actionInfo = view.findViewById(R.id.infoActionTV)
            actionSave = view.findViewById(R.id.saveActionTV)
            title = view.findViewById(R.id.sheetTitleTv)
            seperator = view.findViewById(R.id.separator)
        }
    }

    @JvmStatic
    fun showVaultBlueRenameSheet(fragmentManager: FragmentManager, titleText: String?, cancelLabel: String, confirmLabel: String, context: Activity, fileName: String?, onConfirmClick: ((String) -> Unit)? = null) {
        val vaultActionSheet = CustomBottomSheetFragment.with(fragmentManager).page(R.layout.blue_sheet_rename).screenTag("VaultRenameSheet").cancellable(true)
        vaultActionSheet.holder(VaultRenameSheetHolder(), object : CustomBottomSheetFragment.Binder<VaultRenameSheetHolder> {
            override fun onBind(holder: VaultRenameSheetHolder) {
                with(holder) {
                    title.text = titleText
                    renameEditText.setText(fileName)
                    //renameEditText.setHint(R.string.Rename_name_Hint)
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
                            DialogUtils.showBottomMessage(context, "Please fill in the new name", true)
                        }

                    }
                }
            }
        })
        vaultActionSheet.transparentBackground()
        vaultActionSheet.launch()
    }

    @JvmStatic
    fun showVaultRenameSheet(fragmentManager: FragmentManager, titleText: String?, cancelLabel: String, confirmLabel: String, context: Activity, fileName: String?, onConfirmClick: ((String) -> Unit)? = null) {
        val vaultActionSheet = CustomBottomSheetFragment.with(fragmentManager).page(R.layout.sheet_rename).screenTag("VaultRenameSheet").cancellable(true)
        vaultActionSheet.holder(VaultRenameSheetHolder(), object : CustomBottomSheetFragment.Binder<VaultRenameSheetHolder> {
            override fun onBind(holder: VaultRenameSheetHolder) {
                with(holder) {
                    title.text = titleText
                    renameEditText.setText(fileName)
                    //renameEditText.setHint(R.string.Rename_name_Hint)
                    renameEditText.requestFocus()
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
                            DialogUtils.showBottomMessage(context, "Please fill in the new name", true)
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
    fun showVaultSortSheet(fragmentManager: FragmentManager, titleText: String?, filterNameAZ: String?, filterNameZA: String, filterASC: String, filterDESC: String, sort: IVaultSortActions) {
        val vaultSortSheet = CustomBottomSheetFragment.with(fragmentManager).page(R.layout.layout_sort_vault).screenTag("vaultSortSheet").cancellable(true)
        vaultSortSheet.holder(VaultSortSheetHolder(), object : CustomBottomSheetFragment.Binder<VaultSortSheetHolder> {
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
        lateinit var radioBtnNameAZ: RadioButton
        lateinit var radioBtnNameZA: RadioButton
        lateinit var radioBtnASC: RadioButton
        lateinit var radioBtnDESC: RadioButton
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

    interface IVaultManageFiles {
        fun goToCamera()
        fun goToRecorder()
        fun chooseImportAndDelete()
        fun createFolder()
    }

    @JvmStatic
    fun showVaultManageFilesSheet(fragmentManager: FragmentManager, cameraLabel: String?, recordLabel: String?, importLabel: String, createFolderLabel: String, titleText: String, isImportVisible: Boolean, isCreateFolderVisible: Boolean, action: IVaultManageFiles) {
        val vaultManageFilesSheet = CustomBottomSheetFragment.with(fragmentManager).page(R.layout.manage_files_layout).screenTag("vaultManageFilesSheet").cancellable(true)
        vaultManageFilesSheet.holder(VaultManageFilesSheetHolder(), object : CustomBottomSheetFragment.Binder<VaultManageFilesSheetHolder> {
            override fun onBind(holder: VaultManageFilesSheetHolder) {
                with(holder) {
                    title.text = titleText
                    title.text = titleText
                    //Go to camera action
                    cameraActionTV.text = cameraLabel
                    cameraActionTV.setOnClickListener {
                        vaultManageFilesSheet.dismiss()
                        action.goToCamera()
                    }
                    //Go to recorder action
                    recordAudioActionTV.text = recordLabel
                    recordAudioActionTV.setOnClickListener {
                        vaultManageFilesSheet.dismiss()
                        action.goToRecorder()
                    }
                    cameraActionTV.isVisible = cameraLabel != null
                    recordAudioActionTV.isVisible = recordLabel != null
                    //Import action
                    importActionTV.isVisible = isImportVisible
                    importActionTV.text = importLabel
                    importActionTV.setOnClickListener {
                        vaultManageFilesSheet.dismiss()
                        action.chooseImportAndDelete()
                    }
                    //Move action
                    createFolderActionTV.isVisible = isCreateFolderVisible
                    if (isCreateFolderVisible) {
                        createFolderActionTV.visibility = View.VISIBLE
                        createFolderActionTV.text = createFolderLabel
                        createFolderActionTV.setOnClickListener {
                            vaultManageFilesSheet.dismiss()
                            action.createFolder()
                        }
                    }
                    //delete action

                }
            }
        })
        vaultManageFilesSheet.transparentBackground()
        vaultManageFilesSheet.launch()
    }

    class VaultManageFilesSheetHolder : CustomBottomSheetFragment.PageHolder() {
        lateinit var cameraActionTV: TextView
        lateinit var recordAudioActionTV: TextView
        lateinit var importActionTV: TextView
        lateinit var createDeleteActionTV: TextView
        lateinit var createFolderActionTV: TextView
        lateinit var deleteVaultTooltip: ImageView
        lateinit var title: TextView

        override fun bindView(view: View) {
            cameraActionTV = view.findViewById(R.id.cameraActionTV)
            recordAudioActionTV = view.findViewById(R.id.recordAudioActionTV)
            importActionTV = view.findViewById(R.id.importActionTV)
            createFolderActionTV = view.findViewById(R.id.createFolderActionTV)
            title = view.findViewById(R.id.sheetTitleTv)
        }
    }

    interface IVaultFilesSelector {
        fun goToCamera()
        fun goToRecorder()
        fun importFromVault()
        fun importFromDevice()
    }

    @JvmStatic
    fun showVaultSelectFilesSheet(fragmentManager: FragmentManager, cameraLabel: String?, recordLabel: String?, importLabel: String, importVaultLabel: String, descriptionText: String?, titleText: String, action: IVaultFilesSelector) {
        val vaultManageFilesSheet = CustomBottomSheetFragment.with(fragmentManager).page(R.layout.vault_sheet_actions_selector).screenTag("vaultManageFilesSheet").cancellable(true)
        vaultManageFilesSheet.holder(VaultSelectFilesSheetHolder(), object : CustomBottomSheetFragment.Binder<VaultSelectFilesSheetHolder> {
            override fun onBind(holder: VaultSelectFilesSheetHolder) {
                with(holder) {
                    title.text = titleText
                    if (descriptionText != null) {
                        descriptionTV.text = descriptionText
                    } else {
                        descriptionTV.isVisible = false
                    }

                    //Go to camera action
                    if (cameraLabel != null) {
                        cameraActionTV.text = cameraLabel
                        cameraActionTV.setOnClickListener {
                            vaultManageFilesSheet.dismiss()
                            action.goToCamera()
                        }
                    } else {
                        cameraActionTV.visibility = View.GONE
                    }
                    //Go to recorder action
                    if (recordLabel != null) {
                        recordAudioActionTV.text = recordLabel
                        recordAudioActionTV.setOnClickListener {
                            vaultManageFilesSheet.dismiss()
                            action.goToRecorder()
                        }
                    } else {
                        recordAudioActionTV.visibility = View.GONE
                    }

                    importActionTV.text = importLabel
                    importActionTV.setOnClickListener {
                        vaultManageFilesSheet.dismiss()
                        action.importFromDevice()
                    }


                    importVaultActionTV.text = importVaultLabel
                    importVaultActionTV.setOnClickListener {
                        vaultManageFilesSheet.dismiss()
                        action.importFromVault()
                    }

                }

            }
        })
        vaultManageFilesSheet.transparentBackground()
        vaultManageFilesSheet.launch()
    }

    class VaultSelectFilesSheetHolder : CustomBottomSheetFragment.PageHolder() {
        lateinit var cameraActionTV: TextView
        lateinit var recordAudioActionTV: TextView
        lateinit var importActionTV: TextView
        lateinit var importVaultActionTV: TextView
        lateinit var title: TextView
        lateinit var descriptionTV: TextView

        override fun bindView(view: View) {
            cameraActionTV = view.findViewById(R.id.cameraActionTV)
            recordAudioActionTV = view.findViewById(R.id.recordActionTV)
            descriptionTV = view.findViewById(R.id.sheetTitleDescTv)
            importActionTV = view.findViewById(R.id.selectDeviceFilesActionTV)
            importVaultActionTV = view.findViewById(R.id.selectTellaFilesActionTV)
            title = view.findViewById(R.id.sheetTitleTv)
        }
    }

}