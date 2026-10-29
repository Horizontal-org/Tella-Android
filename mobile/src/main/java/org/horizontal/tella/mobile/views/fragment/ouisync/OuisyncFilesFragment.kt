package org.horizontal.tella.mobile.views.fragment.ouisync

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.ouisync.OuisyncClient
import org.horizontal.tella.mobile.data.ouisync.OuisyncSaveResult
import org.horizontal.tella.mobile.data.ouisync.OuisyncSyncState
import org.horizontal.tella.mobile.databinding.FragmentOuisyncListBinding
import org.horizontal.tella.mobile.util.isDuplicateNameOrFileExistsError
import org.horizontal.tella.mobile.views.base_ui.BaseFragment
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.utils.DialogUtils
import timber.log.Timber

class OuisyncFilesFragment : BaseFragment() {
    private var binding: FragmentOuisyncListBinding? = null
    private val pathStack = ArrayDeque<String>()
    private var repoName: String = ""
    private var repoLabel: String = ""
    private var busy = false
    private var loading = false
    private var reloadRequested = false
    private var reloadQuiet = true
    private var loadJob: Job? = null
    private var loadVersion = 0L
    private val adapter = OuisyncListAdapter(
        onClick = { row ->
            if (busy || loading) return@OuisyncListAdapter
            if (row.directory) {
                pathStack.addLast(row.path)
                backCallback.isEnabled = true
                load(clearRows = true)
            } else {
                confirmSave(row)
            }
        },
        onMenu = { row -> if (!busy && !loading) confirmSave(row) }
    )
    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = goUp()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pathStack.addAll(savedInstanceState?.getStringArrayList(STATE_PATHS).orEmpty())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArrayList(STATE_PATHS, ArrayList(pathStack))
        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentOuisyncListBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun initView(view: View) {
        repoName = arguments?.getString(OuisyncRepositoriesFragment.REPO_NAME).orEmpty()
        repoLabel = arguments?.getString(OuisyncRepositoriesFragment.REPO_LABEL)
            ?.takeIf { it.isNotBlank() } ?: repoName.substringAfterLast('/')
        val listBinding = binding ?: return
        listBinding.list.adapter = adapter
        listBinding.toolbar.backClickListener = { if (pathStack.isEmpty()) nav().popBackStack() else goUp() }
        listBinding.toolbar.onRightClickListener = { load() }
        listBinding.saveFolderButton.setOnClickListener {
            if (!busy && !loading && pathStack.isNotEmpty()) {
                val path = currentPath()
                confirmSave(OuisyncRow(path.substringAfterLast('/'), path, directory = true))
            }
        }
        backCallback.isEnabled = pathStack.isNotEmpty()
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)
        watchForSync()
        load(clearRows = true)
    }

    private fun watchForSync() {
        val context = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                load()
                launch {
                    while (isActive) {
                        delay(REFRESH_INTERVAL_MS)
                        load(quiet = true)
                    }
                }
                try {
                    OuisyncClient.updates(context, repoName).collect { load(quiet = true) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e("ouisync watch failed: ${e.javaClass.simpleName}")
                }
            }
        }
    }

    override fun onDestroyView() {
        loadVersion++
        loadJob?.cancel()
        loadJob = null
        binding?.list?.adapter = null
        binding = null
        busy = false
        loading = false
        reloadRequested = false
        reloadQuiet = true
        super.onDestroyView()
    }

    private fun goUp() {
        if (pathStack.isEmpty()) return
        pathStack.removeLast()
        backCallback.isEnabled = pathStack.isNotEmpty()
        load(clearRows = true)
    }

    private fun currentPath(): String = pathStack.lastOrNull().orEmpty()

    private fun updateControls(listBinding: FragmentOuisyncListBinding) {
        val title = pathStack.lastOrNull()?.substringAfterLast('/')?.ifEmpty { repoLabel } ?: repoLabel
        listBinding.toolbar.setStartTextTitle(title)
        listBinding.progress.isVisible = busy || loading
        listBinding.saveFolderButton.isVisible = pathStack.isNotEmpty() && adapter.itemCount > 0
        listBinding.saveFolderButton.isChecked = !busy && !loading
    }

    private fun load(clearRows: Boolean = false, quiet: Boolean = false) {
        val listBinding = binding ?: return
        if (clearRows) {
            adapter.submit(emptyList())
            listBinding.emptyFolder.isVisible = false
            listBinding.syncing.isVisible = false
        }
        updateControls(listBinding)
        if (busy || (loadJob?.isActive == true && !clearRows)) {
            reloadRequested = true
            if (!quiet) reloadQuiet = false
            return
        }
        val version = ++loadVersion
        loadJob?.cancel()
        val requestedPath = currentPath()
        val context = requireContext().applicationContext
        reloadRequested = false
        reloadQuiet = true
        if (!quiet) {
            loading = true
            updateControls(listBinding)
            listBinding.emptyFolder.isVisible = false
        }
        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                val listing = OuisyncClient.entries(context, repoName, requestedPath)
                if (binding !== listBinding || version != loadVersion || requestedPath != currentPath()) return@launch
                adapter.submit(listing.entries.map { OuisyncRow(it.name, it.path, it.directory) })
                listBinding.syncing.setText(
                    if (listing.syncState == OuisyncSyncState.WaitingForContent) {
                        R.string.ouisync_waiting_for_content
                    } else {
                        R.string.ouisync_syncing
                    }
                )
                listBinding.syncing.isVisible = listing.syncing
                listBinding.emptyFolder.isVisible = listing.entries.isEmpty() && !listing.syncing
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e("ouisync files failed: ${e.javaClass.simpleName}")
                if (!quiet && binding === listBinding && version == loadVersion) {
                    DialogUtils.showBottomMessage(baseActivity, getString(R.string.ouisync_failed), true)
                }
            } finally {
                if (binding === listBinding && version == loadVersion) {
                    loadJob = null
                    loading = false
                    updateControls(listBinding)
                    if (reloadRequested && !busy) {
                        val nextQuiet = reloadQuiet
                        reloadQuiet = true
                        load(quiet = nextQuiet)
                    }
                }
            }
        }
    }

    private fun confirmSave(row: OuisyncRow) {
        BottomSheetUtils.showStandardSheet(
            baseActivity.supportFragmentManager,
            row.name,
            getString(if (row.directory) R.string.ouisync_save_folder_copy else R.string.ouisync_save_copy),
            getString(R.string.action_save),
            getString(R.string.action_cancel),
            onConfirmClick = { save(row) }
        )
    }

    private fun save(row: OuisyncRow) {
        val listBinding = binding ?: return
        if (busy) return
        val context = requireContext().applicationContext
        busy = true
        reloadRequested = reloadRequested || loading
        loadVersion++
        loadJob?.cancel()
        loadJob = null
        loading = false
        updateControls(listBinding)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = OuisyncClient.saveToVault(context, repoName, row.path, row.name, row.directory)
                if (binding !== listBinding) return@launch
                val message = if (result == OuisyncSaveResult.NotSynced) {
                    R.string.ouisync_not_synced
                } else if (row.directory) {
                    R.string.ouisync_folder_saved
                } else {
                    R.string.ouisync_saved
                }
                DialogUtils.showBottomMessage(baseActivity, getString(message), result == OuisyncSaveResult.NotSynced)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e("ouisync save failed: ${e.javaClass.simpleName}")
                if (binding === listBinding) {
                    val message = if (e.isDuplicateNameOrFileExistsError()) {
                        R.string.ouisync_already_in_vault
                    } else {
                        R.string.ouisync_failed
                    }
                    DialogUtils.showBottomMessage(baseActivity, getString(message), true)
                }
            } finally {
                if (binding === listBinding) {
                    busy = false
                    updateControls(listBinding)
                    if (reloadRequested) load()
                }
            }
        }
    }

    companion object {
        private const val STATE_PATHS = "ouisyncFolderPaths"
        private const val REFRESH_INTERVAL_MS = 3_000L
    }
}
