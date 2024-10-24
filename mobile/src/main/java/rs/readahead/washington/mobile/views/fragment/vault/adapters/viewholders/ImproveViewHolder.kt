package rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.hzontal.shared_ui.data.CommonPreferences
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.sharedpref.Preferences.isTimeToShowReminderAnalytics
import rs.readahead.washington.mobile.views.fragment.vault.adapters.ImproveClickOptions
import rs.readahead.washington.mobile.views.fragment.vault.adapters.VaultClickListener
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.BaseViewHolder
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.inflate

class ImproveViewHolder(val view: View) : BaseViewHolder<String?>(view) {

    override fun bind(item: String?, vaultClickListener: VaultClickListener) {
        view.apply {
            val tvImprove = findViewById<TextView>(R.id.tv_help_improve)
            val tvDescription = findViewById<TextView>(R.id.tv_help_improve_description)
            //val imgImprove = findViewById<ImageView>(R.id.img_improve)
            val imgClose = findViewById<ImageView>(R.id.img_close)
            val tvYesIn = findViewById<TextView>(R.id.tv_yes_in)
            val tvLearnMore = findViewById<TextView>(R.id.tv_learn_more)

            if (isTimeToShowReminderAnalytics()) {
                tvImprove.text = tvImprove.context.getString(R.string.Analytics_contribute_reminder_title)
                tvDescription.text = tvDescription.context.getString(R.string.Analytics_contribute_reminder_description)
                //imgImprove.setImageResource(R.drawable.ic_insights_share_data)
                imgClose.visibility = View.INVISIBLE
                tvYesIn.text = tvYesIn.context.getString(R.string.action_ok)
                tvLearnMore.text = tvLearnMore.context.getString(R.string.settings_app_bar)
            }

            imgClose.setOnClickListener { vaultClickListener.onImproveItemClickListener(ImproveClickOptions.CLOSE) }
            tvYesIn.setOnClickListener { vaultClickListener.onImproveItemClickListener(ImproveClickOptions.YES) }
            tvLearnMore.setOnClickListener {
                if (tvLearnMore.text == tvLearnMore.context.getString(R.string.settings_app_bar))
                    vaultClickListener.onImproveItemClickListener(ImproveClickOptions.SETTINGS)
                else vaultClickListener.onImproveItemClickListener(ImproveClickOptions.LEARN_MORE)
            }
        }
    }

    companion object {
        fun from(parent: ViewGroup) = ImproveViewHolder(parent.inflate(R.layout.item_vault_insights_approve))
    }
}