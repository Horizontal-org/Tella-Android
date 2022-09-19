package rs.readahead.washington.mobile.views.fragment.uwazi.download

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentDownloadedTemplatesBinding
import rs.readahead.washington.mobile.util.StringUtils
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import rs.readahead.washington.mobile.views.fragment.uwazi.download.adapter.TemplateContainerAdapter

class DownloadedTemplatesFragment : BaseFragment() {

    private val viewModel : DownloadedTemplatesViewModel by viewModels()

    private lateinit var binding: FragmentDownloadedTemplatesBinding

    private val templateContainerAdapter by lazy { TemplateContainerAdapter() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initObservers()
        //TODO CLEAR INIT VIEW FROM BASE FRAGMENT
        initView(binding.root)
    }


    override fun initView(view: View) {
      with(binding){
          templatesRecyclerView.apply {
              layoutManager = LinearLayoutManager(context)
              adapter = templateContainerAdapter
          }

          toolbar.backClickListener = {nav().popBackStack()}

          tvDescription.text = StringUtils.fromHtml(getString(R.string.Donwload_Templates_Title_Description))

          toolbar.onRightClickListener = {viewModel.refreshTemplateList()}
      }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDownloadedTemplatesBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun initObservers(){
        with(viewModel){
            templates.observe(viewLifecycleOwner, { list ->
                val result =    list.groupBy { it.serverId }.toMap()
                templateContainerAdapter.setContainers(result)
            })

            progress.observe(viewLifecycleOwner, {
                binding.progressCircular.isVisible = it
            })

            showDeleteSheet.observe(viewLifecycleOwner, {
                BottomSheetUtils.showConfirmDelete(activity.supportFragmentManager,
                it.second.entityRow.name,
                getString(R.string.Uwazi_RemoveTemplate_SheetTitle)
                ) { viewModel.confirmDelete(it.second) }
            })

        }
    }
}