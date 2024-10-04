package org.hzontal.shared_ui.veiw_pager_component

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.hzontal.shared_ui.R
import org.hzontal.shared_ui.appbar.ToolbarComponent
import org.hzontal.shared_ui.veiw_pager_component.adapter.ViewPagerAdapter
import org.hzontal.shared_ui.veiw_pager_component.fragments.FragmentProvider

class ViewPagerComponent @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val tabLayout: TabLayout
    private val viewPager: ViewPager2
    private val toolBarTextView: TextView
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private val toolBar: ToolbarComponent
    private var tabTitles: List<String> = emptyList()
    private var fragmentProvider: FragmentProvider? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_pager_component_layout, this, true)
        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)
        toolBarTextView = findViewById(R.id.toolbar_textView)
        toolBar = findViewById(R.id.toolbar)


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
    }

    fun initViewPager(fm: FragmentManager, lifecycle: Lifecycle, pageLimit: Int) {
        viewPagerAdapter = ViewPagerAdapter(fm, lifecycle)
        viewPager.apply {
            offscreenPageLimit = pageLimit
            isSaveEnabled = true
            adapter = viewPagerAdapter
        }

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = getTabTitle(position)
        }.attach()
    }

    fun setupTabs(fragmentProvider: FragmentProvider, tabCount: Int) {
        this.fragmentProvider = fragmentProvider
        val fragments = List(tabCount) { fragmentProvider.createFragment(it) }
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

    fun setToolBarTitle(title: String) {
        toolBarTextView.text = title
    }

    fun setOnToolbarBackClickListener(onBackClick: (() -> Unit)? = null) {
        toolBar.backClickListener = {
            onBackClick?.invoke()  // Call the passed lambda function if it's not null
        }
    }

    fun getViewPager(): ViewPager2 {
        return viewPager
    }
}