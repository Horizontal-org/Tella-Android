package org.horizontal.tella.mobile.views.fragment.uwazi.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.hzontal.shared_ui.submission.SubmittedItem
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.domain.entity.EntityStatus
import org.horizontal.tella.mobile.util.Util
import org.horizontal.tella.mobile.util.ViewUtil
import org.horizontal.tella.mobile.views.adapters.uwazi.VIEW_TYPE_HEADER
import org.horizontal.tella.mobile.views.adapters.uwazi.VIEW_TYPE_LIST

class UwaziSubmittedAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var submitted: MutableList<Any> = ArrayList()


    @SuppressLint("NotifyDataSetChanged")
    fun setEntities(submitted: List<Any>) {
        this.submitted = submitted.toMutableList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            EntityMessageViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.templates_uwazi_message_row, parent, false)
            )
        } else {
            EntityViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.submitted_collect_form_instance_row, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position == 0) {
            (holder as EntityMessageViewHolder).bind(message = submitted[position] as String)
        } else {
            (holder as EntityViewHolder).bind(entityRow = submitted[position] as ViewEntityInstanceItem)
        }
    }

    inner class EntityViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        private lateinit var submittedItem: SubmittedItem

        fun bind(entityRow: ViewEntityInstanceItem) {
            submittedItem = view.findViewById(R.id.submittedItem)
            submittedItem.apply {
                setName(entityRow.instanceName)
                setOrganization(entityRow.translatedTemplateName)
                when (entityRow.status) {
                    EntityStatus.SUBMITTED -> {
                        setDates(entityRow.updated)
                        setSubmittedIcon()
                    }
                    EntityStatus.SUBMISSION_ERROR -> {
                        setSubmitErrorIcon()
                    }
                    EntityStatus.FINALIZED, EntityStatus.SUBMISSION_PENDING, EntityStatus.SUBMISSION_PARTIAL_PARTS -> {
                        setPendingIcon()
                    }
                    else -> {}
                }
                setOnClickListener { entityRow.onOpenClicked() }
                popClickListener = { entityRow.onMoreClicked() }

            }
        }

        private fun setDates(timestamp: Long) {
            submittedItem.setUpdated(
                Util.getElapsedTimeFromTimestamp(
                    timestamp,
                    submittedItem.context
                )
            )
        }

        private fun setSubmittedIcon() {
            val drawable = ViewUtil.getTintedDrawable(
                submittedItem.context,
                R.drawable.ic_check_circle,
                R.color.wa_green
            )
            if (drawable != null) {
                submittedItem.setIconDrawable(drawable)
            }
        }

        private fun setSubmitErrorIcon() {
            val drawable =
                ViewUtil.getTintedDrawable(
                    submittedItem.context,
                    R.drawable.ic_error,
                    R.color.wa_red
                )

            if (drawable != null) {
                submittedItem.setIconDrawable(drawable)
            }
        }

        private fun setPendingIcon() {
            val drawable = ViewUtil.getTintedDrawable(
                submittedItem.context,
                R.drawable.ic_watch_later_orange_24dp,
                R.color.dark_orange
            )
            if (drawable != null) {
                submittedItem.setIconDrawable(drawable)
            }
        }
    }

    override fun getItemCount() = submitted.size

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_LIST
    }

}