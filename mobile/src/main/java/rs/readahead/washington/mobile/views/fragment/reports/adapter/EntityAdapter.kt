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

class EntityAdapter :
    RecyclerView.Adapter<EntityAdapter.EntityViewHolder>() {

    private var entities: List<ViewEntityTemplateItem> = ArrayList()

    @SuppressLint("NotifyDataSetChanged")
    fun setEntities(entities: List<ViewEntityTemplateItem>) {
        this.entities = entities
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): EntityViewHolder {
        val binding = SubmittedCollectFormInstanceRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EntityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EntityViewHolder, position: Int) {
        holder.bind(entities[position])
    }

    override fun getItemCount() = entities.size

    inner class EntityViewHolder(private val binding: SubmittedCollectFormInstanceRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entityRow: ViewEntityTemplateItem) {
            with(binding) {
                submittedItem.apply {
                    setName(entityRow.title)
                    setDates(entityRow.updated)
                    setOrganization(null)
                    setIconByStatus(entityRow.status)
                    setOnClickListener { entityRow.onOpenEntityClicked() }
                    popClickListener = { entityRow.onMoreClicked() }
                }
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
