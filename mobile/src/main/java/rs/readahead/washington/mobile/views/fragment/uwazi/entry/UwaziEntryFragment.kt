package rs.readahead.washington.mobile.views.fragment.uwazi.entry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment
import com.google.gson.Gson
import com.google.gson.JsonArray
import org.json.JSONObject
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.UwaziEntryFragmentBinding
import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityInstance
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityStatus
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import rs.readahead.washington.mobile.views.fragment.uwazi.send.SEND_ENTITY
import rs.readahead.washington.mobile.views.fragment.vault.attachements.OnNavBckListener


const val COLLECT_TEMPLATE = "collect_template"

class UwaziEntryFragment : BaseFragment(), OnNavBckListener {
    private val viewModel: UwaziEntryViewModel by viewModels()
    private lateinit var binding: UwaziEntryFragmentBinding
    private var metadata = JSONObject()
    private var template: CollectTemplate? = null
    private var entityInstance: UwaziEntityInstance? = null
    private val bundle by lazy { Bundle() }
    private var screenView: ViewGroup? = null
    private var entryPrompts : ArrayList<UwaziEntryPrompt> = ArrayList()
    private lateinit var uwaziFormView : UwaziFormView

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
        with(binding) {

            //updated.text = entityInstance?.updated.toString()

            toolbar.backClickListener = { nav().popBackStack() }
            toolbar.onRightClickListener = {
                entityInstance?.status = UwaziEntityStatus.DRAFT
                entityInstance?.let { viewModel.saveEntityInstance(it) }
            }

            nextBtn.setOnClickListener { sendEntity() }

            screenView = binding.screenFormView
        }
    }

    private fun initView() {
        arguments?.let {
            template = Gson().fromJson(it.getString(COLLECT_TEMPLATE), CollectTemplate::class.java)
            entityInstance?.collectTemplate = template
            parseUwaziForm()
        }
    }

    private fun initObservers() {
        with(viewModel) {
            template.observe(viewLifecycleOwner, {
                val template = it
            })

            progress.observe(viewLifecycleOwner, {
                binding.progressCircular.isVisible = it
            })

            instance.observe(viewLifecycleOwner, {
                entityInstance = it
                setUpdated(it.updated)
            })
        }
    }

    override fun onBackPressed(): Boolean {
        return nav().popBackStack()
    }

    private fun setUpdated(updatedTime: Long) {
       // binding.updated.text = updatedTime.toString()
    }

    private fun sendEntity() {
        val gsonTemplate = Gson().toJson(metadata)
        bundle.putString(SEND_ENTITY, gsonTemplate)
        NavHostFragment.findNavController(this@UwaziEntryFragment)
            .navigate(R.id.action_uwaziEntryScreen_to_uwaziSendScreen, bundle)
    }

    private fun parseUwaziForm(){
        var metadataArr = JsonArray()
        metadata.put("template", template?.entityRow?._id )

        val entryDoc = UwaziEntryPrompt("123456789", "123456789", "media", "Primary document", true, "Attach primary document")
        entryPrompts.add(entryDoc)

        for(property in template?.entityRow?.properties!!){
            val obj = JSONObject()
            val mArr = JsonArray()
            mArr.add("111")
            obj.put( property.name, mArr.toString())
            metadataArr.add(obj.toString())
            val entryPrompt = UwaziEntryPrompt(property._id, property.id, property.type, property.name, property.required, property.label)
            entryPrompts.add(entryPrompt);
        }
        metadata.put("metadata",metadataArr)

        val arr : Array<UwaziEntryPrompt?> = arrayOfNulls<UwaziEntryPrompt>(entryPrompts.size)
        for (i in 0 until arr.size) {
            arr[i] = entryPrompts[i]
        }
        uwaziFormView = UwaziFormView(requireContext(),arr)
        screenView?.addView(uwaziFormView)
    }
}