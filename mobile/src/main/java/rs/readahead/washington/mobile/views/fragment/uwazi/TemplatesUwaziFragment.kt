package rs.readahead.washington.mobile.views.fragment.uwazi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import rs.readahead.washington.mobile.databinding.FragmentTemplatesUwaziBinding
import rs.readahead.washington.mobile.views.adapters.UwaziTemplatesAdapter


class TemplatesUwaziFragment : UwaziListFragment() {
    private val viewModel : SharedUwaziViewModel by viewModels()
    private val uwaziTemplatesAdapter : UwaziTemplatesAdapter by lazy { UwaziTemplatesAdapter() }
    private lateinit var binding: FragmentTemplatesUwaziBinding

    override fun getFormListType(): Type {
        return Type.TEMPLATES
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getTemplates()
        initObservers()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTemplatesUwaziBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun getTemplates(){
      viewModel.getTemplates()
    }

    private fun initObservers(){
        viewModel.templates.observe(this, {
            uwaziTemplatesAdapter.submitList(it)
        })
    }


}