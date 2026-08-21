package org.horizontal.tella.mobile.domain.usecases.shared

import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.MyApplication
import javax.inject.Inject

/**
 * Centralized check for file/folder name uniqueness in the vault.
 * Use for all import/create/rename flows so "file name taken" is consistent.
 */
class CheckFileNameUseCase @Inject constructor() {

    /**
     * Checks if [fileName] is unique in the root folder.
     * Prefer [isFileNameUniqueInParent] when the target folder is known.
     */
    fun isFileNameUnique(fileName: String): Single<Boolean> =
        isFileNameUniqueInParent(fileName, null)

    /**
     * Checks if [fileName] is unique under the given [parentId].
     * @param parentId Parent folder id, or null for vault root.
     */
    fun isFileNameUniqueInParent(fileName: String, parentId: String?): Single<Boolean> {
        return if (parentId == null) {
            Single.fromCallable {
                val files = MyApplication.vault.list(null)
                files.none { it.name == fileName }
            }
        } else {
            MyApplication.keyRxVault.rxVault
                .firstOrError()
                .flatMap { rxVault ->
                    rxVault.get(parentId)
                        .flatMap { parent ->
                            rxVault.list(parent)
                                .map { files -> files.none { it.name == fileName } }
                        }
                }
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }
}

