package rs.readahead.washington.mobile.views.fragment.vault.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rs.readahead.washington.mobile.data.entity.XFormEntity
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.FavoriteFormsViewHolder
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.FileActionsViewHolder
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.PanicModeViewHolder
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.RecentFilesViewHolder
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.BaseViewHolder
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.data.DataItem
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.data.VaultFile

const val ITEM_RECENT_FILES = 0
const val ITEM_FAVORITES_FORMS = 1
const val ITEM_PANIC_BUTTON = 2
const val ITEM_FILES_ACTIONS = 3


class VaultAdapter(private val onClick: VaultClickListener) :
    androidx.recyclerview.widget.ListAdapter<DataItem, BaseViewHolder<*>>(
        ListDiffCallback()
    ) {
    private val adapterScope = CoroutineScope(Dispatchers.Default)
    private var panicMode = listOf<DataItem.PanicMode>()
    private var recentFiles = listOf<DataItem.RecentFiles>()
    private var favoriteForms = listOf<DataItem.FavoriteForms>()
    private var actions = listOf<DataItem.FileActions>()
    private var items = listOf<DataItem>()

    fun addPanicMode(vaultFile: VaultFile) {
        panicMode = listOf(
            DataItem.PanicMode(
                vaultFile
            )
        )
        renderList()
    }

    fun hidePanicMode(){
        adapterScope.launch {
            items =   items - panicMode
            withContext(Dispatchers.Main) {
                submitList(items)
            }
        }
    }
    fun addRecentFiles(vaultFiles : List<VaultFile>) {
        recentFiles = listOf(DataItem.RecentFiles(vaultFiles))
        renderList()
    }
    fun addFileActions(vaultFile: VaultFile) {
        actions = listOf(
            DataItem.FileActions(
                vaultFile
            )
        )
        renderList()
    }
    fun addFavoriteForms(forms : List<XFormEntity>) {
        favoriteForms = listOf(DataItem.FavoriteForms(forms))
        renderList()
    }

    fun renderList() {
        adapterScope.launch {
            items =   favoriteForms + recentFiles  + actions + panicMode
            withContext(Dispatchers.Main) {
                submitList(items)
            }
        }
    }

    override fun onCreateViewHolder(view: ViewGroup, viewType: Int): BaseViewHolder<*> {
        return when (viewType) {
            ITEM_RECENT_FILES -> {
                RecentFilesViewHolder.from(view)
            }
            ITEM_FAVORITES_FORMS -> {
                FavoriteFormsViewHolder.from(view)
            }
            ITEM_PANIC_BUTTON -> {
                PanicModeViewHolder.from(view)
            }
            ITEM_FILES_ACTIONS -> {
                FileActionsViewHolder.from(view)
            }
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder<*> , position: Int) {
        when (holder) {
            is PanicModeViewHolder -> {
                val panicItem = getItem(position) as DataItem.PanicMode
                holder.bind(panicItem.vaultFile,onClick)
            }
            is FavoriteFormsViewHolder -> {
                val favoriteItem = getItem(position) as DataItem.FavoriteForms
                holder.bind(favoriteItem.forms,onClick)
            }
            is RecentFilesViewHolder -> {
                val recentFiles = getItem(position) as DataItem.RecentFiles
                holder.bind(recentFiles.vaultFiles,onClick)
            }
            is FileActionsViewHolder -> {
                val recentFiles = getItem(position) as DataItem.FileActions
                holder.bind(recentFiles.vaultFile,onClick)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DataItem.FileActions -> ITEM_FILES_ACTIONS
            is DataItem.RecentFiles -> ITEM_RECENT_FILES
            is DataItem.PanicMode -> ITEM_PANIC_BUTTON
            is DataItem.FavoriteForms -> ITEM_FAVORITES_FORMS
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