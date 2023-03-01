package rs.readahead.washington.mobile.views.base_ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.viewbinding.ViewBinding
import com.tooltip.Tooltip
import rs.readahead.washington.mobile.R
import timber.log.Timber


typealias Inflate<T> = (LayoutInflater, ViewGroup?, Boolean) -> T

abstract class BaseBindingFragment<VB : ViewBinding>(
    private val inflate: Inflate<VB>
) : Fragment() {

    protected lateinit var baseActivity: BaseActivity
    private var _binding: VB? = null
    val binding get() = _binding
    private var rootView: View? = null
    var hasInitializedRootView = false

    private fun getPersistentView(inflater: LayoutInflater, container: ViewGroup?): View? {
        if (binding == null) {
            // Inflate the layout for this fragment
            _binding = inflate.invoke(inflater, container, false)
            rootView = binding?.root


        } else {
            // Do not inflate the layout again.
            // The returned View of onCreateView will be added into the fragment.
            // However it is not allowed to be added twice even if the parent is same.
            // So we must remove rootView from the existing parent view group
            // (it will be added back).
            (rootView?.parent as? ViewGroup)?.removeView(rootView)
        }

        return rootView
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return getPersistentView(inflater, container)
    }

    override fun onAttach(context: Context) {
        Timber.d("***** ${this.javaClass.name} onAttach")

        super.onAttach(context)

        baseActivity = context as BaseActivity
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

}
