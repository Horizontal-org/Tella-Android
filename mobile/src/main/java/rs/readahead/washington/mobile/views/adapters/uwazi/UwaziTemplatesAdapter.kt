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

const val VIEW_TYPE_HEADER = 0
const val VIEW_TYPE_LIST = 1
class UwaziTemplatesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var templates: MutableList<Any> = ArrayList()

    @SuppressLint("NotifyDataSetChanged")
    fun setEntityTemplates(templates : List<Any>){
        this.templates = templates.toMutableList()
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER ) EntityMessageViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.templates_uwazi_message_row, parent, false))
        else
            EntityViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.templates_uwazi_row, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder , position: Int) {
        if (position == 0){
            (holder as EntityMessageViewHolder ).bind(message = templates[0] as Int)
        }else{
            (holder as EntityViewHolder ).bind(entityRow = templates[position] as ViewEntityTemplateItem)
        }
    }

    override fun getItemViewType(position: Int): Int {
      return if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_LIST
    }

    inner class EntityViewHolder(val view : View) : RecyclerView.ViewHolder(view) {
        fun bind(entityRow: ViewEntityTemplateItem) {
            view.apply{
                view.findViewById<TextView>(R.id.name).text = entityRow.translatedTemplateName
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
                    contentDescription = context.getString(
                        if (entityRow.isFavorite) R.string.action_unfavorite else R.string.action_favorite
                    )
                    setOnClickListener { entityRow.onFavoriteClicked() }
                }
                setOnClickListener { entityRow.onOpenEntityClicked() }
            }
        }
    }

    inner class EntityMessageViewHolder(val view : View) : RecyclerView.ViewHolder(view) {
        fun bind(message: Int) {
            view.apply{
                view.findViewById<TextView>(R.id.message_textView).text = context.getString(message)
            }
        }
    }

    override fun getItemCount() = templates.size



}