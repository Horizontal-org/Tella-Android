package rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.fragment.vault.adapters.ImproveClickOptions
import rs.readahead.washington.mobile.views.fragment.vault.adapters.VaultClickListener
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.BaseViewHolder
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.inflate

class ImproveViewHolder(val view: View) : BaseViewHolder<String?>(view) {

    override fun bind(item: String?, vaultClickListener: VaultClickListener) {
        view.apply {
            val imgClose = findViewById<ImageView>(R.id.img_close)
            val tvYesIn = findViewById<TextView>(R.id.tv_yes_in)
            val tvLearnMore = findViewById<TextView>(R.id.tv_learn_more)
            imgClose.setOnClickListener {
                vaultClickListener.onImproveItemClickListener(ImproveClickOptions.CLOSE)
            }
            tvYesIn.setOnClickListener {
                vaultClickListener.onImproveItemClickListener(ImproveClickOptions.YES)
            }
            tvLearnMore.setOnClickListener {
                vaultClickListener.onImproveItemClickListener(ImproveClickOptions.LEARN_MORE)
            }
        }
    }

    companion object {
        fun from(parent: ViewGroup): ImproveViewHolder {
            return ImproveViewHolder(parent.inflate(R.layout.item_vault_insights_approve))
        }
    }
}