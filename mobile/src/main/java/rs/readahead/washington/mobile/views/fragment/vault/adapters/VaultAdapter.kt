package rs.readahead.washington.mobile.views.fragment.vault.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.hzontal.tella_vault.VaultFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm
import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate
import rs.readahead.washington.mobile.views.fragment.vault.adapters.connections.ServerDataItem
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.*
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.BaseViewHolder
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.data.DataItem

const val ITEM_CONNECTIONS = 0
const val ITEM_RECENT_FILES = 1
const val ITEM_FAVORITES_FORMS = 2
const val ITEM_FAVORITES_TEMPLATES = 3
const val ITEM_PANIC_BUTTON = 4
const val ITEM_FILES_ACTIONS = 5
const val ITEM_TITLE = 6
const val ITEM_TEllA_IMPROVE = 7

const val ID_CONNECTIONS = "1"
private const val ID_FILES_ACTIONS = "2"
private const val ID_PANIC_MODE = "3"
private const val ID_FILES_TITLE = "4"
private const val ID_IMPROVEMENT = "5"


class VaultAdapter(private val onClick: VaultClickListener) :
    androidx.recyclerview.widget.ListAdapter<DataItem, BaseViewHolder<*>>(
        ListDiffCallback()
    ) {
    private val adapterScope = CoroutineScope(Dispatchers.Default)
    private var recentFiles = listOf<DataItem.RecentFiles>()
    private var favoriteForms = listOf<DataItem.FavoriteForms>()
    private var favoriteTemplates = listOf<DataItem.FavoriteTemplates>()
    private var actions = listOf<DataItem.FileActions>()
    private var titles = listOf<DataItem.Titles>()
    private var improveInsights = listOf<DataItem.ImproveAction>()
    private var connections = listOf<DataItem.ConnectionsItem>()
    private var items = listOf<DataItem>()

    init {
        addFileActions()
    }

    fun addConnectionServers(connectionsList: List<ServerDataItem>) {
        connections = listOf(DataItem.ConnectionsItem(connectionsList))
        renderList()
    }

    fun removeConnectionServers() {
        adapterScope.launch {
            items = items - connections.toSet()
            withContext(Dispatchers.Main) {
                submitList(items)
            }
        }
    }

    fun addRecentFiles(vaultFiles: List<VaultFile?>) {
        recentFiles = listOf(DataItem.RecentFiles(vaultFiles))
        renderList()
    }

    fun removeRecentFiles() {
        adapterScope.launch {
            items = items - recentFiles.toSet()
            withContext(Dispatchers.Main) {
                submitList(items)
            }
        }
    }

    fun removeFavoriteForms() {
        adapterScope.launch {
            items = items - favoriteForms.toSet()
            withContext(Dispatchers.Main) {
                submitList(items)
            }
        }
    }

    fun removeFavoriteTemplates() {
        adapterScope.launch {
            items = items - favoriteTemplates.toSet()
            withContext(Dispatchers.Main) {
                submitList(items)
            }
        }
    }

    fun removeTitle() {
        adapterScope.launch {
            items = items - titles.toSet()
            withContext(Dispatchers.Main) {
                submitList(items)
            }
        }
    }

    private fun addFileActions() {
        actions = listOf(
            DataItem.FileActions(
                ID_FILES_ACTIONS
            )
        )
        renderList()
    }

    fun addImprovementSection() {
        if ((Preferences.isShowVaultImprovementSection() && !Preferences.hasAcceptedImprovements()) || Preferences.isTimeToShowReminderImprovements()) {
            improveInsights = listOf(DataItem.ImproveAction(ID_IMPROVEMENT))
            renderList()
        }
    }

    fun removeImprovementSection() {
        adapterScope.launch {
            items = items - improveInsights.toSet()
            withContext(Dispatchers.Main) { submitList(items) }
        }
    }

    fun addFavoriteForms(forms: List<CollectForm>) {
        favoriteForms = listOf(DataItem.FavoriteForms(forms))
        renderList()
    }

    fun addTitle() {
        titles = listOf(DataItem.Titles(ID_FILES_TITLE))
        renderListAfterward()
    }

    fun addFavoriteTemplates(templates: List<CollectTemplate>) {
        favoriteTemplates = listOf(DataItem.FavoriteTemplates(templates))
        renderList()
    }

    private fun renderList() {
        adapterScope.launch {
            items = connections + improveInsights + favoriteForms + favoriteTemplates + recentFiles + titles + actions
            withContext(Dispatchers.Main.immediate) {
                submitList(items)
            }
        }
    }

    private fun renderListAfterward() {
        adapterScope.launch {
            items = emptyList()
            if (Preferences.isShowFavoriteForms()) {
                items = items + favoriteForms
            }
            if (Preferences.isShowRecentFiles()) {
                items = items + recentFiles
            }
            if (Preferences.isShowFavoriteTemplates()) {
                items = items + favoriteTemplates

            }
            items = items + titles
            items = items + actions

            withContext(Dispatchers.Main) {
                submitList(items)
            }
        }
    }

    override fun onCreateViewHolder(view: ViewGroup, viewType: Int): BaseViewHolder<*> {
        return when (viewType) {
            ITEM_CONNECTIONS -> {
                ServersViewHolder.from(view)
            }
            ITEM_RECENT_FILES -> {
                RecentFilesViewHolder.from(view)
            }
            ITEM_FAVORITES_FORMS -> {
                FavoriteFormsViewHolder.from(view)
            }
            ITEM_FAVORITES_TEMPLATES -> {
                FavoriteTemplatesViewHolder.from(view)
            }
            ITEM_PANIC_BUTTON -> {
                PanicModeViewHolder.from(view)
            }
            ITEM_FILES_ACTIONS -> {
                FileActionsViewHolder.from(view)
            }
            ITEM_TITLE -> {
                TitleViewHolder.from(view)
            }
            ITEM_TEllA_IMPROVE -> {
                ImproveViewHolder.from(view)
            }
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder<*>, position: Int) {
        when (holder) {
            is ServersViewHolder -> {
                val connectionsList = getItem(position) as DataItem.ConnectionsItem
                holder.bind(connectionsList.item, onClick)
            }
            is FavoriteFormsViewHolder -> {
                val favoriteItem = getItem(position) as DataItem.FavoriteForms
                holder.bind(favoriteItem.forms, onClick)
            }
            is FavoriteTemplatesViewHolder -> {
                val favoriteItemTemplate = getItem(position) as DataItem.FavoriteTemplates
                holder.bind(favoriteItemTemplate.templates, onClick)
            }
            is RecentFilesViewHolder -> {
                val recentFiles = getItem(position) as DataItem.RecentFiles
                holder.bind(recentFiles.vaultFiles, onClick)
            }
            is FileActionsViewHolder -> {
                val recentFiles = getItem(position) as DataItem.FileActions
                holder.bind(recentFiles.idActions, onClick)
            }
            is TitleViewHolder -> {
                val titles = getItem(position) as DataItem.Titles
                holder.bind(titles.idTitles, onClick)
            }
            is ImproveViewHolder -> {
                holder.bind(null, onClick)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DataItem.ConnectionsItem -> ITEM_CONNECTIONS
            is DataItem.FileActions -> ITEM_FILES_ACTIONS
            is DataItem.RecentFiles -> ITEM_RECENT_FILES
            is DataItem.FavoriteForms -> ITEM_FAVORITES_FORMS
            is DataItem.FavoriteTemplates -> ITEM_FAVORITES_TEMPLATES
            is DataItem.Titles -> ITEM_TITLE
            is DataItem.ImproveAction -> ITEM_TEllA_IMPROVE
            else -> throw ClassCastException("Unknown position $position")
        }
    }
}

class ListDiffCallback : DiffUtil.ItemCallback<DataItem>() {
    override fun areItemsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
        return oldItem == newItem
    }

}