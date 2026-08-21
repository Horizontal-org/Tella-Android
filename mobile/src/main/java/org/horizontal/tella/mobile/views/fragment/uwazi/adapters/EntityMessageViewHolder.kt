package org.horizontal.tella.mobile.views.fragment.uwazi.adapters

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.horizontal.tella.mobile.R

class EntityMessageViewHolder(val view : View) : RecyclerView.ViewHolder(view) {
        fun bind(message: String) {
            view.apply{
                view.findViewById<TextView>(R.id.message_textView).text = message
            }
        }
    }