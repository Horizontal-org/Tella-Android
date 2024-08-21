package org.hzontal.shared_ui.veiw_pager_component

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.hzontal.shared_ui.R
import org.hzontal.shared_ui.veiw_pager_component.adapter.ViewPagerAdapter

class ViewPagerComponent @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val tabLayout: TabLayout
    private val viewPager: ViewPager2
    private val viewPagerAdapter: ViewPagerAdapter
    private var tabTitles: List<String> = emptyList()


    init {
        LayoutInflater.from(context).inflate(R.layout.view_pager_component_layout, this, true)
        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)

        viewPagerAdapter = ViewPagerAdapter((context as FragmentActivity), mutableListOf())
        viewPager.adapter = viewPagerAdapter

        context.theme.obtainStyledAttributes(attrs, R.styleable.ViewPagerComponent, 0, 0).apply {
            try {
                val titlesResId = getResourceId(R.styleable.ViewPagerComponent_tabTitles, 0)
                if (titlesResId != 0) {
                    tabTitles = resources.getStringArray(titlesResId).toList()
                }
            } finally {
                recycle()
            }
        }
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = getTabTitle(position)
        }.attach()
    }

    fun setupTabs(fragments: List<Fragment>) {
        viewPagerAdapter.updateFragments(fragments)
        updateTabTitles()
    }

    fun setTabTitles(titles: List<String>) {
        tabTitles = titles
        updateTabTitles()
    }

    private fun updateTabTitles() {
        for (i in 0 until tabLayout.tabCount) {
            val tab = tabLayout.getTabAt(i)
            tab?.text = getTabTitle(i)
        }
    }

    private fun getTabTitle(position: Int): String {
        return tabTitles.getOrNull(position) ?: "Tab $position"
    }
}