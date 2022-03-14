package rs.readahead.washington.mobile.views.dialog.uwazi.step1

import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import org.hzontal.shared_ui.bottomsheet.KeyboardUtil
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentEnterServerBinding
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import rs.readahead.washington.mobile.views.dialog.ID_KEY
import rs.readahead.washington.mobile.views.dialog.OBJECT_KEY
import rs.readahead.washington.mobile.views.dialog.TITLE_KEY
import rs.readahead.washington.mobile.views.dialog.uwazi.UwaziConnectFlowViewModel
import rs.readahead.washington.mobile.views.dialog.uwazi.step2.LoginTypeFragment
import rs.readahead.washington.mobile.views.dialog.uwazi.step3.LoginFragment

const val IS_UPDATE_SERVER = "is_update_server"

class EnterServerFragment : BaseFragment() {
    private var validated = true
    private lateinit var binding: FragmentEnterServerBinding
    private val viewModel: UwaziConnectFlowViewModel by viewModels()
    private val server by lazy { UWaziUploadServer() }
    private var isUpdate = false
    private var serverUwazi: UWaziUploadServer? = null

    companion object {
        val TAG = EnterServerFragment::class.java.simpleName
        @JvmStatic
        fun newInstance(server: UWaziUploadServer, isUpdate: Boolean): EnterServerFragment {
            val frag = EnterServerFragment()
            val args = Bundle()
            args.putInt(TITLE_KEY, R.string.settings_docu_dialog_title_server_settings)
            args.putSerializable(ID_KEY, server.id)
            args.putString(OBJECT_KEY, Gson().toJson(server))
            args.putBoolean(rs.readahead.washington.mobile.views.dialog.IS_UPDATE_SERVER, isUpdate)
            frag.arguments = args
            return frag
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEnterServerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
        initObservers()
    }

    override fun initView(view: View) {
        arguments?.getBoolean(rs.readahead.washington.mobile.views.dialog.IS_UPDATE_SERVER)?.let {
            isUpdate = it
        }

        arguments?.getString(OBJECT_KEY)?.let {
            serverUwazi = Gson().fromJson(it, UWaziUploadServer::class.java)
        }
        if (serverUwazi != null){
            binding.url.setText(serverUwazi!!.url)
        }
    }

    private fun initListeners() {
        with(binding) {
            backBtn.setOnClickListener {
                activity.finish()
            }
            nextBtn.setOnClickListener {
                if (!MyApplication.isConnectedToInternet(activity)) {
                    DialogUtils.showBottomMessage(
                        activity,
                        getString(R.string.settings_docu_error_no_internet),
                        true
                    )
                } else {
                    validateUrl(url, urlLayout)
                    if (validated) {
                        viewModel.getServerLanguage(url.text.toString())
                    }
                }
            }
        }
    }

    private fun validateUrl(field: EditText, layout: TextInputLayout) {
        validated = true
        var url = field.text.toString()
        layout.error = null
        if (TextUtils.isEmpty(url)) {
            layout.error = getString(R.string.settings_text_empty_field)
            validated = false
        } else {
            url = url.trim { it <= ' ' }
            field.setText(url)
            if (!Patterns.WEB_URL.matcher(url).matches()) {
                layout.error = getString(R.string.settings_docu_error_not_valid_URL)
                validated = false
            }
            server.url = url
            if (serverUwazi!=null){
                serverUwazi?.url = url
            }
        }
    }

    private fun initObservers() {
        with(viewModel) {
            isPublic.observe(viewLifecycleOwner, { isPublicInstance ->
                if (isPublicInstance) {
                    activity.addFragment(
                        LoginTypeFragment.newInstance(if (serverUwazi == null) server else serverUwazi!!, isUpdate),
                        R.id.container
                    )
                } else {
                    activity.addFragment(
                        LoginFragment.newInstance(if (serverUwazi == null) server else serverUwazi!!, isUpdate),
                        R.id.container
                    )
                }
                KeyboardUtil.hideKeyboard(activity)
            })

            progress.observe(viewLifecycleOwner,{
                binding.progressBar.isVisible = it
            })
        }
    }


}