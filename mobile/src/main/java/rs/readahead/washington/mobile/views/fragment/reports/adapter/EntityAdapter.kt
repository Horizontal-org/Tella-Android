package rs.readahead.washington.mobile.views.fragment.reports.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.hzontal.shared_ui.submission.SubmittedItem
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.util.Util
import rs.readahead.washington.mobile.util.ViewUtil

class EntityAdapter : RecyclerView.Adapter<EntityAdapter.EntityViewHolder>() {

    private var submitted: MutableList<Any> = ArrayList()

    @SuppressLint("NotifyDataSetChanged")
    fun setEntities(submitted: List<Any>) {
        this.submitted = submitted.toMutableList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): EntityAdapter.EntityViewHolder {
        return EntityViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.submitted_collect_form_instance_row, parent, false)
        )
    }

    override fun onBindViewHolder(holder: EntityAdapter.EntityViewHolder, position: Int) {
        holder.bind(entityRow = submitted[position] as ViewEntityTemplateItem)
    }

    inner class EntityViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        private lateinit var submittedItem: SubmittedItem

        fun bind(entityRow: ViewEntityTemplateItem) {
            submittedItem = view.findViewById(R.id.submittedItem)
            submittedItem.apply {
                setName(entityRow.title)
                setDates(entityRow.updated)
                setOrganization(null)
                if (entityRow.status == EntityStatus.SUBMITTED) {
                    setSubmittedIcon()
                } else if (entityRow.status == EntityStatus.SUBMISSION_ERROR) {
                    setSubmitErrorIcon()
                } else if (entityRow.status == EntityStatus.FINALIZED || entityRow.status == EntityStatus.SUBMISSION_PENDING || entityRow.status == EntityStatus.SUBMISSION_PARTIAL_PARTS) {
                    setPendingIcon()
                }else {
                    submittedItem.setIconDrawable(null)
                }
                setOnClickListener { entityRow.onOpenEntityClicked() }
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
                R.drawable.ic_watch_later_black_24dp,
                R.color.dark_orange
            )
            if (drawable != null) {
                submittedItem.setIconDrawable(drawable)
            }
        }
    }

    override fun getItemCount() = submitted.size

}