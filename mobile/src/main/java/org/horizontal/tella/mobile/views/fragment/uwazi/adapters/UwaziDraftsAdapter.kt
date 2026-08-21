package org.horizontal.tella.mobile.views.fragment.uwazi.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.views.adapters.uwazi.VIEW_TYPE_HEADER
import org.horizontal.tella.mobile.views.adapters.uwazi.VIEW_TYPE_LIST

class UwaziDraftsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var drafts: MutableList<Any> = ArrayList()

    @SuppressLint("NotifyDataSetChanged")
    fun setEntityDrafts(drafts : List<Any>){
        this.drafts = drafts.toMutableList()
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER ) EntityMessageViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.templates_uwazi_message_row, parent, false))
        else
            EntityViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.draft_uwazi_row, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position == 0){
            (holder as EntityMessageViewHolder).bind(message = drafts[position] as String)
        }else{
            (holder as EntityViewHolder).bind(entityRow = drafts[position] as ViewEntityInstanceItem)
        }
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

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_LIST
    }

}