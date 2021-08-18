package rs.readahead.washington.mobile.views.fragment.vault

import android.os.Bundle
import android.view.*
import android.widget.ImageView
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.hzontal.tella_locking_ui.common.extensions.toggleVisibility
import org.hzontal.shared_ui.appbar.ToolbarComponent
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.activity.MainActivity
import rs.readahead.washington.mobile.views.base_ui.BaseToolbarFragment
import rs.readahead.washington.mobile.views.fragment.vault.adapters.attachments.AttachmentsAdapter
import rs.readahead.washington.mobile.views.fragment.vault.adapters.attachments.IGalleryMediaHandler
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.data.MockVaultFiles
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.data.VaultFile

class AttachmentsFragment : BaseToolbarFragment(), View.OnClickListener, IGalleryMediaHandler {
    private lateinit var attachmentsRecyclerView: RecyclerView
    private val attachmentAdapter by lazy { AttachmentsAdapter(this) }
    private lateinit var gridLayoutManager: GridLayoutManager
    private lateinit var detailsFab: FloatingActionButton
    private lateinit var toolbar: ToolbarComponent
    private lateinit var collapsingToolbar: CollapsingToolbarLayout
    private lateinit var listCheck: ImageView
    private lateinit var gridCheck: ImageView
    private var isGrid = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_vault_attachments, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.home_menu, menu)
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
        listCheck = view.findViewById(R.id.listCheck)
        gridCheck = view.findViewById(R.id.gridCheck)
        toolbar = view.findViewById(R.id.toolbar)
        gridLayoutManager = GridLayoutManager(activity, 1)
        attachmentAdapter.setLayoutManager(gridLayoutManager)
        attachmentsRecyclerView.apply {
            adapter = attachmentAdapter
            layoutManager = gridLayoutManager
        }
        detailsFab = view.findViewById(R.id.detailsFab)
        detailsFab.setOnClickListener { onFabDetailsClick() }
        toolbar.backClickListener = { nav().navigateUp() }
        listCheck.setOnClickListener(this)
        gridCheck.setOnClickListener(this)
        initData()
    }

    private fun initData() {
        attachmentAdapter.submitList(MockVaultFiles.getListVaultFiles())
    }

    private fun onMoreDetailsClicked(vaultFile: VaultFile) {

    }

    private fun onFabDetailsClick() {

    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.gridCheck -> {
                isGrid = false
                gridCheck.toggleVisibility(false)
                listCheck.toggleVisibility(true)
                gridLayoutManager.spanCount = 3
                attachmentAdapter.notifyItemRangeChanged(0, attachmentAdapter.itemCount)
            }
            R.id.listCheck -> {
                isGrid = true
                gridCheck.toggleVisibility(true)
                listCheck.toggleVisibility(false)
                gridLayoutManager.spanCount = 1
                attachmentAdapter.notifyItemRangeChanged(0, attachmentAdapter.itemCount)
            }
        }
    }

    override fun playMedia(vaultFile: VaultFile) {
    }

    override fun onSelectionNumChange(num: Int) {
    }

    override fun onMediaSelected(vaultFile: VaultFile) {
    }

    override fun onMediaDeselected(vaultFile: VaultFile) {
    }


}