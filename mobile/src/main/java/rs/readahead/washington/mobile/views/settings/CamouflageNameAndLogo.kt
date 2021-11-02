package rs.readahead.washington.mobile.views.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.bus.event.CamouflageAliasChangedEvent
import rs.readahead.washington.mobile.util.CamouflageManager
import rs.readahead.washington.mobile.views.adapters.CamouflageRecycleViewAdapter
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import java.lang.IndexOutOfBoundsException


class CamouflageNameAndLogo : BaseFragment() {

    private val cm = CamouflageManager.getInstance()
    private lateinit var adapter: CamouflageRecycleViewAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_name_and_logo, container, false)

        initView(view)

        return view
    }

    override fun initView(view: View) {
        (activity as OnFragmentSelected?)?.hideAppbar()

        recyclerView = view.findViewById(R.id.iconsRecyclerView)

        adapter = CamouflageRecycleViewAdapter()
        val galleryLayoutManager: RecyclerView.LayoutManager = GridLayoutManager(requireContext(), 3)
        recyclerView.setLayoutManager(galleryLayoutManager)
        recyclerView.setAdapter(adapter)

        adapter.setIcons(cm.options, cm.selectedAliasPosition)

        view.findViewById<View>(R.id.back).setOnClickListener {
            (activity as OnFragmentSelected?)?.showAppbar()
            activity.onBackPressed()
        }

        view.findViewById<View>(R.id.next).setOnClickListener {
            confirmCamouflage(adapter.selectedPosition)
        }
    }

    private fun confirmCamouflage(position: Int) {
        BottomSheetUtils.showConfirmSheetWithImage(
            activity.supportFragmentManager,
            getString(R.string.settings_sec_confirm_camouflage_title),
            getString(R.string.settings_sec_confirm_camouflage_desc, getString(cm.options[position].stringResId)),
            getString(R.string.settings_sec_confirm_exit_tella),
            getString(R.string.action_cancel),
            ContextCompat.getDrawable(activity,cm.options[position].drawableResId),
            consumer = object : BottomSheetUtils.ActionConfirmed {
                override fun accept(isConfirmed: Boolean) {
                    camouflage(position)
                }
            }
        )
    }

    private fun camouflage(position: Int) {
        try {
            val option = cm.options[position]
            if (option != null) {
                if (cm.setLauncherActivityAlias(requireContext(), option.alias)) {
                    MyApplication.bus()
                        .post(CamouflageAliasChangedEvent())
                }
            }
        } catch (ignored: IndexOutOfBoundsException) {
        }
    }
}