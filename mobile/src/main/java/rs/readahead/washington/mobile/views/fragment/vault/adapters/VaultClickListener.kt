package rs.readahead.washington.mobile.views.fragment.vault.adapters

import com.hzontal.tella_vault.VaultFile
import rs.readahead.washington.mobile.data.entity.XFormEntity
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm

interface VaultClickListener : FilesActionsListener {
    fun onRecentFilesItemClickListener(vaultFile: VaultFile)
    fun onFavoriteItemClickListener(form: CollectForm)
}

interface FilesActionsListener{
    fun allFilesClickListener()
    fun imagesClickListener()
    fun audioClickListener()
    fun documentsClickListener()
    fun othersClickListener()
    fun videoClickListener()
}
