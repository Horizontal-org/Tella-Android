package rs.readahead.washington.mobile.views.fragment.vault.adapters

import com.hzontal.tella_vault.VaultFile
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm
import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate
import rs.readahead.washington.mobile.views.fragment.vault.adapters.connections.ServerDataItem

interface VaultClickListener : FilesActionsListener {
    fun onRecentFilesItemClickListener(vaultFile: VaultFile)
    fun onFavoriteItemClickListener(form: CollectForm)
    fun onImproveItemClickListener(improveClickOptions: ImproveClickOptions)
    fun onFavoriteTemplateClickListener(template: CollectTemplate)
    fun onServerItemClickListener(item: ServerDataItem)
}

interface FilesActionsListener {
    fun allFilesClickListener()
    fun imagesClickListener()
    fun audioClickListener()
    fun documentsClickListener()
    fun othersClickListener()
    fun videoClickListener()
}

enum class ImproveClickOptions { CLOSE, YES, LEARN_MORE, SETTINGS }
