package org.hzontal.shared_ui.veiw_pager_component.adapter

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(
    fragmentActivity: FragmentActivity,
    private var fragmentList: MutableList<Fragment>
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = fragmentList.size

    override fun createFragment(position: Int): Fragment = fragmentList[position]

    @SuppressLint("NotifyDataSetChanged")
    fun updateFragments(fragments: List<Fragment>) {
        fragmentList.clear()
        fragmentList.addAll(fragments)
        notifyDataSetChanged()
    }
}
