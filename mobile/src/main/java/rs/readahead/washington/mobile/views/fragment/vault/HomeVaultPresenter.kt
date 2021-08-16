package rs.readahead.washington.mobile.views.fragment.vault

import rs.readahead.washington.mobile.mvp.contract.IBasePresenter
import timber.log.Timber

class HomeVaultPresenter constructor(val view: IHomeVaultPresenter.IView) : IHomeVaultPresenter.IPresenter {

    init {

    }


    override fun destroy() {
    }

    override fun executePanicMode() {
            Timber.d("Panic mode executed")
    }
}