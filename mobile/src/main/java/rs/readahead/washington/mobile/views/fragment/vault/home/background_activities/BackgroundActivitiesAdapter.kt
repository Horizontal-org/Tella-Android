package rs.readahead.washington.mobile.views.fragment.vault.home.background_activities

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import rs.readahead.washington.mobile.databinding.ItemBackgroundActivityBinding
import rs.readahead.washington.mobile.domain.entity.background_activity.BackgroundActivityModel

class BackgroundActivitiesAdapter(private val dataList: List<BackgroundActivityModel>) :
    RecyclerView.Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemBackgroundActivityBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataList[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }
}