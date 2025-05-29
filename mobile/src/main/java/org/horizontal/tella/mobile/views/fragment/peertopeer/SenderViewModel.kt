package org.horizontal.tella.mobile.views.fragment.peertopeer

import androidx.lifecycle.ViewModel
import com.hzontal.tella_vault.VaultFile
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile
import org.horizontal.tella.mobile.util.fromJsonToObjectList
import javax.inject.Inject

@HiltViewModel
class SenderViewModel @Inject constructor(
) : ViewModel() {

    fun putVaultFilesInForm(vaultFileList: String): Single<List<VaultFile>> {
        return Single.fromCallable {
            vaultFileList.fromJsonToObjectList(String::class.java) ?: emptyList()
        }
            .flatMap { fileIds ->
                MyApplication.keyRxVault.rxVault
                    .firstOrError()
                    .flatMap { rxVault ->
                        Observable.fromIterable(fileIds)
                            .flatMapSingle { fileId ->
                                rxVault[fileId]
                                    .subscribeOn(Schedulers.io())
                                    .onErrorReturn { null } // safe, allows null
                            }
                            .filter { it != null } // filter out nulls
                            .map { it!! } // safe to force unwrap if you're sure it's not null now
                            .toList()
                    }
            }
            .subscribeOn(Schedulers.io())
    }

    fun mediaFilesToVaultFiles(files: List<FormMediaFile>?): List<VaultFile> {
        val vaultFiles = ArrayList<VaultFile>()
        files?.map { mediaFile ->
            vaultFiles.add(mediaFile.vaultFile)
        }
        return vaultFiles
    }
}


