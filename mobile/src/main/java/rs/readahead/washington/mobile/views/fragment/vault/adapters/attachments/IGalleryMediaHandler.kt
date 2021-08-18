package rs.readahead.washington.mobile.views.fragment.vault.adapters.attachments

import com.hzontal.tella_vault.VaultFile


interface IGalleryMediaHandler {
    fun playMedia(vaultFile: VaultFile)
    fun onSelectionNumChange(num: Int)
    fun onMediaSelected(vaultFile: VaultFile)
    fun onMediaDeselected(vaultFile: VaultFile)
}