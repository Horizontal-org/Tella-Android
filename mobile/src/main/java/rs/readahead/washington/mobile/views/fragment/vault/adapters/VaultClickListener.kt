package rs.readahead.washington.mobile.views.fragment.vault.adapters

import rs.readahead.washington.mobile.data.entity.XFormEntity
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.data.VaultFile

interface VaultClickListener : FilesActionsListener {
    fun onPanicModeSwipeListener(progress : Int)
    fun onRecentFilesItemClickListener(vaultFile: VaultFile)
    fun onFavoriteItemClickListener(form: XFormEntity)
}

interface FilesActionsListener{
    fun myFilesClickListener()
    fun galleryClickListener()
    fun audioClickListener()
    fun documentsClickListener()
    fun othersClickListener()
}