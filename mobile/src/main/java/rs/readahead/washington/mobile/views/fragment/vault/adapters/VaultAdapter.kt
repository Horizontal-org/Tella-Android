package rs.readahead.washington.mobile.views.fragment.vault.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.hzontal.tella_vault.VaultFile
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

const val ITEM_RECENT_FILES = 0
const val ITEM_FAVORITES_FORMS = 1
const val ITEM_PANIC_BUTTON = 2
const val ITEM_FILES_ACTIONS = 3

private const val ID_FILES_ACTIONS = "1"
private const val ID_PANIC_MODE = "2"


class VaultAdapter(private val onClick: VaultClickListener) :
    androidx.recyclerview.widget.ListAdapter<DataItem, BaseViewHolder<*>>(
        ListDiffCallback()
    ) {
    private val adapterScope = CoroutineScope(Dispatchers.Default)
    private var recentFiles = listOf<DataItem.RecentFiles>()
    private var favoriteForms = listOf<DataItem.FavoriteForms>()
    private var actions = listOf<DataItem.FileActions>()
    private var items = listOf<DataItem>()

    init {
        addFileActions()
    }


    fun addRecentFiles(vaultFiles: List<VaultFile?>) {
        recentFiles = listOf(DataItem.RecentFiles(vaultFiles))
        renderList()
    }

   private fun addFileActions() {
        actions = listOf(
            DataItem.FileActions(
                ID_FILES_ACTIONS
            )
        )
        renderList()
    }

    fun addFavoriteForms(forms: List<XFormEntity>) {
        favoriteForms = listOf(DataItem.FavoriteForms(forms))
        renderList()
    }

    fun renderList() {
        adapterScope.launch {
            items = favoriteForms + recentFiles + actions
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

    override fun onBindViewHolder(holder: BaseViewHolder<*>, position: Int) {
        when (holder) {
            is FavoriteFormsViewHolder -> {
                val favoriteItem = getItem(position) as DataItem.FavoriteForms
                holder.bind(favoriteItem.forms, onClick)
            }
            is RecentFilesViewHolder -> {
                val recentFiles = getItem(position) as DataItem.RecentFiles
                holder.bind(recentFiles.vaultFiles, onClick)
            }
            is FileActionsViewHolder -> {
                val recentFiles = getItem(position) as DataItem.FileActions
                holder.bind(recentFiles.idActions, onClick)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DataItem.FileActions -> ITEM_FILES_ACTIONS
            is DataItem.RecentFiles -> ITEM_RECENT_FILES
            is DataItem.FavoriteForms -> ITEM_FAVORITES_FORMS
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