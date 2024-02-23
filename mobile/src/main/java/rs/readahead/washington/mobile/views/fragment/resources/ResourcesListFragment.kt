package rs.readahead.washington.mobile.views.fragment.resources

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.viewModels
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.BlankCollectFormRowBinding
import rs.readahead.washington.mobile.databinding.FragmentResourcesListBinding
import rs.readahead.washington.mobile.domain.entity.resources.Resource
import rs.readahead.washington.mobile.util.hide
import rs.readahead.washington.mobile.util.show
import rs.readahead.washington.mobile.views.activity.viewer.PDFReaderActivity
import rs.readahead.washington.mobile.views.activity.viewer.PDFReaderActivity.Companion.HIDE_MENU
import rs.readahead.washington.mobile.views.activity.viewer.PDFReaderActivity.Companion.VIEW_PDF
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment


@AndroidEntryPoint
class ResourcesListFragment :
    BaseBindingFragment<FragmentResourcesListBinding>(FragmentResourcesListBinding::inflate) {

    private val model: ResourcesViewModel by viewModels()

    private var availableResources = HashMap<String, Resource>()
    private var downloadedResources = HashMap<String, Resource>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!hasInitializedRootView) {
            hasInitializedRootView = true
            initView()
        }
        initObservers()
        refreshLists()
    }

    override fun onResume() {
        super.onResume()
    }

    private fun initObservers() {
        with(model) {
            resources.observe(
                viewLifecycleOwner
            ) { listFormResult ->
                onAvailableResourcesList(
                    listFormResult!!
                )
            }

            savedResources.observe(viewLifecycleOwner) { resources ->
                resources.forEach { downloadedResources[it.fileName] = it }
                updateResourcesViews()
            }

            downloadedResource.observe(viewLifecycleOwner) {
                downloadedResource(it)
            }

            pdfFile.observe(viewLifecycleOwner) {
                openPdf(it)
            }

            progress.observe(viewLifecycleOwner) {
                showProgress(it)
            }

            deletedResource.observe(viewLifecycleOwner) {
                DialogUtils.showBottomMessage(
                    baseActivity,
                    getString(
                        R.string.Resources_RemovedResource, it
                    ),
                    false
                )
            }

            error.observe(viewLifecycleOwner) {
                it?.message?.let { it1 ->
                    DialogUtils.showBottomMessage(
                        baseActivity,
                        it1,
                        true
                    )
                }
            }
        }
    }

    private fun showProgress(show: Boolean) {
        if (show) {
            binding.progressBar.show()
        } else {
            binding.progressBar.hide()
        }
    }

    private fun updateResourcesViews() {
        binding.blankResources.removeAllViews()
        createResourcesViews(availableResources.values.toList(), binding.blankResources, false)
        if (availableResources.isEmpty()) {
            binding.availableResourcesEmpty.show()
            binding.availableResourcesInfo.hide()
        } else {
            binding.availableResourcesInfo.show()
            binding.availableResourcesEmpty.hide()
        }
        binding.downloadedResources.removeAllViews()
        if (downloadedResources.isEmpty()) {
            binding.downloadedResourcesEmpty.show()
        } else {
            binding.downloadedResourcesEmpty.hide()
        }
        createResourcesViews(downloadedResources.values.toList(), binding.downloadedResources, true)
    }

    private fun downloadedResource(resource: Resource) {
        if (downloadedResources.isEmpty()) {
            binding.downloadedResourcesEmpty.hide()
        }

        binding.blankResources.removeView(
            binding.blankResources.findViewWithTag(resource.fileName)
        )

        availableResources.remove(resource.fileName)
        downloadedResources[resource.fileName] = resource
        binding.downloadedResources.addView(getResourceItem(resource, true))

        if (availableResources.isEmpty()) {
            binding.availableResourcesEmpty.show()
            binding.availableResourcesInfo.hide()
        }
    }

    private fun deletedResource(resource: Resource) {
        if (availableResources.isEmpty()) {
            binding.availableResourcesEmpty.hide()
            binding.availableResourcesInfo.show()
        }

        binding.downloadedResources.removeView(
            binding.downloadedResources.findViewWithTag(resource.fileName)
        )
        downloadedResources.remove(resource.fileName)
        availableResources[resource.fileName] = resource
        binding.blankResources.addView(getResourceItem(resource, false))

        if (downloadedResources.isEmpty()) {
            binding.downloadedResourcesEmpty.show()
        }
    }

    private fun onAvailableResourcesList(listFormResult: List<Resource>) {
        listFormResult.forEach {
            if (!downloadedResources.containsKey(it.fileName)) {
                availableResources[it.fileName] = it
            }
        }
        updateResourcesViews()
    }

    private fun createResourcesViews(
        resources: List<Resource>,
        listView: LinearLayout,
        isDownloaded: Boolean
    ) {
        for (resource in resources) {
            val view = getResourceItem(resource, isDownloaded)
            listView.addView(view, resources.indexOf(resource))
        }
    }

    private fun getResourceItem(resource: Resource?, isDownloaded: Boolean): View {
        val itemBinding =
            BlankCollectFormRowBinding.inflate(
                LayoutInflater.from(context),
                binding.resources,
                false
            )
        itemBinding.root.tag = resource?.fileName
        val row = itemBinding.formRow
        val name = itemBinding.name
        val organization = itemBinding.organization
        val dlOpenButton = itemBinding.dlOpenButton
        val pinnedIcon = itemBinding.favoritesButton
        //val rowLayout = itemBinding.rowLayout
        //val updateButton = itemBinding.laterButton
        if (resource != null) {
            dlOpenButton.alpha = 1F
            name.text = resource.fileName
            organization.text = resource.project

            pinnedIcon.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.pdf_file_24px,
                    null
                )
            )

            if (!isDownloaded) {
                dlOpenButton.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.ic_download,
                        null
                    )
                )

                dlOpenButton.contentDescription = getString(R.string.action_download)

                dlOpenButton.setOnClickListener {
                    if (MyApplication.isConnectedToInternet(requireContext())) {
                        dlOpenButton.hide()
                        model.downloadResource(resource)
                    } else {
                        DialogUtils.showBottomMessage(
                            baseActivity,
                            getString(R.string.collect_blank_toast_not_connected),
                            true
                        )
                    }
                }
            } else {
                dlOpenButton.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.ic_more,
                        null
                    )
                )

                dlOpenButton.contentDescription =
                    getString(R.string.collect_blank_action_desc_more_options)

                dlOpenButton.setOnClickListener {
                    showDownloadedMenu(
                        resource
                    )
                }

                row.setOnClickListener {
                    model.getPdfFile(resource.fileId)
                }
            }
        }
        return itemBinding.root
    }

    private fun showDownloadedMenu(resource: Resource) {
        BottomSheetUtils.showViewDeleteMenuSheet(
            requireActivity().supportFragmentManager,
            resource.title,
            requireContext().getString(R.string.View_Report),
            requireContext().getString(R.string.Resources_RemoveFromDownloads),
            object : BottomSheetUtils.ActionSeleceted {
                override fun accept(action: BottomSheetUtils.Action) {
                    if (action === BottomSheetUtils.Action.VIEW) {
                        model.getPdfFile(resource.fileId)
                    }
                    if (action === BottomSheetUtils.Action.DELETE) {
                        model.removeResource(resource)
                        deletedResource(resource)
                        //refreshLists()
                    }
                }
            },
            requireContext().getString(R.string.Resources_RemoveFromDownloads),
            String.format(
                requireContext().resources.getString(R.string.Collect_Subtitle_RemoveForm),
                resource.title
            ),
            requireContext().getString(R.string.action_remove),
            requireContext().getString(R.string.action_cancel)
        )
    }

    private fun refreshLists() {
        model.listResources()
        model.getResources()
    }

    private fun openPdf(
        vaultFile: VaultFile
    ) {
        val intent = Intent(baseActivity, PDFReaderActivity::class.java).apply {
            putExtra(VIEW_PDF, vaultFile)
            putExtra(HIDE_MENU, true)
        }
        startActivity(intent)
    }

    private fun initView() {
        binding.toolbar.backClickListener = { nav().popBackStack() }
        binding.toolbar.onRightClickListener = { refreshLists() }
    }
}