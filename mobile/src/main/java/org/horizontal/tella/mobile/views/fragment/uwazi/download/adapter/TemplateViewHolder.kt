package org.horizontal.tella.mobile.views.fragment.uwazi.download.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.horizontal.tella.mobile.databinding.ItemDownloadStatusTemplateBinding

class TemplateViewHolder internal constructor(private val binding: ItemDownloadStatusTemplateBinding) : RecyclerView.ViewHolder(binding.root) {

    fun seTemplate(item : ViewTemplateItem){
        with(binding){
            if(item.isDownloaded){
                imgMore.isVisible = true
                imgDownload.isVisible = false
            }else{
                imgMore.isVisible = false
                imgDownload.isVisible = true
            }
            tvTemplateName.text = when {
                item.translatedTemplateName.isEmpty() -> {
                    item.templateName
                }
                else -> item.translatedTemplateName
            }
            imgMore.setOnClickListener { item.onMoreClicked() }
            imgDownload.setOnClickListener { item.onDownloadClicked() }
            imgUpdated.isVisible = (item.isDownloaded && item.isUpdated)
        }
    }

    companion object {
        fun from(parent : ViewGroup) : TemplateViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val binding = ItemDownloadStatusTemplateBinding.inflate(layoutInflater,parent,false)
            return TemplateViewHolder(binding)
        }
    }

}