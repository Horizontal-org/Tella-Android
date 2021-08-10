package rs.readahead.washington.mobile.views.fragment.vault

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.CollapsingToolbarLayout
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.data.entity.XFormEntity
import rs.readahead.washington.mobile.views.activity.MainActivity
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import rs.readahead.washington.mobile.views.fragment.vault.adapters.VaultAdapter
import rs.readahead.washington.mobile.views.fragment.vault.adapters.VaultClickListener
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.data.MockVaultFiles
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.data.VaultFile

class HomeVaultFragment : BaseFragment() , VaultClickListener {
    private lateinit var toolbar: Toolbar
    private lateinit var collapsingToolbar : CollapsingToolbarLayout
    private lateinit var vaultRecyclerView : RecyclerView
    private val vaultAdapter by lazy {VaultAdapter(this)}

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_vault, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu,inflater);
        inflater.inflate(R.menu.home_menu,menu)
    }

    override fun initView(view: View){
        toolbar = view.findViewById(R.id.toolbar)
        collapsingToolbar = view.findViewById(R.id.collapsing_toolbar)
        vaultRecyclerView = view.findViewById(R.id.vaultRecyclerView)
        setUpToolbar()
        initData()
    }

    private fun initData(){
        vaultAdapter.apply {
            addPanicMode(MockVaultFiles.getRootFile())
            addRecentFiles(MockVaultFiles.getListVaultFiles())
            addFileActions(MockVaultFiles.getRootFile())
            addFavoriteForms(MockVaultFiles.getListForms())
        }
        vaultRecyclerView.apply {
            adapter = vaultAdapter
            layoutManager = LinearLayoutManager(activity)
        }
    }

    private fun setUpToolbar() {
        val activity = context as MainActivity
        activity.setSupportActionBar(toolbar)
        collapsingToolbar.setupWithNavController(toolbar, findNavController())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPanicModeSwipeListener(progress : Int) {
    }

    override fun onRecentFilesItemClickListener(vaultFile: VaultFile) {
    }

    override fun onFavoriteItemClickListener(form: XFormEntity) {
    }

    override fun myFilesClickListener() {
    }

    override fun galleryClickListener() {
    }

    override fun audioClickListener() {
    }

    override fun documentsClickListener() {
    }

    override fun othersClickListener() {
    }
}