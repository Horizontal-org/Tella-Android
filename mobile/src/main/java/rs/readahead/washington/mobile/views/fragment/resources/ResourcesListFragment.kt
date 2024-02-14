package rs.readahead.washington.mobile.views.fragment.resources

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.BlankCollectFormRowBinding
import rs.readahead.washington.mobile.databinding.FragmentResourcesListBinding
import rs.readahead.washington.mobile.domain.entity.reports.ResourceTemplate
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.views.base_ui.BaseBindingFragment

@AndroidEntryPoint
class ResourcesListFragment :
    BaseBindingFragment<FragmentResourcesListBinding>(FragmentResourcesListBinding::inflate) {

    private val model: ResourcesViewModel by viewModels()
    private lateinit var selectedServer: TellaReportServer

    private var availableResources = arrayListOf<ResourceTemplate>()
    private var downloadedResources: MutableList<ResourceTemplate>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!hasInitializedRootView) {
            hasInitializedRootView = true
            initView()
        }
        initObservers()
    }

    override fun onResume() {
        super.onResume()
        model.getResources()
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
            serversList.observe(viewLifecycleOwner, {
            })
        }
    }

    private fun onAvailableResourcesList(listFormResult: List<ResourceTemplate>) {
        availableResources.addAll(listFormResult)
        updateResourcesViews()
    }

    private fun updateResourcesViews() {
        binding.blankResources.removeAllViews()
        createResourcesViews(availableResources, binding.blankResources)
    }

    private fun createResourcesViews(resources: List<ResourceTemplate>, listView: LinearLayout) {
        for (resource in resources) {
            val view = getResourceItem(resource)
            listView.addView(view, resources.indexOf(resource))
        }
    }

    private fun getResourceItem(resource: ResourceTemplate?): View {
        val itemBinding =
            BlankCollectFormRowBinding.inflate(LayoutInflater.from(context), binding.resources, false)
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
            dlOpenButton.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.ic_download,
                    null
                )
            )
            dlOpenButton.setOnClickListener { view: View? ->
                if (MyApplication.isConnectedToInternet(requireContext())) {
                    model.downloadResource(resource)
                } else {
                    DialogUtils.showBottomMessage(
                        baseActivity,
                        getString(R.string.collect_blank_toast_not_connected),
                        true
                    )
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
    private fun initView() {
        binding.toolbar.backClickListener = { nav().popBackStack() }
        /* binding.resourcesRecyclerView.apply {
             layoutManager = LinearLayoutManager(baseActivity)
             // adapter =
         }*/
    }
}