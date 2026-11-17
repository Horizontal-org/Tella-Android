package org.horizontal.tella.mobile.views.fragment.ouisync

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.data.ouisync.OuisyncClient
import org.horizontal.tella.mobile.databinding.FragmentOuisyncListBinding
import org.horizontal.tella.mobile.util.navigateSafe
import org.horizontal.tella.mobile.views.base_ui.BaseFragment
import org.hzontal.shared_ui.utils.DialogUtils
import timber.log.Timber

class OuisyncRepositoriesFragment : BaseFragment() {
    private var binding: FragmentOuisyncListBinding? = null
    private var loadJob: Job? = null
    private var loadVersion = 0L
    private var loading = false
    private val adapter = OuisyncListAdapter(onClick = { row ->
        if (!loading) {
            nav().navigateSafe(
                R.id.action_ouisyncRepos_to_files,
                bundleOf(REPO_NAME to row.path, REPO_LABEL to row.name)
            )
        }
    })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOuisyncListBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun initView(view: View) {
        val listBinding = binding ?: return
        parentFragmentManager.setFragmentResultListener(
            OuisyncImportFragment.IMPORT_COMPLETE, viewLifecycleOwner
        ) { _, _ ->
            DialogUtils.showBottomMessage(
                baseActivity,
                getString(R.string.ouisync_imported_repository),
                false
            )
            load()
        }
        listBinding.empty.setText(R.string.ouisync_empty)
        listBinding.list.adapter = adapter
        listBinding.fab.isVisible = true
        listBinding.toolbar.backClickListener = { requireActivity().finish() }
        listBinding.toolbar.onRightClickListener = { load() }
        listBinding.fab.setOnClickListener { nav().navigateSafe(R.id.action_ouisyncRepos_to_import) }
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    override fun onDestroyView() {
        loadVersion++
        loadJob?.cancel()
        loadJob = null
        binding?.list?.adapter = null
        binding = null
        loading = false
        super.onDestroyView()
    }

    private fun load() {
        val listBinding = binding ?: return
        val version = ++loadVersion
        loadJob?.cancel()
        val context = requireContext().applicationContext
        loading = true
        listBinding.progress.isVisible = true
        listBinding.emptyFolder.isVisible = false
        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                val repos = OuisyncClient.repositories(context)
                if (binding !== listBinding || version != loadVersion) return@launch
                adapter.submit(repos.map { OuisyncRow(it.name, it.path, directory = true) })
                listBinding.emptyFolder.isVisible = repos.isEmpty()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e("ouisync list failed: ${e.javaClass.simpleName}")
                if (binding === listBinding && version == loadVersion) {
                    DialogUtils.showBottomMessage(
                        baseActivity,
                        getString(R.string.ouisync_failed),
                        true
                    )
                }
            } finally {
                if (binding === listBinding && version == loadVersion) {
                    loadJob = null
                    loading = false
                    listBinding.progress.isVisible = false
                }
            }
        }
    }

    companion object {
        const val REPO_NAME = "repoName"
        const val REPO_LABEL = "repoLabel"
    }
}
