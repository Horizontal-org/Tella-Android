package rs.readahead.washington.mobile.views.fragment.vault.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.hzontal.tella_vault.VaultFile
import org.hzontal.shared_ui.appbar.ToolbarComponent
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.util.DateUtil
import rs.readahead.washington.mobile.util.FileUtil
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import rs.readahead.washington.mobile.views.fragment.vault.attachements.VAULT_FILE_ARG

const val VAULT_FILE_INFO_TOOLBAR = "VAULT_FILE_INFO_TOOLBAR"
class VaultInfoFragment : BaseFragment() {
    private lateinit var fileInfoTv: TextView
    private lateinit var fileSizeTv: TextView
    private lateinit var fileFormatTv: TextView
    private lateinit var fileCreatedTv: TextView
    private lateinit var fileResolutionTv: TextView
    private lateinit var fileLengthTv: TextView
    private lateinit var filePathTv: TextView

    fun newInstance(vaultFile: VaultFile,showToolbar : Boolean): VaultInfoFragment {
        val args = Bundle()
        args.putSerializable(VAULT_FILE_ARG,vaultFile)
        args.putBoolean(VAULT_FILE_INFO_TOOLBAR,showToolbar)
        val fragment = VaultInfoFragment()
        fragment.arguments = args
        return fragment
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_vault_info, container, false)
    }

    override fun initView(view: View) {
        with(view){
            fileInfoTv = findViewById(R.id.fileInfoTv)
            fileSizeTv = findViewById(R.id.fileSizeTv)
            fileFormatTv = findViewById(R.id.fileFormatTv)
            fileCreatedTv = findViewById(R.id.fileCreatedTv)
            fileResolutionTv = findViewById(R.id.fileResolutionTv)
            fileLengthTv = findViewById(R.id.fileLengthTv)
            filePathTv = findViewById(R.id.filePathTv)
            findViewById<ToolbarComponent>(R.id.toolbar).backClickListener = {back()}
        }
         arguments?.getSerializable(VAULT_FILE_ARG)?.let {
             (it as VaultFile).apply {
                 fileInfoTv.text = name
                 fileFormatTv.text = mimeType
                 fileCreatedTv.text = DateUtil.getDate(created)
                 fileSizeTv.text = FileUtil.getFileSizeString(size)
                 filePathTv.text = path
             }
         }
        arguments?.getBoolean(VAULT_FILE_INFO_TOOLBAR)?.let { isToolbarShonw ->
            view.findViewById<ToolbarComponent>(R.id.toolbar).visibility = if(isToolbarShonw) View.VISIBLE else View.GONE
        }
    }
}