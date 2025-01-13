package org.hzontal.shared_ui.veiw_pager_component.adapter

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(fm: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fm, lifecycle) {

    private val fragmentList = mutableListOf<Fragment>()
    private val fragmentIds = mutableListOf<Long>()

    override fun getItemCount(): Int = fragmentList.size

    override fun createFragment(position: Int): Fragment = fragmentList[position]

    // Returns a unique item ID for the fragment at the given position
    override fun getItemId(position: Int): Long {
        return fragmentIds[position]
    }

    // Checks whether the fragment with the given ID exists
    override fun containsItem(itemId: Long): Boolean {
        return fragmentIds.contains(itemId)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateFragments(fragments: List<Fragment>) {
        fragmentList.clear()
        fragmentList.addAll(fragments)

        // Update fragment IDs, ensuring each fragment has a unique ID
        fragmentIds.clear()
        fragmentIds.addAll(fragments.map { it.hashCode().toLong() })

        // Instead of notifyDataSetChanged(), the adapter now uses itemId to handle fragment updates
        notifyDataSetChanged() // Not strictly necessary anymore, but harmless to keep
    }
}
