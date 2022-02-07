package rs.readahead.washington.mobile.views.fragment.uwazi.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import rs.readahead.washington.mobile.R

class UwaziDraftsAdapter : RecyclerView.Adapter<UwaziDraftsAdapter.EntityViewHolder>() {

    private var drafts: MutableList<ViewEntityInstanceItem> = ArrayList()


    @SuppressLint("NotifyDataSetChanged")
    fun setEntityDrafts(drafts : List<ViewEntityInstanceItem>){
        this.drafts = drafts.toMutableList()
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntityViewHolder {
        return EntityViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.draft_uwazi_row, parent, false))
    }

    override fun onBindViewHolder(holder: EntityViewHolder, position: Int) {
        holder.bind(entityRow = drafts[position])
    }

    inner class EntityViewHolder(val view : View) : RecyclerView.ViewHolder(view) {
        fun bind(entityRow: ViewEntityInstanceItem) {
            view.apply{
                view.findViewById<TextView>(R.id.name).text = entityRow.instanceName
                view.findViewById<TextView>(R.id.organization).text = entityRow.serverName
                view.findViewById<ImageButton>(R.id.popupMenu).setOnClickListener { entityRow.onMoreClicked() }
                setOnClickListener { entityRow.onOpenClicked() }
            }
        }
    }

    override fun getItemCount() = drafts.size



}