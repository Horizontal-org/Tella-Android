package org.horizontal.tella.mobile.views.fragment.vault.adapters.connections

import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.domain.entity.ServerType
import org.horizontal.tella.mobile.views.fragment.vault.adapters.VaultClickListener
import org.horizontal.tella.mobile.views.fragment.vault.adapters.viewholders.base.BaseViewHolder
import org.horizontal.tella.mobile.views.fragment.vault.adapters.viewholders.base.inflate

class ServerViewHolder(val view: View) : BaseViewHolder<ServerDataItem>(view) {
    private lateinit var reportTypeTextView: TextView
    private lateinit var reportTypeImg: ImageView
    private lateinit var twoLinesReportTypeTextView: TextView


    override fun bind(item: ServerDataItem, vaultClickListener: VaultClickListener) {
        reportTypeTextView = view.findViewById(R.id.server_name_textView)
        twoLinesReportTypeTextView = view.findViewById(R.id.two_line_server_name_textView)
        reportTypeImg = view.findViewById(R.id.server_img)

        // Set the default padding
       // val defaultPadding = view.context.resources.getDimensionPixelSize(R.dimen.hide_tella_small_margin)
      //  view.setPadding(view.paddingLeft, defaultPadding, view.paddingRight, view.paddingBottom)

        when (item.type) {
            ServerType.UWAZI -> {
                reportTypeTextView.text = view.context.getString(R.string.Home_BottomNav_Uwazi)
                reportTypeImg.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        view.resources,
                        R.drawable.ic_uwazi_small,
                        null
                    )
                )
            }
            ServerType.TELLA_UPLOAD -> {
                reportTypeTextView.text = view.context.getText(R.string.Home_BottomNav_Reports)
                reportTypeImg.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        view.resources,
                        R.drawable.baseline_assignment_24,
                        null
                    )
                )
            }
            ServerType.TELLA_RESORCES -> {
                reportTypeTextView.text = view.context.getText(R.string.Home_BottomNav_Resources)
                reportTypeImg.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        view.resources,
                        R.drawable.resource_info,
                        null
                    )
                )
            }
            ServerType.ODK_COLLECT -> {
                reportTypeTextView.text = view.context.getText(R.string.Home_BottomNav_Forms)
                reportTypeImg.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        view.resources,
                        R.drawable.ic_list_numbered_24,
                        null
                    )
                )
            }
            ServerType.GOOGLE_DRIVE -> {
                reportTypeTextView.text = view.context.getText(R.string.google_drive)
                reportTypeImg.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        view.resources,
                        R.drawable.ic_google_drive_white,
                        null
                    )
                )
            }
            ServerType.DROP_BOX -> {
                reportTypeTextView.text = view.context.getString(R.string.dropbox)
                reportTypeImg.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        view.resources,
                        R.drawable.ic_dropbox_small,
                        null
                    )
                )
            }
            ServerType.NEXTCLOUD -> {
                reportTypeTextView.text = view.context.getString(R.string.NextCloud)
                reportTypeImg.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        view.resources,
                        R.drawable.ic_nextcloud_small,
                        null
                    )
                )
            }
            ServerType.PEERTOPEER -> {
                twoLinesReportTypeTextView.text = view.context.getText(R.string.NearBySharing)
                reportTypeImg.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        view.resources,
                        R.drawable.ic_share,
                        null
                    )
                )
            }
            else -> { // todo create default server type
            }
        }

        view.setOnClickListener {
            vaultClickListener.onServerItemClickListener(item)
        }
    }

    companion object {
        fun from(parent: ViewGroup): ServerViewHolder {
            return ServerViewHolder(parent.inflate(R.layout.item_home_vault_server))
        }
    }
}