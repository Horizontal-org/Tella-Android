package rs.readahead.washington.mobile.views.base_ui

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.tooltip.Tooltip
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.util.NavigationManager
import rs.readahead.washington.mobile.util.setupForAccessibility
import rs.readahead.washington.mobile.views.fragment.reports.di.NavControllerProvider
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
        initView(view)
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
                .show<Tooltip>()
    }

    open fun back() {
        nav().navigateUp()
    }

    protected open fun navManager(): NavigationManager {
        return NavigationManager(navController, bundle)
    }

    abstract fun initView(view: View)
}