package org.horizontal.tella.mobile.views.fragment.uwazi.download.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.horizontal.tella.mobile.databinding.ItemUwaziCollectContainerBinding

class TemplateContainerViewHolder internal constructor(private val binding: ItemUwaziCollectContainerBinding) : RecyclerView.ViewHolder(binding.root) {

        fun setTemplateContainer(item : List<ViewTemplateItem>){
            with(binding){
                tvServerName.text = item[0].serverName
                val templateListAdapter = TemplateListAdapter()
                templateListAdapter.setTemplates(item)
                templatesRecyclerview.apply {
                    layoutManager = LinearLayoutManager(context)
                    adapter = templateListAdapter
                }
            }
        }
        
        companion object {
            fun from(parent : ViewGroup) : TemplateContainerViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemUwaziCollectContainerBinding.inflate(layoutInflater,parent,false)
                return TemplateContainerViewHolder(binding)
            }
        } 
    }