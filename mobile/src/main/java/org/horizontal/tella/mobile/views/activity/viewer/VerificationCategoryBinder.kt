package org.horizontal.tella.mobile.views.activity.viewer

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import org.horizontal.tella.mobile.R

fun interface VerificationCategoryClickListener {
    fun onCategory(category: VerificationCategory)
}

object VerificationCategoryBinder {

    @JvmStatic
    fun bind(root: View, onCategory: VerificationCategoryClickListener) {
        bindRow(
            root.findViewById(R.id.verification_category_file),
            R.drawable.ic_folder_24px,
            R.string.verification_info_subheading_file_metadata,
            VerificationCategory.FILE,
            onCategory
        )
        bindRow(
            root.findViewById(R.id.verification_category_device),
            R.drawable.ic_smartphone_white_24dp,
            R.string.verification_info_subheading_device_metadata,
            VerificationCategory.DEVICE,
            onCategory
        )
        bindRow(
            root.findViewById(R.id.verification_category_network),
            R.drawable.ic_wifi_24px,
            R.string.verification_info_subheading_network_metadata,
            VerificationCategory.NETWORK,
            onCategory
        )
        bindRow(
            root.findViewById(R.id.verification_category_location),
            R.drawable.ic_gps_fixed_white_24dp,
            R.string.verification_info_subheading_location_metadata,
            VerificationCategory.LOCATION,
            onCategory
        )
        bindRow(
            root.findViewById(R.id.verification_category_other),
            R.drawable.ic_info_24px,
            R.string.verification_info_subheading_other_metadata,
            VerificationCategory.OTHER,
            onCategory
        )
    }

    private fun bindRow(
        row: View,
        iconRes: Int,
        labelRes: Int,
        category: VerificationCategory,
        onCategory: VerificationCategoryClickListener
    ) {
        row.findViewById<ImageView>(R.id.category_icon).setImageResource(iconRes)
        row.findViewById<TextView>(R.id.category_label).setText(labelRes)
        row.setOnClickListener { onCategory.onCategory(category) }
    }
}
