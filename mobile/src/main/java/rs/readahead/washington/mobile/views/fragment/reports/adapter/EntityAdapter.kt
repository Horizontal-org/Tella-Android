package rs.readahead.washington.mobile.views.fragment.reports.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.hzontal.shared_ui.submission.SubmittedItem
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.util.Util
import rs.readahead.washington.mobile.util.ViewUtil

class EntityAdapter :
    RecyclerView.Adapter<EntityAdapter.EntityViewHolder>() {

    private var entities: List<Any> = ArrayList()


    fun setEntities(submitted: List<Any>) {
        this.entities = submitted
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
        holder.bind(entityRow = entities[position] as ViewEntityTemplateItem)
    }

    inner class EntityViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        private val submittedItem: SubmittedItem = itemView.findViewById(R.id.submittedItem)

        fun bind(entityRow: ViewEntityTemplateItem) {
            with(submittedItem) {
                setName(entityRow.title)
                setDates(entityRow.updated, context)
                setOrganization(null)
                setIconByStatus(entityRow.status)
                setOnClickListener { entityRow.onOpenEntityClicked() }
                popClickListener = { entityRow.onMoreClicked() }
            }
        }

        private fun SubmittedItem.setIconByStatus(status: EntityStatus) {
            val drawableResId = when (status) {
                EntityStatus.SUBMITTED -> R.drawable.ic_check_circle
                EntityStatus.SUBMISSION_ERROR -> R.drawable.ic_error
                EntityStatus.FINALIZED,
                EntityStatus.SUBMISSION_PENDING,
                EntityStatus.SUBMISSION_PARTIAL_PARTS -> R.drawable.ic_watch_later_orange_24dp
                else -> null
            }
            drawableResId?.let { drawbleId ->
                val tintedDrawable = ViewUtil.getTintedDrawable(
                    context, drawbleId,
                    getIconTintByStatus(status)
                )
                setIconDrawable(tintedDrawable)
            } ?: setIconDrawable(null)
        }

        private fun getIconTintByStatus(status: EntityStatus): Int {
            return when (status) {
                EntityStatus.SUBMITTED -> R.color.wa_green
                EntityStatus.SUBMISSION_ERROR -> R.color.wa_red
                EntityStatus.FINALIZED,
                EntityStatus.SUBMISSION_PENDING,
                EntityStatus.SUBMISSION_PARTIAL_PARTS -> R.color.dark_orange
                else -> -1 // Provide a default color if needed
            }
        }

        private fun setDates(timestamp: Long, context: Context) {
            val elapsedTime = Util.getElapsedTimeFromTimestamp(timestamp, context)
            val updatedText = context.getString(R.string.Modified_Label) + " " + elapsedTime.lowercase()
            submittedItem.setUpdated(updatedText)
        }


    }

    override fun getItemCount() = entities.size

}