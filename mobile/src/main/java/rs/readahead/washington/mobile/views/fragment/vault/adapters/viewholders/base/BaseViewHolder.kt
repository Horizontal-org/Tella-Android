package rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView

abstract class BaseViewHolder<in Model>(itemView: View) : RecyclerView.ViewHolder(itemView) {
    protected val mContext: Context = itemView.context

    abstract fun bind(item: Model, vararg args: Any)
}
