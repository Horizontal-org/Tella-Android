package rs.readahead.washington.mobile.views.fragment.uwazi.download

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import rs.readahead.washington.mobile.databinding.FragmentDownloadedTemplatesBinding
import rs.readahead.washington.mobile.views.fragment.uwazi.download.adapter.TemplateContainerAdapter

class DownloadedTemplatesFragment : Fragment() {


    private val viewModel : DownloadedTemplatesViewModel by viewModels()

    private lateinit var binding: FragmentDownloadedTemplatesBinding

    private val templateContainerAdapter by lazy { TemplateContainerAdapter() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getServers()
        initObservers()
        initView()
    }

    private fun initView() {
      with(binding){
          templatesRecyclerView.apply {
              layoutManager = LinearLayoutManager(context)
              adapter = templateContainerAdapter
          }
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

            onTemplateDownloaded.observe(viewLifecycleOwner, { list ->

            })
        }
    }
}