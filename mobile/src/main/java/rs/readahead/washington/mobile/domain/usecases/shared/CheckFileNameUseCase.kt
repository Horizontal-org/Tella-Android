package rs.readahead.washington.mobile.domain.usecases.shared

import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication
import javax.inject.Inject


class CheckFileNameUseCase @Inject constructor() {
    fun isFileNameUnique(fileName: String): Single<Boolean> {
        return Single.fromCallable {
            val files = MyApplication.vault.list(null)

            // Check if any file has the same name
            files.none { it.name == fileName }
        }
            .subscribeOn(Schedulers.io()) // Ensure computation is done on the I/O thread
            .observeOn(AndroidSchedulers.mainThread()) // Results observed on the main thread
    }
}

