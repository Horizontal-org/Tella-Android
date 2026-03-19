package org.horizontal.tella.mobile.views.base_ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.tooltip.Tooltip
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.util.NavigationManager
import org.horizontal.tella.mobile.util.setupForAccessibility
import org.horizontal.tella.mobile.views.fragment.reports.di.NavControllerProvider
import timber.log.Timber

abstract class BaseFragment : Fragment() {

    protected lateinit var baseActivity: BaseActivity
    private val navigationManager by lazy { NavigationManager(navController,bundle) }
    protected val bundle by lazy { Bundle() }
    private val navController by lazy { NavControllerProvider(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        childFragmentManager.setupForAccessibility(requireContext())
    }

    override fun onAttach(context: Context) {
        Timber.d("***** ${this.javaClass.name} onAttach")

        super.onAttach(context)

        baseActivity = context as BaseActivity
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        Timber.d("***** ${this.javaClass.name} onCreateView")

        view?.findViewById<View>(R.id.appbar)?.outlineProvider = null

        return super.onCreateView(inflater, container, savedInstanceState)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Timber.d("***** ${this.javaClass.name} onViewCreated")

        super.onViewCreated(view, savedInstanceState)
        
        // Apply edge-to-edge handling to prevent white spaces at top/bottom
        applyEdgeToEdgeIfNeeded(view)
        
        initView(view)
    }

    /**
     * Apply edge-to-edge window insets handling to the root view.
     * Override this method if a fragment needs custom edge-to-edge handling.
     */
    protected open fun applyEdgeToEdgeIfNeeded(view: View) {
        // Apply window insets to the root view to handle edge-to-edge display
        // This prevents white spaces at the top and bottom where system bars are
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    protected fun showToast(message: String) {
        if (isAdded) {
            baseActivity.showToast(message)
        }
    }

    protected open fun nav(): NavController {
        return NavHostFragment.findNavController(this)
    }

    protected fun showTooltip(v: View, text: String, gravity: Int) {
        Tooltip.Builder(v)
                .setText(text)
                .setTextColor(resources.getColor(R.color.wa_black))
                .setBackgroundColor(resources.getColor(R.color.wa_white))
                .setGravity(gravity)
                .setCornerRadius(12f)
                .setPadding(24)
                .setDismissOnClick(true)
                .setCancelable(true)
                .show()
    }

    open fun back() {
        nav().navigateUp()
    }

    protected open fun navManager(): NavigationManager {
        return NavigationManager(navController, bundle)
    }

    abstract fun initView(view: View)
}