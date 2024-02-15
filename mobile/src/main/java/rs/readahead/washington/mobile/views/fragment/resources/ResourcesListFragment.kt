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
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.BlankCollectFormRowBinding
import rs.readahead.washington.mobile.databinding.FragmentResourcesListBinding
import rs.readahead.washington.mobile.domain.entity.resources.Resource
import rs.readahead.washington.mobile.util.hide
import rs.readahead.washington.mobile.util.show
import rs.readahead.washington.mobile.views.activity.CollectFormEntryActivity
import rs.readahead.washington.mobile.views.activity.viewer.PDFReaderActivity
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
        model.listResources()
        model.getResources()
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
            serversList.observe(viewLifecycleOwner) {
            }

            savedResources.observe(viewLifecycleOwner) { resources ->
                resources.forEach { downloadedResources.put(it.fileName, it) }
                updateResourcesViews()
            }

            downloadedResource.observe(viewLifecycleOwner) {
                availableResources.remove(it.fileName)
                downloadedResources.put(it.fileName, it)
                updateResourcesViews()
            }

            pdfFile.observe(viewLifecycleOwner) {
                openPdf(it)
            }

            progress.observe(viewLifecycleOwner) {
                showProgress(it)
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
            binding.avaivableResourcesTitle.hide()
        } else {
            binding.avaivableResourcesTitle.show()
        }
        binding.downloadedResources.removeAllViews()
        if (downloadedResources.isEmpty()) {
            binding.downloadedResourcesTitle.hide()
        } else {
            binding.downloadedResourcesTitle.show()
        }
        createResourcesViews(downloadedResources.values.toList(), binding.downloadedResources, true)
    }

    private fun onAvailableResourcesList(listFormResult: List<Resource>) {
        listFormResult.forEach {
            if (!downloadedResources.containsKey(it.fileName)) {
                availableResources.put(it.fileName, it)
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
        val row = itemBinding.formRow
        val name = itemBinding.name
        val organization = itemBinding.organization
        val dlOpenButton = itemBinding.dlOpenButton
        val pinnedIcon = itemBinding.favoritesButton
        val rowLayout = itemBinding.rowLayout
        val updateButton = itemBinding.laterButton
        if (resource != null) {
            name.text = resource.fileName
            organization.text = resource.title

            if (!isDownloaded) {
                dlOpenButton.show()

                dlOpenButton.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.ic_download,
                        null
                    )
                )
                dlOpenButton.setOnClickListener { view: View? ->
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
                dlOpenButton.hide()
                row.setOnClickListener { view: View? ->
                    model.getMediaFile(resource.fileId)
                }
            }

            /*          if (collectForm.isDownloaded) {
                          dlOpenButton.setImageDrawable(
                              ResourcesCompat.getDrawable(
                                  resources,
                                  R.drawable.ic_more,
                                  null
                              )
                          )
                          dlOpenButton.contentDescription =
                              getString(R.string.collect_blank_action_desc_more_options)
                          dlOpenButton.setOnClickListener { view: View? ->
                              showDownloadedMenu(
                                  collectForm
                              )
                          }
                          rowLayout.setOnClickListener { view: View? ->
                              model.getBlankFormDef(
                                  collectForm
                              )
                          }
                          pinnedIcon.setOnClickListener { view: View? ->
                              model.toggleFavorite(collectForm)
                              updateFormViews()
                          }
                          if (collectForm.isUpdated) {
                              pinnedIcon.visibility = View.VISIBLE
                              updateButton.visibility = View.VISIBLE
                              updateButton.setOnClickListener { view: View? ->
                                  if (MyApplication.isConnectedToInternet(requireContext())) {
                                      model.updateBlankFormDef(collectForm)
                                  } else {
                                      DialogUtils.showBottomMessage(
                                          baseActivity,
                                          getString(R.string.collect_blank_toast_not_connected),
                                          true
                                      )
                                  }
                              }
                          } else {
                              updateButton.visibility = View.GONE
                          }
                      } else {
                          pinnedIcon.visibility = View.GONE
                          dlOpenButton.setImageDrawable(
                              ResourcesCompat.getDrawable(
                                  resources,
                                  R.drawable.ic_download,
                                  null
                              )
                          )
                          dlOpenButton.contentDescription =
                              getString(R.string.collect_blank_action_download_form)
                          dlOpenButton.setOnClickListener { view: View? ->
                              if (MyApplication.isConnectedToInternet(requireContext())) {
                                  model.downloadBlankFormDef(collectForm)
                              } else {
                                  DialogUtils.showBottomMessage(
                                      baseActivity,
                                      getString(R.string.collect_blank_toast_not_connected),
                                      true
                                  )
                              }
                          }
                      }
                      if (collectForm.isPinned) {
                          pinnedIcon.setImageDrawable(
                              ResourcesCompat.getDrawable(
                                  resources,
                                  R.drawable.star_filled_24dp,
                                  null
                              )
                          )
                          pinnedIcon.contentDescription = getString(R.string.action_unfavorite)
                      } else {
                          pinnedIcon.setImageDrawable(
                              ResourcesCompat.getDrawable(
                                  resources,
                                  R.drawable.star_border_24dp,
                                  null
                              )
                          )
                          pinnedIcon.contentDescription = getString(R.string.action_favorite)
                      }*/
        }
        return itemBinding.root
    }

    private fun openPdf(
        vaultFile: VaultFile
    ) {
        val intent = Intent(baseActivity, PDFReaderActivity::class.java).apply {
            putExtra(VIEW_PDF, vaultFile)
        }
        startActivity(intent)
    }

    private fun initView() {
        binding.toolbar.backClickListener = { nav().popBackStack() }
        /* binding.resourcesRecyclerView.apply {
             layoutManager = LinearLayoutManager(baseActivity)
             // adapter =
         }*/
    }
}