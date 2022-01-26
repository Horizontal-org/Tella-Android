package rs.readahead.washington.mobile.views.fragment.uwazi.entry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.gson.Gson
import rs.readahead.washington.mobile.databinding.UwaziEntryFragmentBinding
import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityInstance
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityStatus
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import rs.readahead.washington.mobile.views.fragment.vault.attachements.OnNavBckListener

const val COLLECT_TEMPLATE = "collect_template"

class UwaziEntryFragment : BaseFragment(), OnNavBckListener  {
    private val viewModel : UwaziEntryViewModel by viewModels()
    private lateinit var binding: UwaziEntryFragmentBinding
    private var template: CollectTemplate? = null
    private var entityInstance: UwaziEntityInstance = UwaziEntityInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = UwaziEntryFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initObservers()
        initView()
    }

    override fun initView(view: View) {
        with(binding){
            updated.text = entityInstance.updated.toString()
            toolbar.backClickListener = {nav().popBackStack()}
            toolbar.onRightClickListener = {
                entityInstance.status = UwaziEntityStatus.DRAFT
                viewModel.saveEntityInstance(entityInstance)}
        }
    }

    private fun initView() {
        arguments?.let {
            template = Gson().fromJson(it.getString(COLLECT_TEMPLATE), CollectTemplate::class.java)
            entityInstance.collectTemplate = template

        }
    }

    private fun initObservers(){
        with(viewModel){
            template.observe(viewLifecycleOwner,{
                val template = it
            })

            progress.observe(viewLifecycleOwner, {
                binding.progressCircular.isVisible = it
            })

            instance.observe(viewLifecycleOwner,{
                entityInstance = it
                setUpdated(it.updated)
            })
        }
    }

    override fun onBackPressed(): Boolean {
        return  nav().popBackStack()
    }


    private fun setUpdated(updatedTime : Long){
        binding.updated.text = updatedTime.toString()
    }
}