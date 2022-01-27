package rs.readahead.washington.mobile.views.adapters.uwazi

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.fragment.uwazi.adapters.ViewEntityTemplateItem

class UwaziTemplatesAdapter : RecyclerView.Adapter<UwaziTemplatesAdapter.EntityViewHolder>() {

    private var templates: MutableList<ViewEntityTemplateItem> = ArrayList()


    @SuppressLint("NotifyDataSetChanged")
    fun setEntityTemplates(templates : List<ViewEntityTemplateItem>){
        this.templates = templates.toMutableList()
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntityViewHolder {
        return EntityViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.templates_uwazi_row, parent, false))
    }

    override fun onBindViewHolder(holder: EntityViewHolder, position: Int) {
        holder.bind(entityRow = templates[position])
    }

    inner class EntityViewHolder(val view : View) : RecyclerView.ViewHolder(view) {
        fun bind(entityRow: ViewEntityTemplateItem) {
            view.apply{
                view.findViewById<TextView>(R.id.name).text = entityRow.templateName
                view.findViewById<TextView>(R.id.organization).text = entityRow.serverName
                view.findViewById<ImageButton>(R.id.popupMenu).setOnClickListener { entityRow.onMoreClicked() }
                view.findViewById<AppCompatImageView>(R.id.favorites_button).apply {
                    setImageDrawable(
                        if (entityRow.isFavorite) {
                            ResourcesCompat.getDrawable(resources,R.drawable.star_filled_24dp,null)
                        } else {
                            ResourcesCompat.getDrawable(resources,R.drawable.star_border_24dp,null)
                        }
                    )

                    setOnClickListener { entityRow.onFavoriteClicked() }
                }
            }
        }
    }

    override fun getItemCount() = templates.size



}