package org.horizontal.tella.mobile.views.fragment.vault.adapters.viewholders.data

import com.hzontal.tella_vault.VaultFile
import org.horizontal.tella.mobile.domain.entity.collect.CollectForm
import org.horizontal.tella.mobile.domain.entity.uwazi.UwaziTemplate
import org.horizontal.tella.mobile.views.fragment.vault.adapters.ID_CONNECTIONS
import org.horizontal.tella.mobile.views.fragment.vault.adapters.ID_FAVORITE_FORMS
import org.horizontal.tella.mobile.views.fragment.vault.adapters.ID_FAVORITE_TEMPLATES
import org.horizontal.tella.mobile.views.fragment.vault.adapters.ID_RECENT_FILES
import org.horizontal.tella.mobile.views.fragment.vault.adapters.connections.ServerDataItem

sealed class DataItem {
    abstract val id: String

    data class ConnectionsItem(val item: List<ServerDataItem>) : DataItem() {
        override val id = ID_CONNECTIONS
    }

    data class RecentFiles(val vaultFiles: List<VaultFile?>) : DataItem() {
        override val id = ID_RECENT_FILES
    }

    data class FavoriteForms(val forms: List<CollectForm>) : DataItem() {
        override val id: String = ID_FAVORITE_FORMS
    }

    data class FavoriteTemplates(val templates: List<UwaziTemplate>) : DataItem() {
        override val id: String = ID_FAVORITE_TEMPLATES
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