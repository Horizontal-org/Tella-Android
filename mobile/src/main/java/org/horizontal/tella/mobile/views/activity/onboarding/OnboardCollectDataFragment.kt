package org.horizontal.tella.mobile.views.activity.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.OnboardCollectDataBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment

class OnboardCollectDataFragment :
    BaseBindingFragment<OnboardCollectDataBinding>(OnboardCollectDataBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
    }

    override fun onResume() {
        super.onResume()
        (baseActivity as OnBoardActivityInterface).enableSwipe(
            isSwipeable = true, isTabLayoutVisible = false
        )
        (baseActivity as OnBoardActivityInterface).showButtons(
            isNextButtonVisible = true, isBackButtonVisible = true
        )
    }

    private fun initView(view: View) {
        ViewCompat.setAccessibilityHeading(binding.sheetTitle, true)
        ViewCompat.setAccessibilityHeading(binding.connectionsHeading, true)
        bindConnections()
    }

    private fun bindConnections() {
        val connections = listOf(
            R.drawable.ic_dropbox_small to R.string.settings_docu_add_server_dialog_select_tella_dropbox,
            R.drawable.ic_google_drive_small to R.string.google_drive,
            R.drawable.ic_nextcloud_small to R.string.NextCloud,
            org.hzontal.shared_ui.R.drawable.ic_uwazi_small to R.string.settings_docu_add_server_dialog_select_tella_uwazi,
            R.drawable.ic_servers_icon to R.string.settings_docu_add_server_dialog_select_tella_web,
            R.drawable.ic_collect_menu to R.string.settings_docu_add_server_dialog_select_odk,
        )

        val inflater = LayoutInflater.from(requireContext())
        connections.chunked(2).forEach { rowItems ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            rowItems.forEach { (iconRes, labelRes) ->
                val item = inflater.inflate(R.layout.onboard_connection_item, row, false)
                item.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                val label = getString(labelRes)
                item.findViewById<ImageView>(R.id.connection_icon).setImageResource(iconRes)
                item.findViewById<TextView>(R.id.connection_label).text = label
                item.contentDescription = label
                row.addView(item)
            }
            if (rowItems.size == 1) {
                row.addView(View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                })
            }
            binding.connectionsContainer.addView(row)
        }
    }
}
