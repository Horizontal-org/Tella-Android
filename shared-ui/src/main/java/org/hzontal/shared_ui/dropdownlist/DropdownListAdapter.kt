package org.hzontal.shared_ui.dropdownlist

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.proxym.shared.widget.dropdown_list.CustomDropdownItemClickListener
import org.hzontal.shared_ui.R


class DropdownListAdapter(
    private val testList: List<DropDownItem>,
    private var itemClickListener: CustomDropdownItemClickListener,
    private var context: Context,
) : RecyclerView.Adapter<DropdownListAdapter.ViewHolder>() {
    private var checkedPosition = -1

    override fun getItemCount(): Int {
        return testList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.layout_item_dropdown,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindViewHolder(testList[position])
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName: TextView = view.findViewById(R.id.country_name_tv)

        init {
            itemView.setOnClickListener {

                if (checkedPosition != adapterPosition) {
                    notifyItemChanged(checkedPosition)
                    checkedPosition = adapterPosition
                }
                tvName.setBackgroundResource(R.color.dark_purple)
                tvName.setTextColor(ContextCompat.getColor(context, R.color.wa_white))

                itemClickListener.onDropDownItemClicked(adapterPosition, testList[adapterPosition])
            }
        }

        fun bindViewHolder(item: DropDownItem) {
            if (checkedPosition == adapterPosition) {
                tvName.setBackgroundResource(R.color.wa_white_15)
            } else {
                tvName.setBackgroundResource(R.color.wa_white_8)
            }
            tvName.text = item.name
        }


    }
}