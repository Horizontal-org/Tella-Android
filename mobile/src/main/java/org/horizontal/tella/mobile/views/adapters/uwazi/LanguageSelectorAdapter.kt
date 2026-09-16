package org.horizontal.tella.mobile.views.adapters.uwazi

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.databinding.ItemLanguageSelectorBinding


class LanguageSelectorAdapter  : RecyclerView.Adapter<LanguageSelectorAdapter.LanguageSelectorViewHolder>() {

    private var lastSelectedPosition = -1
    private var languages: MutableList<ViewLanguageItem> = ArrayList()

    @SuppressLint("NotifyDataSetChanged")
    fun setLanguages(languages: List<ViewLanguageItem>){
        this.languages = languages.toMutableList()
        if (languages.isNotEmpty() && lastSelectedPosition < 0) {
            lastSelectedPosition = 0
            languages.first().onLanguageClicked()
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageSelectorViewHolder{
        return LanguageSelectorViewHolder(
            ItemLanguageSelectorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun getItemCount(): Int = languages.size


    override fun onBindViewHolder(holder: LanguageSelectorViewHolder, position: Int) {
        holder.setLanguage(languages[position])
        holder.changeBackground(lastSelectedPosition == position)
    }


    inner class LanguageSelectorViewHolder (val binding : ItemLanguageSelectorBinding) : RecyclerView.ViewHolder(binding.root)  {

         fun setLanguage(item: ViewLanguageItem) {
            with(binding){
                tvLanguageBig.text = item.languageBigText
                tvLanguageSmall.text = item.languageSmallText
                root.setOnClickListener {
                    lastSelectedPosition = adapterPosition
                    item.onLanguageClicked()
                    notifyDataSetChanged()
                }
            }
        }

        fun changeBackground(isChecked : Boolean){
            binding.imgCheck.isVisible = isChecked
            binding.root.setBackgroundColor(
                ContextCompat.getColor(
                    binding.root.context,
                    if (isChecked) R.color.delimiter else android.R.color.transparent
                )
            )
        }
    }
}
