package com.proxym.shared.widget.dropdown_list

import org.hzontal.shared_ui.dropdownlist.DropDownItem

interface CustomDropdownItemClickListener {
    fun onDropDownItemClicked(position: Int, chosenItem: DropDownItem)
}