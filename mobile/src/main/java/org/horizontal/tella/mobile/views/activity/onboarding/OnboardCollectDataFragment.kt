package org.horizontal.tella.mobile.views.activity.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.horizontal.tella.mobile.BuildConfig
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.OnboardCollectDataBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment

class OnboardCollectDataFragment :
    BaseBindingFragment<OnboardCollectDataBinding>(OnboardCollectDataBinding::inflate) {

    override fun applyEdgeToEdgeIfNeeded(view: View) {
        OnboardingInsets.applyCarouselSlide(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
    }

    private fun initView(view: View) {
        ViewCompat.setAccessibilityPaneTitle(binding.root, getString(R.string.Onboard_Collect_Data))
        ViewCompat.setAccessibilityHeading(binding.sheetTitle, true)
        ViewCompat.setAccessibilityHeading(binding.connectionsHeading, true)
        bindConnections()
    }

    private fun bindConnections() {
        val connections = buildList {
            if (BuildConfig.ENABLE_DROPBOX) {
                add(R.drawable.ic_onboard_dropbox to R.string.settings_docu_add_server_dialog_select_tella_dropbox)
            }
            if (BuildConfig.ENABLE_GOOGLE_DRIVE) {
                add(R.drawable.ic_onboard_google_drive to R.string.google_drive)
            }
            add(R.drawable.ic_onboard_nextcloud to R.string.NextCloud)
            add(R.drawable.ic_onboard_uwazi to R.string.settings_docu_add_server_dialog_select_tella_uwazi)
            add(R.drawable.ic_onboard_tella_web to R.string.settings_docu_add_server_dialog_select_tella_web)
            add(R.drawable.ic_onboard_odk to R.string.settings_docu_add_server_dialog_select_odk)
        }

        val inflater = LayoutInflater.from(requireContext())
        val rows = connections.chunked(3)
        rows.forEachIndexed { rowIndex, rowItems ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            }
            rowItems.forEachIndexed { columnIndex, (iconRes, labelRes) ->
                val item = inflater.inflate(R.layout.onboard_connection_item, row, false)
                item.layoutParams =
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                val label = getString(labelRes).trim()
                item.findViewById<ImageView>(R.id.connection_icon).setImageResource(iconRes)
                item.findViewById<TextView>(R.id.connection_label).text = label
                applyConnectionItemAccessibility(item, label, rowIndex, columnIndex)
                row.addView(item)
            }
            repeat(3 - rowItems.size) {
                row.addView(
                    View(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    }
                )
            }
            binding.connectionsContainer.addView(row)
        }
        applyConnectionsGridAccessibility(rows.size)
    }

    private fun applyConnectionItemAccessibility(
        item: View,
        label: String,
        rowIndex: Int,
        columnIndex: Int,
    ) {
        item.isClickable = false
        ViewCompat.setAccessibilityDelegate(
            item,
            object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: AccessibilityNodeInfoCompat,
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.contentDescription = label
                    info.isClickable = false
                    info.setCollectionItemInfo(
                        AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain(
                            rowIndex,
                            1,
                            columnIndex,
                            1,
                            false,
                        ),
                    )
                }
            },
        )
    }

    private fun applyConnectionsGridAccessibility(rowCount: Int) {
        binding.connectionsContainer.importantForAccessibility =
            View.IMPORTANT_FOR_ACCESSIBILITY_YES
        ViewCompat.setAccessibilityDelegate(
            binding.connectionsContainer,
            object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: AccessibilityNodeInfoCompat,
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.setCollectionInfo(
                        AccessibilityNodeInfoCompat.CollectionInfoCompat.obtain(
                            rowCount,
                            GRID_COLUMN_COUNT,
                            false,
                            AccessibilityNodeInfoCompat.CollectionInfoCompat.SELECTION_MODE_NONE,
                        ),
                    )
                }
            },
        )
    }

    private companion object {
        const val GRID_COLUMN_COUNT = 3
    }
}
