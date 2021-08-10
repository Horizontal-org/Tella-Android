package rs.readahead.washington.mobile.views.fragment.vault

import android.os.Bundle
import android.view.*
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.base_ui.BaseToolbarFragment
import rs.readahead.washington.mobile.views.fragment.vault.adapters.VaultAdapter
import rs.readahead.washington.mobile.views.fragment.vault.adapters.attachments.AttachmentsAdapter
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.data.VaultFile

class AttachmentsFragment : BaseToolbarFragment() {
    private val attachmentAdapter by lazy { AttachmentsAdapter( ::onMoreDetailsClicked) }
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

    override fun setUpToolbar() {
    }

    override fun initView(view: View) {

    }

    private fun onMoreDetailsClicked(vaultFile: VaultFile){

    }

}