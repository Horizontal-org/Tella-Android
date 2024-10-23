package rs.readahead.washington.mobile.views.fragment.reports.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.hzontal.shared_ui.submission.SubmittedItem
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.SubmittedCollectFormInstanceRowBinding
import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.util.Util
import rs.readahead.washington.mobile.util.ViewUtil
import rs.readahead.washington.mobile.views.adapters.uwazi.VIEW_TYPE_HEADER
import rs.readahead.washington.mobile.views.adapters.uwazi.VIEW_TYPE_LIST
import rs.readahead.washington.mobile.views.fragment.uwazi.adapters.EntityMessageViewHolder

class EntityAdapter :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var entities: List<ViewEntityTemplateItem> = ArrayList()
    private var descriptionText: String = ""

    @SuppressLint("NotifyDataSetChanged")
    fun setEntities(entities: List<ViewEntityTemplateItem>, descriptionText: String) {
        this.entities = entities
        this.descriptionText = descriptionText
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return if (descriptionText.isNotEmpty()) entities.size + 1 else entities.size
    }

    override fun getItemViewType(position: Int): Int {
        // Return header view type if the position is 0 and descriptionText is not empty
        return if (descriptionText.isNotEmpty() && position == 0) {
            VIEW_TYPE_HEADER
        } else {
            VIEW_TYPE_LIST
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            EntityMessageViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.templates_uwazi_message_row, parent, false)
            )
        } else {
            val binding = SubmittedCollectFormInstanceRowBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            EntityViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == VIEW_TYPE_HEADER) {
            // Bind description text in the header view
            (holder as EntityMessageViewHolder).bind(message = descriptionText)
        } else {
            // Adjust position if the header is present (description is not empty)
            val adjustedPosition = if (descriptionText.isNotEmpty()) position - 1 else position
            (holder as EntityViewHolder).bind(entities[adjustedPosition])
        }
    }

    inner class EntityViewHolder(private val binding: SubmittedCollectFormInstanceRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entityRow: ViewEntityTemplateItem) {
            with(binding.submittedItem) {
                setName(entityRow.title)
                setDates(entityRow.updated)
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
            drawableResId?.let { drawableId ->
                val tintedDrawable = ViewUtil.getTintedDrawable(
                    context, drawableId,
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

        private fun setDates(timestamp: Long) {
            val elapsedTime = Util.getElapsedTimeFromTimestamp(timestamp, itemView.context)
            val updatedText =
                itemView.context.getString(R.string.Modified_Label) + " " + elapsedTime.lowercase()
            binding.submittedItem.setUpdated(updatedText)
        }
    }
}
