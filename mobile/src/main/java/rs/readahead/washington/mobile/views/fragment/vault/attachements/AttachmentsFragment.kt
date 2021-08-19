package rs.readahead.washington.mobile.views.fragment.vault.attachements

import android.os.Bundle
import android.view.*
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.hzontal.tella_locking_ui.common.extensions.toggleVisibility
import com.hzontal.tella_vault.VaultFile
import org.hzontal.shared_ui.appbar.ToolbarComponent
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.activity.MainActivity
import rs.readahead.washington.mobile.views.adapters.AttachmentsRecycleViewAdapter
import rs.readahead.washington.mobile.views.base_ui.BaseToolbarFragment
import rs.readahead.washington.mobile.views.custom.SpacesItemDecoration
import rs.readahead.washington.mobile.views.fragment.vault.adapters.attachments.AttachmentsAdapter
import rs.readahead.washington.mobile.views.fragment.vault.adapters.attachments.IGalleryMediaHandler
import timber.log.Timber

class AttachmentsFragment : BaseToolbarFragment(), View.OnClickListener, IGalleryMediaHandler, IAttachmentsPresenter.IView{
    private lateinit var attachmentsRecyclerView: RecyclerView
    private val attachmentAdapter by lazy { AttachmentsAdapter(this,activity) }
    private val attachmentsPresenter by lazy { AttachmentsPresenter(this) }
    private lateinit var gridLayoutManager: GridLayoutManager
    private lateinit var detailsFab: FloatingActionButton
    private lateinit var toolbar: ToolbarComponent
    private lateinit var collapsingToolbar: CollapsingToolbarLayout
    private lateinit var listCheck: ImageView
    private lateinit var gridCheck: ImageView
    private lateinit var emptyViewMsgContainer : LinearLayout
    private lateinit var checkBoxList : AppCompatImageView
    private var isListCheckOn = false

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
        attachmentsRecyclerView.addItemDecoration(SpacesItemDecoration(5))
        listCheck = view.findViewById(R.id.listCheck)
        gridCheck = view.findViewById(R.id.gridCheck)
        emptyViewMsgContainer = view.findViewById(R.id.emptyViewMsgContainer)
        toolbar = view.findViewById(R.id.toolbar)
        gridLayoutManager = GridLayoutManager(activity, 1)
        attachmentAdapter.setLayoutManager(gridLayoutManager)
        attachmentsRecyclerView.apply {
            adapter = attachmentAdapter
            layoutManager = gridLayoutManager
        }
        detailsFab = view.findViewById(R.id.detailsFab)
        checkBoxList = view.findViewById(R.id.checkBoxList)

        detailsFab.setOnClickListener { onFabDetailsClick() }
        toolbar.backClickListener = { nav().navigateUp() }
        listCheck.setOnClickListener(this)
        gridCheck.setOnClickListener(this)
        checkBoxList.setOnClickListener(this)
        initData()
    }

    private fun initData() {
        attachmentsPresenter.getFiles(null,null)
    }

    private fun onFabDetailsClick() {

    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.gridCheck -> {
                gridCheck.toggleVisibility(false)
                listCheck.toggleVisibility(true)
                gridLayoutManager.spanCount = 4
                attachmentAdapter.notifyItemRangeChanged(0, attachmentAdapter.itemCount)
            }
            R.id.listCheck -> {
                gridCheck.toggleVisibility(true)
                listCheck.toggleVisibility(false)
                gridLayoutManager.spanCount = 1
                attachmentAdapter.notifyItemRangeChanged(0, attachmentAdapter.itemCount)
            }
            R.id.checkBoxList ->{
                isListCheckOn = !isListCheckOn
                attachmentAdapter.enableSelectMode(isListCheckOn)
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

    override fun onGetFilesStart() {
    }

    override fun onGetFilesEnd() {
    }

    override fun onGetFilesSuccess(files: List<VaultFile?>) {
        if(files.isEmpty()){
            attachmentsRecyclerView.visibility = View.GONE
            emptyViewMsgContainer.visibility = View.VISIBLE
        }else{
            attachmentsRecyclerView.visibility = View.VISIBLE
            emptyViewMsgContainer.visibility = View.GONE
        }
        attachmentAdapter.submitList(files)
    }

    override fun onGetFilesError(error: Throwable?) {
        Timber.d(error, javaClass.name)
    }

    override fun onMediaImported(vaultFile: VaultFile?) {
    }

    override fun onImportError(error: Throwable?) {
    }

    override fun onImportStarted() {
    }

    override fun onImportEnded() {
    }

    override fun onMediaFilesAdded(vaultFile: VaultFile?) {
    }

    override fun onMediaFilesAddingError(error: Throwable?) {
    }

    override fun onMediaFilesDeleted(num: Int) {
    }

    override fun onMediaFilesDeletionError(throwable: Throwable?) {
    }

    override fun onMediaExported(num: Int) {
    }

    override fun onExportError(error: Throwable?) {
    }

    override fun onExportStarted() {
    }

    override fun onExportEnded() {
    }

    override fun onCountTUServersEnded(num: Long?) {
    }

    override fun onCountTUServersFailed(throwable: Throwable?) {

    }


}