package org.hzontal.shared_ui.bottomsheet

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.ColorRes
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.hzontal.shared_ui.R
import java.util.*

class CustomBottomSheetFragment : BottomSheetDialogFragment() {

    @LayoutRes
    private var layoutRes: Int = 0

    private lateinit var manager: FragmentManager
    private val clickers = ArrayList<Pair<Int, () -> Unit>>()
    private var backClickListener: (() -> Unit)? = null

    private var screenTag: String? = null

    @ColorRes
    private var statusBarColor: Int? = null

    @StyleRes
    private var animationStyle: Int? = null

    private var binder: Binder<PageHolder>? = null
    private var pageHolder: PageHolder? = null
    private var isCancellable = false
    private var isTransparent: Boolean = false
    private var isFullscreen: Boolean = false

    /**
     * Called to init LayoutRes with ID layout.
     * Mandatory
     *
     * @param layoutRes ID layout to setContentView on CustomBottomSheetFragment Activity
     * @return Instantiated CustomBottomSheetFragment object
     */
    fun page(@LayoutRes layoutRes: Int): CustomBottomSheetFragment {
        this.layoutRes = layoutRes
        return this
    }

    /**
     * Called to init setStatusBarColor with color Res Id.
     * Mandatory
     *
     * @param statusBarColor ColorRes
     * @return Instantiated CustomBottomSheetFragment object
     */
    fun statusBarColor(@ColorRes statusBarColor: Int): CustomBottomSheetFragment {
        this.statusBarColor = statusBarColor
        return this
    }

    fun animate(@StyleRes animationStyle: Int): CustomBottomSheetFragment {
        this.animationStyle = animationStyle
        return this
    }

    /**
     * Called to init HashSet<Pair></Pair><Integer></Integer>, Clicker>> clickers with IDView and Clicker
     *
     * @param idRes ID View to findViewById on Activity
     * @param clicker OnClickListener
     * @return Instantiated CustomBottomSheetFragment object
     */
    fun click(@IdRes idRes: Int, clicker: () -> Unit): CustomBottomSheetFragment {
        this.clickers.add(Pair(idRes, clicker))
        return this
    }

    /**
     * Called to init PageHolder with a View extends PageHolder Object
     *
     * @param pageHolder CustomView for ButterKnife.bind on Message Activity
     * @param binder Interface for bind CustomView extends PageHolder
     * @param <T> CustomView Object
     * @return Instantiated CustomView
    </T> */

    @Suppress("UNCHECKED_CAST")
    fun <T : PageHolder> holder(pageHolder: T, binder: Binder<T>): CustomBottomSheetFragment {
        this.pageHolder = pageHolder
        this.binder = binder as Binder<PageHolder>
        return this
    }

    /**
     * Called to init an action for @Override BackPressed method
     *
     * @param listener OnClickListener
     * @return Instantiated CustomBottomSheetFragment object
     */
    fun back(listener: () -> Unit): CustomBottomSheetFragment {
        this.backClickListener = listener
        return this
    }

    /**
     * Called to init screenTag for tracking
     *
     * @return Instantiated CustomBottomSheetFragment object
     */
    fun screenTag(screenTag: String): CustomBottomSheetFragment {
        this.screenTag = screenTag
        return this
    }

    /**
     * Called to set Dialog fragment to full screen
     *
     * @return Instantiated CustomBottomSheetFragment object
     */
    fun fullScreen(): CustomBottomSheetFragment {
        isFullscreen = true
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.AppBottomSheetDialogTheme)
        return this
    }

    /**
     * Called to set Dialog fragment background transparent
     *
     * @return Instantiated CustomBottomSheetFragment object
     */
    fun transparentBackground(): CustomBottomSheetFragment {
        isTransparent = true
        return this
    }

    /**
     * Called to set Dialog fragment cancellable
     *
     * @return Instantiated CustomBottomSheetFragment object
     */
    fun cancellable(isCancellable: Boolean): CustomBottomSheetFragment {
        this.isCancellable = isCancellable
        return this
    }

    fun launch() {
        val tag = screenTag ?: "MESSAGE"
        showOnce(manager, tag)
    }

    override fun onStart() {
        if (dialog != null && dialog!!.window != null) {
            dialog?.let {
                it.window?.let { window ->
                    if (animationStyle != null) window.attributes.windowAnimations =
                        animationStyle!!
                    if (isTransparent) window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))else {
                    if (isFullscreen) window.setBackgroundDrawable(ColorDrawable(getResources().getColor(R.color.dark_purple)))}
                }
            }

        }
        super.onStart()

        if (isFullscreen){
            val sheetContainer = requireView().parent as? ViewGroup ?: return
            sheetContainer.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        }
    }

    override fun getTheme(): Int {
        return R.style.AppBottomSheetDialogTheme
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST)
        return inflater.inflate(layoutRes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        for (i in clickers.indices) {
            val viewClick = clickers[i]
            view.findViewById<View>(viewClick.first).setOnClickListener { viewClick.second() }
        }

        pageHolder?.let {
            it.bindView(view)
            binder?.onBind(it)
        }

        isCancelable = isCancellable
        configBackPressCallback()
        if (statusBarColor != null) applyStatusBarColor(statusBarColor!!)
    }

    private fun configBackPressCallback() {
        dialog!!.setOnKeyListener(DialogInterface.OnKeyListener { _, keyCode, _ ->
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                backClickListener?.invoke()
                return@OnKeyListener true
            }
            false
        })
    }

    private fun applyStatusBarColor(@ColorRes colorInt: Int) {
        val window = dialog!!.window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (window != null) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = ContextCompat.getColor(requireContext(), colorInt)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        view.fitSystemWindowsAndAdjustResize()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return if (isFullscreen)
            BottomSheetDialog(requireContext(), theme).apply {
                behavior.state = STATE_EXPANDED
                behavior.peekHeight = 1000
            }
        else super.onCreateDialog(savedInstanceState)
    }


    interface Binder<T : PageHolder> {
        fun onBind(holder: T)
    }

    abstract class PageHolder {
        abstract fun bindView(view: View)
    }

    companion object {
        /**
         * Called to init CustomBottomSheetFragment object with fragmentManager.
         * Mandatory
         *
         * @param fragmentManager Object used to launch CustomBottomSheetFragment
         * @return Instantiated CustomBottomSheetFragment object
         */
        fun with(fragmentManager: FragmentManager): CustomBottomSheetFragment {
            val process = CustomBottomSheetFragment()
            process.manager = fragmentManager
            return process
        }
    }
}

fun DialogFragment.showOnce(manager: FragmentManager, tag: String) {
    if (manager.findFragmentByTag(tag) == null) {
        show(manager, tag)
    }
}

fun View?.fitSystemWindowsAndAdjustResize() = this?.let { view ->
    ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
        view.fitsSystemWindows = true
        val bottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom

        WindowInsetsCompat
            .Builder()
            .setInsets(
                WindowInsetsCompat.Type.systemBars(),
                Insets.of(0, 0, 0, bottom)
            )
            .build()
            .apply {
                ViewCompat.onApplyWindowInsets(v, this)
            }
    }
}
