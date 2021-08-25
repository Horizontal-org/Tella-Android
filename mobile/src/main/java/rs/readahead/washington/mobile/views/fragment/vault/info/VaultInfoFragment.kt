package rs.readahead.washington.mobile.views.fragment.vault.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.base_ui.BaseFragment

class VaultInfoFragment : BaseFragment() {
    private lateinit var fileInfoTv: TextView
    private lateinit var fileSizeTv: TextView
    private lateinit var fileFormatTv: TextView
    private lateinit var fileCreatedTv: TextView
    private lateinit var fileResolutionTv: TextView
    private lateinit var fileLengthTv: TextView
    private lateinit var filePathTv: TextView

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
        }

    }
}