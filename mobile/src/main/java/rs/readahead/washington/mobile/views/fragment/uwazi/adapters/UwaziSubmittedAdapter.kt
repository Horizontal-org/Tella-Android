package rs.readahead.washington.mobile.views.fragment.uwazi.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.SubmittedCollectFormInstanceRowBinding
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityStatus
import rs.readahead.washington.mobile.util.Util
import rs.readahead.washington.mobile.util.ViewUtil

class UwaziSubmittedAdapter : RecyclerView.Adapter<UwaziSubmittedAdapter.EntityViewHolder>() {

    private var submitted: MutableList<ViewEntityInstanceItem> = ArrayList()


    @SuppressLint("NotifyDataSetChanged")
    fun setEntities(drafts: List<ViewEntityInstanceItem>) {
        this.submitted = drafts.toMutableList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntityViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = SubmittedCollectFormInstanceRowBinding.inflate(layoutInflater, parent, false)
        return EntityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EntityViewHolder, position: Int) {
        holder.bind(entityRow = submitted[position])
    }

    inner class EntityViewHolder(val view: SubmittedCollectFormInstanceRowBinding) :
        RecyclerView.ViewHolder(view.root) {

        fun bind(entityRow: ViewEntityInstanceItem) {
            view.apply {
                submittedItem.setName(entityRow.instanceName)
                submittedItem.setOrganization(entityRow.translatedTemplateName)
                if (entityRow.status == UwaziEntityStatus.SUBMITTED) {
                    setDates(entityRow.updated)
                    setSubmittedIcon()
                } else if (entityRow.status == UwaziEntityStatus.SUBMISSION_ERROR) {
                    setSubmitErrorIcon()
                } else if (entityRow.status == UwaziEntityStatus.FINALIZED || entityRow.status == UwaziEntityStatus.SUBMISSION_PENDING || entityRow.status == UwaziEntityStatus.SUBMISSION_PARTIAL_PARTS) {
                    setPendingIcon()
                }
                submittedItem.setOnClickListener { entityRow.onOpenClicked() }
                submittedItem.popClickListener = { entityRow.onMoreClicked() }

            }
        }

        private fun setDates(timestamp: Long) {
            view.submittedItem.setUpdated(
                Util.getElapsedTimeFromTimestamp(
                    timestamp,
                    view.submittedItem.context
                )
            )
        }

        private fun setSubmittedIcon() {
            val drawable = ViewUtil.getTintedDrawable(
                view.submittedItem.context,
                R.drawable.ic_check_circle,
                R.color.wa_green
            )
            if (drawable != null) {
                view.submittedItem.setIconDrawable(drawable)
            }
        }

        private fun setSubmitErrorIcon() {
            val drawable =
                ViewUtil.getTintedDrawable(
                    view.submittedItem.context,
                    R.drawable.ic_error,
                    R.color.wa_red
                )

            if (drawable != null) {
                view.submittedItem.setIconDrawable(drawable)
            }
        }

        private fun setPendingIcon() {
            val drawable = ViewUtil.getTintedDrawable(
                view.submittedItem.context,
                R.drawable.ic_watch_later_black_24dp,
                R.color.dark_orange
            )
            if (drawable != null) {
                view.submittedItem.setIconDrawable(drawable)
            }
        }
    }


    override fun getItemCount() = submitted.size


}