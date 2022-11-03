package rs.readahead.washington.mobile.views.fragment.vault.adapters.connections

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.domain.entity.ServerType
import rs.readahead.washington.mobile.views.fragment.vault.adapters.VaultClickListener
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.BaseViewHolder
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.inflate

class ServerViewHolder(val view: View) : BaseViewHolder<ServerDataItem>(view) {
    private lateinit var reportTypeTextView: TextView
    private lateinit var reportTypeImg: ImageView

    override fun bind(item: ServerDataItem, vaultClickListener: VaultClickListener) {

        reportTypeTextView = view.findViewById(R.id.server_name_textView)
        reportTypeImg = view.findViewById(R.id.report_img)

        when (item.type) {
            ServerType.UWAZI -> {
                reportTypeTextView.text = view.context.getText(R.string.Home_BottomNav_Uwazi)
                reportTypeImg.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        view.resources,
                        R.drawable.ic_uwazi,
                        null
                    )
                )
            }
            ServerType.TELLA_UPLOAD -> {
                reportTypeTextView.text = view.context.getText(R.string.Home_BottomNav_Reports)
                reportTypeImg.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        view.resources,
                        R.drawable.ic_reports,
                        null
                    )
                )
            }
            ServerType.ODK_COLLECT -> {
                reportTypeTextView.text = view.context.getText(R.string.Home_BottomNav_Forms)
                reportTypeImg.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        view.resources,
                        R.drawable.ic_forms,
                        null
                    )
                )
            }
            else -> { //todo create default server type
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