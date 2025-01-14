package org.horizontal.tella.mobile.views.fragment.uwazi.download.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class TemplateContainerAdapter : RecyclerView.Adapter<TemplateContainerViewHolder>() {

    private var containers: MutableMap<Long,List<ViewTemplateItem>> = HashMap()

    @SuppressLint("NotifyDataSetChanged")
    fun setContainers(containers : Map<Long,List<ViewTemplateItem>>){
        this.containers = containers.toMutableMap()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemplateContainerViewHolder {
        return TemplateContainerViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: TemplateContainerViewHolder, position: Int) {
       val key = (ArrayList(containers.keys))[position]

        containers[key]?.let { holder.setTemplateContainer(it) }
    }

    override fun getItemCount() = containers.size

}