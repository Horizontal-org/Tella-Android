package rs.readahead.washington.mobile.views.fragment.uwazi.adapters

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import rs.readahead.washington.mobile.R

class EntityMessageViewHolder(val view : View) : RecyclerView.ViewHolder(view) {
        fun bind(message: String) {
            view.apply{
                view.findViewById<TextView>(R.id.message_textView).text = message
            }
        }
    }