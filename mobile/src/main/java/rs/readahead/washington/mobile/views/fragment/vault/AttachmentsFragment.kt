package rs.readahead.washington.mobile.views.fragment.vault

import android.os.Bundle
import android.view.*
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.hzontal.shared_ui.appbar.ToolbarComponent
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.activity.MainActivity
import rs.readahead.washington.mobile.views.base_ui.BaseToolbarFragment
import rs.readahead.washington.mobile.views.fragment.vault.adapters.attachments.AttachmentsAdapter
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.data.MockVaultFiles
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.data.VaultFile

class AttachmentsFragment : BaseToolbarFragment() {
    private lateinit var attachmentsRecyclerView : RecyclerView
    private val attachmentAdapter by lazy { AttachmentsAdapter( ::onMoreDetailsClicked) }
    private lateinit var detailsFab : FloatingActionButton
    private lateinit var toolbar : ToolbarComponent
    private lateinit var collapsingToolbar : CollapsingToolbarLayout


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_vault_attachments, container, false)
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu,inflater);
        inflater.inflate(R.menu.home_menu,menu)
    }

    override fun setToolbarLabel(labelRes: Int) {

    }

    override fun setToolbarHomeIcon(iconRes: Int) {
    }

    override fun setUpToolbar() {
        val activity = context as MainActivity
        activity.setSupportActionBar(toolbar)
        collapsingToolbar.setupWithNavController(toolbar, findNavController())
    }

    override fun initView(view: View) {
        attachmentsRecyclerView = view.findViewById(R.id.attachmentsRecyclerView)
        toolbar = view.findViewById(R.id.toolbar)
        attachmentsRecyclerView.apply {
            adapter = attachmentAdapter
            layoutManager = LinearLayoutManager(activity)
        }
        detailsFab = view.findViewById(R.id.detailsFab)
        detailsFab.setOnClickListener { onFabDetailsClick() }
        toolbar.backClickListener = {nav().navigateUp()}
        initData()
    }

    private fun initData(){
        attachmentAdapter.submitList(MockVaultFiles.getListVaultFiles())
    }

    private fun onMoreDetailsClicked(vaultFile: VaultFile){

    }

    private fun onFabDetailsClick(){

    }



}