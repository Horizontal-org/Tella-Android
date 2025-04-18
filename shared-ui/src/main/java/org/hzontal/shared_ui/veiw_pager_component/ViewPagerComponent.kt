package org.hzontal.shared_ui.veiw_pager_component

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import org.hzontal.shared_ui.R
import org.hzontal.shared_ui.appbar.ToolbarComponent
import org.hzontal.shared_ui.textviews.CenterMessageTextView
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
    private var textViewEmpty: CenterMessageTextView

    init {
        LayoutInflater.from(context).inflate(R.layout.view_pager_component_layout, this, true)
        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)
        toolBarTextView = findViewById(R.id.toolbar_textView)
        toolBar = findViewById(R.id.toolbar)
        textViewEmpty = findViewById(R.id.textView_empty)

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
            tab.customView = getTabTitleView(position, 0)
        }.attach()


        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                updateTabTextColor(tab, true)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                updateTabTextColor(tab, false)
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                // No action needed
            }
        })
    }

    fun setupTabs(fragmentProvider: FragmentProvider, tabCount: Int) {
        this.fragmentProvider = fragmentProvider
        val fragments = List(tabCount) { fragmentProvider.createFragment(it) }
        viewPagerAdapter.updateFragments(fragments)
    }

    fun setTabTitles(titles: List<String>) {
        tabTitles = titles
    }

    private fun getTabTitleView(position: Int, count: Int): View {
        val tabView = LayoutInflater.from(context).inflate(R.layout.tabs_title_layout, null)
        val tabTitleTextView = tabView.findViewById<TextView>(R.id.tab_title)
        val tabNumberTextView = tabView.findViewById<TextView>(R.id.tab_number)

        tabTitleTextView.text = tabTitles.getOrNull(position) ?: "Tab $position"
        tabNumberTextView.text = if (count > 0) " ($count)" else ""
        return tabView
    }

    fun updateTabTitle(position: Int, count: Int) {
        val tab = tabLayout.getTabAt(position)
        tab?.customView?.let { customView ->
            // Update the existing custom view directly
            val tabTitleTextView = customView.findViewById<TextView>(R.id.tab_title)
            val tabNumberTextView = customView.findViewById<TextView>(R.id.tab_number)

            // Update the title and count text
            tabTitleTextView.text = tabTitles.getOrNull(position) ?: "Tab $position"
            tabNumberTextView.text = if (count > 0) " ($count)" else ""
        }
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

    fun setCenterMessageImg(description: String, img: Int) {
        textViewEmpty.setText(description)
        textViewEmpty.setTopIcon(img)
    }

    fun setEmptyTextViewMessageVisibility(isVisible: Boolean) {
        if (isVisible) {
            textViewEmpty.visibility = View.VISIBLE
        } else {
            textViewEmpty.visibility = View.GONE
        }
    }


    private fun updateTabTextColor(tab: TabLayout.Tab, isSelected: Boolean) {
        tab.customView?.let { customView ->
            val tabTitleTextView = customView.findViewById<TextView>(R.id.tab_title)

            val selectedColor = ContextCompat.getColor(context, R.color.wa_white)
            val unselectedColor = ContextCompat.getColor(context,R.color.wa_white_50)

            // Update text color based on selection state
            tabTitleTextView.setTextColor(if (isSelected) selectedColor else unselectedColor)
        }
    }

}