package rs.readahead.washington.mobile.views.base_ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.viewbinding.ViewBinding
import com.tooltip.Tooltip
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.fragment.reports.NavigationManager
import rs.readahead.washington.mobile.views.fragment.reports.di.NavControllerProvider

abstract class BaseBindingDialogFragment<VB : ViewBinding>(
    private val inflate: Inflate<VB>
) : DialogFragment() {

    protected lateinit var baseActivity: BaseActivity
    private var _binding: VB? = null
    private val navigationManager by lazy { NavigationManager(navController, bundle) }
    protected val bundle by lazy { Bundle() }
    private val navController by lazy { NavControllerProvider(this) }

    protected val binding: VB
        get() = _binding ?: throw IllegalStateException("ViewBinding is not initialized.")
    private var rootView: View? = null
    var hasInitializedRootView = false
    private var isViewInitialized: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return _binding?.root ?: inflate.invoke(inflater, container, false).also {
            _binding = it
            rootView = it.root
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!isViewInitialized) {
            isViewInitialized = true
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is BaseActivity) {
            baseActivity = context
        } else {
            throw ClassCastException("$context must inherit from BaseActivity")
        }
    }

    protected fun showToast(message: String) {
        if (isAdded) {
            baseActivity.showToast(message)
        }
    }

    protected open fun nav(): NavController {
        return NavControllerProvider(this).navController
    }

    protected open fun navManager(): NavigationManager {
        return NavigationManager(navController, bundle)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        isViewInitialized = false
    }

}