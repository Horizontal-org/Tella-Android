package rs.readahead.washington.mobile.views.fragment.vault.home

import android.os.Bundle
import android.view.*
import android.widget.RelativeLayout
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.hzontal.tella_vault.VaultFile
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.entity.XFormEntity
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.views.activity.MainActivity
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import rs.readahead.washington.mobile.views.custom.CountdownTextView
import rs.readahead.washington.mobile.views.fragment.vault.adapters.VaultAdapter
import rs.readahead.washington.mobile.views.fragment.vault.adapters.VaultClickListener

class HomeVaultFragment : BaseFragment(), VaultClickListener, IHomeVaultPresenter.IView  {
    private lateinit var toolbar: Toolbar
    private lateinit var collapsingToolbar: CollapsingToolbarLayout
    private lateinit var vaultRecyclerView: RecyclerView
    private lateinit var panicModeView: RelativeLayout
    private lateinit var countDownTextView: CountdownTextView
    private var timerDuration = 0
    private var panicActivated = false
    private val vaultAdapter by lazy { VaultAdapter(this) }
    private lateinit var homeVaultPresenter: HomeVaultPresenter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_vault, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.home_menu, menu)
    }

    override fun initView(view: View) {
        toolbar = view.findViewById(R.id.toolbar)
        collapsingToolbar = view.findViewById(R.id.collapsing_toolbar)
        vaultRecyclerView = view.findViewById(R.id.vaultRecyclerView)
        panicModeView = view.findViewById(R.id.panic_mode_view)
        countDownTextView = view.findViewById(R.id.countdown_timer)
        setUpToolbar()
        initData()
        initListeners()
    }

    private fun initData() {
        homeVaultPresenter = HomeVaultPresenter(this)
        vaultRecyclerView.apply {
            adapter = vaultAdapter
            layoutManager = LinearLayoutManager(activity)
        }
        timerDuration = resources.getInteger(R.integer.panic_countdown_duration)

    }
    private fun initListeners(){
        panicModeView.setOnClickListener { onPanicClicked() }
    }

    private fun setUpToolbar() {
        val activity = context as MainActivity
        activity.setSupportActionBar(toolbar)
        collapsingToolbar.setupWithNavController(toolbar, findNavController())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
               nav().navigate(R.id.main_settings)
                return true
            }
            R.id.action_close -> {
                MyApplication.exit(activity)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPanicModeSwipeListener(progress: Int) {
        showPanicScreens()
    }

    override fun onRecentFilesItemClickListener(vaultFile: VaultFile) {
    }

    override fun onFavoriteItemClickListener(form: XFormEntity) {
    }

    override fun allFilesClickListener() {
        nav().navigate(R.id.action_homeScreen_to_attachments_screen)
    }

    override fun imagesClickListener() {
    }

    override fun audioClickListener() {
    }

    override fun documentsClickListener() {
    }

    override fun othersClickListener() {

    }

    override fun videoClickListener() {
    }

    private fun stopPanicking() {
        countDownTextView.cancel()
        countDownTextView.setCountdownNumber(timerDuration)
        panicActivated = false
        // showMainControls()
    }

    override fun onResume() {
        super.onResume()
        setupPanicView()
        if (panicActivated) {
            showPanicScreens()
            panicActivated = false
        } else {
            maybeClosePanic()
        }
    }

    private fun maybeClosePanic(): Boolean {
        if (panicModeView.visibility == View.VISIBLE) {
            stopPanicking()
            hidePanicScreens()
        }
        return false // todo: check panic state here
    }

    private fun hidePanicScreens() {
        collapsingToolbar.visibility = View.VISIBLE
        (activity as MainActivity).showBottomNavigation()
        setupPanicView()
        panicModeView.visibility = View.GONE
    }

    private fun showPanicScreens() {
        // really show panic screen
        collapsingToolbar.visibility = View.GONE
        (activity as MainActivity).hideBottomNavigation()

        panicModeView.visibility = View.VISIBLE
        panicModeView.alpha = 1f
        countDownTextView.start(
            timerDuration
        ) {
            homeVaultPresenter.executePanicMode()
        }
    }

    private fun setupPanicView() {
        if (Preferences.isQuickExit()) {
            vaultAdapter.addPanicMode()
        } else {
            vaultAdapter.hidePanicMode()
        }
    }
   private fun onPanicClicked() {
        hidePanicScreens()
        stopPanicking()
    }

    override fun onCountTUServersEnded(num: Long?) {
    }

    override fun onCountTUServersFailed(throwable: Throwable?) {
    }

    override fun onCountCollectServersEnded(num: Long?) {
    }

    override fun onCountCollectServersFailed(throwable: Throwable?) {
    }

}