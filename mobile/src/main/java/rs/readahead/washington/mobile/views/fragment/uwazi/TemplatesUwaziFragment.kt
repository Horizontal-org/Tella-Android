package rs.readahead.washington.mobile.views.fragment.uwazi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.ActionSeleceted
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils.showEditDeleteMenuSheet
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentTemplatesUwaziBinding
import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate
import rs.readahead.washington.mobile.views.adapters.uwazi.UwaziTemplatesAdapter
import rs.readahead.washington.mobile.views.fragment.uwazi.entry.COLLECT_TEMPLATE


class TemplatesUwaziFragment : UwaziListFragment() {
    private val viewModel : SharedUwaziViewModel by viewModels()
    private val uwaziTemplatesAdapter : UwaziTemplatesAdapter by lazy { UwaziTemplatesAdapter() }
    private  var binding: FragmentTemplatesUwaziBinding? = null
    private val bundle by lazy { Bundle() }

    override fun getFormListType(): Type {
        return Type.TEMPLATES
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTemplatesUwaziBinding.inflate(inflater, container, false)
        return binding?.root!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initObservers()
    }

    private fun initObservers(){
        with(viewModel){
            templates.observe(viewLifecycleOwner,{
                if (it.isEmpty()){
                    binding?.textViewEmpty?.isVisible = true
                }else{
                    binding?.textViewEmpty?.isVisible = false
                    uwaziTemplatesAdapter.setEntityTemplates(it)
                }

            })

            progress.observe(viewLifecycleOwner,{
                binding?.progressCircular?.isVisible = it
            })

            showSheetMore.observe(viewLifecycleOwner,{
                showDownloadedMenu(it)
            })
        }
    }

    private fun initView(){
        binding?.templatesRecyclerView?.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = uwaziTemplatesAdapter
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.listTemplates()
    }

    private fun showDownloadedMenu(template: CollectTemplate) {
        showEditDeleteMenuSheet(
            requireActivity().supportFragmentManager,
            template.entityRow.name,
            getString(R.string.Uwazi_Action_FillEntity),
            getString(R.string.Uwazi_Action_RemoveTemplate),
            object : ActionSeleceted {
                override fun accept(action: BottomSheetUtils.Action) {
                    if (action === BottomSheetUtils.Action.EDIT) {
                        val gsonTemplate = Gson().toJson(template)
                        bundle.putString(COLLECT_TEMPLATE, gsonTemplate)
                        NavHostFragment.findNavController(this@TemplatesUwaziFragment).navigate(R.id.action_uwaziScreen_to_uwaziEntryScreen, bundle)
                    }
                    if (action === BottomSheetUtils.Action.DELETE) {
                        viewModel.confirmDelete(template)
                    }
                }
            },
            getString(R.string.action_delete) + " \""+ template.entityRow.name+ "\"?",
            requireContext().resources.getString(R.string.Uwazi_Subtitle_RemoveTemplate),
            requireContext().getString(R.string.action_remove),
            requireContext().getString(R.string.action_cancel)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.templatesRecyclerView?.adapter = null
        binding = null
    }


}