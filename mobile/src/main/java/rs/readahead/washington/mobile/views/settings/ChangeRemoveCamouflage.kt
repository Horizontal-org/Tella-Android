package rs.readahead.washington.mobile.views.settings

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.bus.event.CamouflageAliasChangedEvent
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.util.CamouflageManager
import rs.readahead.washington.mobile.views.base_ui.BaseFragment


class ChangeRemoveCamouflage : BaseFragment() {

    private val cm = CamouflageManager.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_change_remove_camouflage, container, false)

        initView(view)

        return view
    }

    override fun initView(view: View) {
        (baseActivity as OnFragmentSelected?)?.showAppbar()
        (baseActivity as OnFragmentSelected?)?.setToolbarLabel(R.string.settings_servers_hide_tella_title)

        val fragmentTitle = view.findViewById<TextView>(R.id.title)
        fragmentTitle.text = getString(
            R.string.settings_servers_add_camouflage_subtitle,
            cm.getLauncherName(requireContext())
        )

        val camoImage = view.findViewById<ImageView>(R.id.server_icon)
        if (getCamoImage() != null) {
            camoImage.setImageDrawable(getCamoImage())
            camoImage.requestLayout()
            val dimension = resources.displayMetrics.density * 160
            camoImage.layoutParams.height = dimension.toInt()
            camoImage.layoutParams.width = dimension.toInt()
            camoImage.scaleType = ImageView.ScaleType.FIT_XY
        } else {
            camoImage.setImageDrawable(ContextCompat.getDrawable(baseActivity, R.drawable.ic_server))
        }

        view.findViewById<View>(R.id.change_method).setOnClickListener {
            baseActivity.addFragment(HideTella(), R.id.my_nav_host_fragment)
        }

        view.findViewById<View>(R.id.remove_camouflage).setOnClickListener {
            showRemovingCamouflage()
        }
    }

    private fun removeCamouflage() {
        if (cm.setDefaultLauncherActivityAlias(requireContext())) {
            MyApplication.bus().post(CamouflageAliasChangedEvent())
        }
    }

    private fun showRemovingCamouflage() {
        BottomSheetUtils.showWarningSheetWithImageAndTimeout(
            baseActivity.supportFragmentManager,
            getString(R.string.SettingsCamo_Dialog_RemovingCamouflage),
            getString(R.string.SettingsCamo_Dialog_TimeoutDesc),
            getCamoImage(),
            consumer = object : BottomSheetUtils.ActionConfirmed {
                override fun accept(isConfirmed: Boolean) {
                    removeCamouflage()
                }
            }
        )
    }

    private fun getCamoImage(): Drawable? {
        return ContextCompat.getDrawable(baseActivity,R.mipmap.tella_icon)
    }
}