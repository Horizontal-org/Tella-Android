package rs.readahead.washington.mobile.views.fragment.vault.adapters

import com.hzontal.tella_vault.VaultFile
import rs.readahead.washington.mobile.data.entity.XFormEntity

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
