package rs.readahead.washington.mobile.views.fragment.uwazi.download.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import java.util.ArrayList

class TemplateListAdapter : RecyclerView.Adapter<TemplateViewHolder>() {

    private var templates: MutableList<ViewTemplateItem> = ArrayList()

    @SuppressLint("NotifyDataSetChanged")
    fun setTemplates(templates: List<ViewTemplateItem>) {
        this.templates = templates.toMutableList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemplateViewHolder {
        return TemplateViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: TemplateViewHolder, position: Int) {
        val item = templates[position]
        holder.seTemplate(item)
    }

    override fun getItemCount() = templates.size

}