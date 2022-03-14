package rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.data

import com.hzontal.tella_vault.VaultFile
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm
import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate

sealed class DataItem {
    abstract val id: String

    data class RecentFiles(val vaultFiles: List<VaultFile?>) : DataItem() {
        override val id = vaultFiles[0]?.id.toString()
    }

    data class FavoriteForms(val forms: List<CollectForm>) : DataItem() {
        override val id: String = forms[0].id.toString()
    }

    data class FavoriteTemplates(val templates: List<CollectTemplate>) : DataItem() {
        override val id: String = templates[0].id.toString()
    }

    data class FileActions(val idActions: String) : DataItem() {
        override val id = idActions
    }

    data class Titles(val idTitles: String) : DataItem() {
        override val id = idTitles
    }
}