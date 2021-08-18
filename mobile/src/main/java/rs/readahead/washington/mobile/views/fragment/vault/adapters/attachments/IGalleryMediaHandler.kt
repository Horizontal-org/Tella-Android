package rs.readahead.washington.mobile.views.fragment.vault.adapters.attachments

import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.data.VaultFile

interface IGalleryMediaHandler {
    fun playMedia(vaultFile: VaultFile)
    fun onSelectionNumChange(num: Int)
    fun onMediaSelected(vaultFile: VaultFile)
    fun onMediaDeselected(vaultFile: VaultFile)
}