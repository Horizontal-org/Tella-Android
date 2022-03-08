package rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.data

import com.hzontal.tella_vault.VaultFile
import rs.readahead.washington.mobile.data.entity.XFormEntity
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm

sealed class DataItem {
    abstract val id: String

    data class RecentFiles(val vaultFiles: List<VaultFile?>) : DataItem() {
        override val id = vaultFiles[0]?.id.toString()
    }

    data class FavoriteForms(val forms: List<CollectForm>) : DataItem() {
        override val id: String = forms[0].id.toString()
    }

    data class FileActions(val idActions: String) : DataItem() {
        override val id = idActions
    }

    data class Titles(val idTitles: String) : DataItem() {
        override val id = idTitles
    }

    data class ImproveAction(val idTitles: String) : DataItem() {
        override val id = idTitles
    }
}