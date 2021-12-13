package rs.readahead.washington.mobile.views.settings

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
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
        (activity as OnFragmentSelected?)?.showAppbar()
        (activity as OnFragmentSelected?)?.setToolbarLabel(R.string.settings_servers_hide_tella_title)

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
            camoImage.getLayoutParams().height = dimension.toInt()
            camoImage.getLayoutParams().width = dimension.toInt()
            camoImage.setScaleType(ImageView.ScaleType.FIT_XY);
        } else {
            camoImage.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_server))
        }

        view.findViewById<View>(R.id.change_method).setOnClickListener {
            activity.addFragment(HideTella(), R.id.my_nav_host_fragment)
        }

        view.findViewById<View>(R.id.remove_camouflage).setOnClickListener {
            removeCamouflage()
        }
    }

    private fun removeCamouflage() {
        if (cm.setDefaultLauncherActivityAlias(requireContext())) {
            MyApplication.bus().post(CamouflageAliasChangedEvent())
        }
    }

    private fun getCamoImage(): Drawable? {
        var drawable = ContextCompat.getDrawable(activity, R.drawable.ic_server)
        val currentAlias = Preferences.getAppAlias()

        if (currentAlias == cm.calculatorOption.alias) {
            return ContextCompat.getDrawable(activity, cm.calculatorOption.drawableResId)
        }

        for (option in cm.options) {
            if (option.alias == currentAlias) {
                drawable = ContextCompat.getDrawable(activity, option.drawableResId)
            }
        }
        return drawable
    }
}