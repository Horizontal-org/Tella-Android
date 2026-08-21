package org.horizontal.tella.mobile.views.activity.camera

import com.hzontal.tella_vault.VaultFile
import org.horizontal.tella.mobile.mvp.contract.IMetadataAttachPresenterContract
import org.horizontal.tella.mobile.views.activity.MetaDataFragment

class CameraFragment : MetaDataFragment(), IMetadataAttachPresenterContract.IView {
    override fun onMetadataAttached(vaultFile: VaultFile?) {
        TODO("Not yet implemented")
    }

    override fun onMetadataAttachError(throwable: Throwable?) {
        TODO("Not yet implemented")
    }
}