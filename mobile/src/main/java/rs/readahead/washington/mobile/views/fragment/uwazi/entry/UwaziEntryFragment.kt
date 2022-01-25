package rs.readahead.washington.mobile.views.fragment.uwazi.entry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.google.gson.Gson
import rs.readahead.washington.mobile.databinding.UwaziEntryFragmentBinding
import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import rs.readahead.washington.mobile.views.fragment.vault.attachements.OnNavBckListener

const val COLLECT_TEMPLATE = "collect_template"

class UwaziEntryFragment : BaseFragment(), OnNavBckListener  {
    private val viewModel : UwaziEntryViewModel by viewModels()
    private lateinit var binding: UwaziEntryFragmentBinding
    private var template: CollectTemplate? = null

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

            toolbar.backClickListener = {nav().popBackStack()}
        }
    }

    private fun initView() {
        arguments?.let {
            template = Gson().fromJson(it.getString(COLLECT_TEMPLATE), CollectTemplate::class.java)
            template?.entityRow?._id?.let { it1 -> viewModel.getBlankTemplate(it1) }

        }
    }

    private fun initObservers(){
        with(viewModel){
            template.observe(viewLifecycleOwner,{
                val template = it
            })
        }
    }

    override fun onBackPressed(): Boolean {
        return  nav().popBackStack()
    }

}