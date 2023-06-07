package rs.readahead.washington.mobile.views.fragment.vault.attachements.helpers

import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.core.view.updatePadding
import org.hzontal.shared_ui.pinview.ResourceUtils
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.FragmentVaultAttachmentsBinding
import rs.readahead.washington.mobile.util.changeStatusColor
import rs.readahead.washington.mobile.util.setMargins
import rs.readahead.washington.mobile.views.activity.MainActivity
import rs.readahead.washington.mobile.views.base_ui.BaseActivity

class MoveModeUIUpdater(private var binding: FragmentVaultAttachmentsBinding) {

    fun updateUI(enable: Boolean, baseActivity : BaseActivity) {
        val theme = if (enable) R.style.AppTheme_DarkNoActionBar_Blue else R.style.AppTheme_DarkNoActionBar
        val color = if (enable) R.color.prussian_blue else R.color.space_cadet
        val colorDrawable = ColorDrawable(ResourceUtils.getColor(baseActivity, color))

        (baseActivity as MainActivity).setTheme(theme)

        binding.apply {
            toolbar.background = colorDrawable
            rootView.background = colorDrawable
            appbar.background = colorDrawable
            moveContainer.visibility = if (enable) View.VISIBLE else View.GONE
            checkBoxList.visibility = if (enable) View.GONE else View.VISIBLE
            fabButton.visibility = if (enable) View.GONE else View.VISIBLE
            fabMoveButton.visibility = if (enable) View.VISIBLE else View.GONE
            attachmentsRecyclerView.apply {
                val margin = if (enable) 17 else 0
                val padding = if (enable) 2 else 0
                val bg = if (enable) R.color.wa_white_12 else R.color.space_cadet
                setMargins(margin, 0, margin, if (enable) 37 else 17)
                updatePadding(right = padding, left = padding)
                background = ColorDrawable(ResourceUtils.getColor(baseActivity, bg))
            }
        }

        baseActivity.supportActionBar?.setBackgroundDrawable(colorDrawable)
        baseActivity.window.changeStatusColor(baseActivity, color)
        baseActivity.invalidateOptionsMenu()
    }
}