package rs.readahead.washington.mobile.views.fragment.vault.adapters

import org.hzontal.shared_ui.buttons.HomeButton
import rs.readahead.washington.mobile.data.entity.XFormEntity
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.data.VaultFile

interface VaultClickListener : FilesActionsListener {
    fun onPanicModeSwipeListener(progress : Int)
    fun onRecentFilesItemClickListener(vaultFile: VaultFile)
    fun onFavoriteItemClickListener(form: XFormEntity)
}

interface FilesActionsListener{
    fun allFilesClickListener()
    fun imagesClickListener()
    fun audioClickListener()
    fun documentsClickListener()
    fun othersClickListener()
    fun videoClickListener()
}
