package org.horizontal.tella.mobile.views.dialog.uwazi.step2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.FragmentLoginTypeBinding
import org.horizontal.tella.mobile.domain.entity.UWaziUploadServer
import org.horizontal.tella.mobile.views.base_ui.BaseFragment
import org.horizontal.tella.mobile.views.dialog.ID_KEY
import org.horizontal.tella.mobile.views.dialog.IS_UPDATE_SERVER
import org.horizontal.tella.mobile.views.dialog.OBJECT_KEY
import org.horizontal.tella.mobile.views.dialog.TITLE_KEY
import org.horizontal.tella.mobile.views.dialog.uwazi.step3.LoginFragment
import org.horizontal.tella.mobile.views.dialog.uwazi.step5.LanguageFragment

internal const val URL_KEY = "uk"
class LoginTypeFragment : BaseFragment() {
    private var validated = false
    private var loginAsPublicInstance = false
    private lateinit var binding: FragmentLoginTypeBinding
    private lateinit var server: UWaziUploadServer
    private var isUpdate = false

    companion object{
        val TAG = LoginTypeFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(server: UWaziUploadServer,isUpdate :Boolean): LoginTypeFragment {
            val frag = LoginTypeFragment()
            val args = Bundle()
                args.putInt(TITLE_KEY, R.string.settings_docu_dialog_title_server_settings)
                args.putSerializable(ID_KEY, server.id)
                args.putString(OBJECT_KEY, Gson().toJson(server))
                args.putBoolean(IS_UPDATE_SERVER,isUpdate)
            frag.arguments = args
            return frag
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginTypeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
    }

    override fun initView(view: View) {
        arguments?.getString(OBJECT_KEY)?.let {
            server = Gson().fromJson(it,UWaziUploadServer::class.java)
        }
        arguments?.getBoolean(IS_UPDATE_SERVER)?.let {
            isUpdate = it
        }
    }

    private fun initListeners() {
        with(binding) {
            backBtn.setOnClickListener {
               baseActivity.supportFragmentManager.popBackStack()
            }
            nextBtn.setOnClickListener {
                if(validated&&loginAsPublicInstance){
                    baseActivity.addFragment(LanguageFragment.newInstance(server,isUpdate),R.id.container)
                }else if (validated && !loginAsPublicInstance){
                    baseActivity.addFragment(LoginFragment.newInstance(server,isUpdate),R.id.container)
                }
            }

            publicButton.setOnClickListener {
                publicButton.isChecked = true
                loginButton.isChecked = false
                validated = true
                loginAsPublicInstance = true
            }

            loginButton.setOnClickListener {
                loginButton.isChecked = true
                publicButton.isChecked = false
                validated = true
                loginAsPublicInstance = false
            }
        }
    }
}