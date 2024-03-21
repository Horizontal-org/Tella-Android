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
        reportTypeImg = view.findViewById(R.id.server_img)

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