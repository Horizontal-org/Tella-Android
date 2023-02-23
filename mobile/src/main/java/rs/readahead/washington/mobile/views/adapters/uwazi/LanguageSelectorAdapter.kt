package rs.readahead.washington.mobile.views.adapters.uwazi

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.databinding.ItemLanguageSelectorBinding


class LanguageSelectorAdapter  : RecyclerView.Adapter<LanguageSelectorAdapter.LanguageSelectorViewHolder>() {

    private var lastSelectedPosition = -1
    private var languages: MutableList<ViewLanguageItem> = ArrayList()

    @SuppressLint("NotifyDataSetChanged")
    fun setLanguages(languages: List<ViewLanguageItem>){
        this.languages = languages.toMutableList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageSelectorViewHolder{
        return LanguageSelectorViewHolder(ItemLanguageSelectorBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun getItemCount(): Int = languages.size


    override fun onBindViewHolder(holder: LanguageSelectorViewHolder, position: Int) {
        holder.setLanguage(languages[position])
        holder.binding.root
        holder.changeBackground(lastSelectedPosition == position)
    }


    inner class LanguageSelectorViewHolder (val binding : ItemLanguageSelectorBinding) : RecyclerView.ViewHolder(binding.root)  {

         fun setLanguage(item: ViewLanguageItem) {
            with(binding){
                tvLanguageSmall.text = item.languageSmallText
                tvLanguageBig.text = item.languageBigText
                root.setOnClickListener {
                    lastSelectedPosition = adapterPosition
                    item.onLanguageClicked()
                    notifyDataSetChanged()
                }
            }
        }

        fun changeBackground(isChecked : Boolean){
            if (isChecked){
                binding.imgCheck.isVisible = true
                binding.root.setBackgroundColor(binding.root.context.resources.getColor(R.color.wa_white_15))
            }else{
                binding.imgCheck.isVisible = false
                binding.root.setBackgroundColor(binding.root.context.resources.getColor(R.color.wa_white_8))
            }

        }
    }
}