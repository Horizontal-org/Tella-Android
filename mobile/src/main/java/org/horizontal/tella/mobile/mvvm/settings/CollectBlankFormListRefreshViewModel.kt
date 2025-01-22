package org.horizontal.tella.mobile.mvvm.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.horizontal.tella.mobile.data.database.DataSource
import org.horizontal.tella.mobile.data.database.KeyDataSource
import org.horizontal.tella.mobile.data.repository.OpenRosaRepository
import org.horizontal.tella.mobile.domain.entity.IErrorBundle
import org.horizontal.tella.mobile.domain.entity.Server
import org.horizontal.tella.mobile.domain.entity.collect.CollectServer
import org.horizontal.tella.mobile.domain.entity.collect.ListFormResult
import org.horizontal.tella.mobile.domain.repository.IOpenRosaRepository
import org.horizontal.tella.mobile.mvvm.base.BaseSettingsViewModel
import javax.inject.Inject

@HiltViewModel
class CollectBlankFormListRefreshViewModel @Inject constructor(
    private val keyDataSource: KeyDataSource
) : BaseSettingsViewModel() {

    private val odkRepository: IOpenRosaRepository = OpenRosaRepository()

    private val _refreshSuccess = MutableLiveData<Unit>()
    val refreshSuccess: LiveData<Unit> get() = _refreshSuccess

    private val _refreshError = MutableLiveData<Throwable>()
    val refreshError: LiveData<Throwable> get() = _refreshError

    private val _errorBundles = MutableLiveData<List<IErrorBundle>>()
    val errorBundles: LiveData<List<IErrorBundle>> get() = _errorBundles

    fun refreshBlankForms() {
        disposables.add(
            keyDataSource.dataSource
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(::fetchCollectServers)
                .flatMap(::handleServerResults)
                .flatMap { listFormResult ->
                    keyDataSource.dataSource.flatMap { dataSource ->
                        dataSource.updateBlankForms(listFormResult).toObservable()
                    }
                }
                .subscribe({ listFormResult ->
                    // Emit error bundles to LiveData
                    _errorBundles.postValue(listFormResult.errors)

                    // Log errors
                    listFormResult.errors.forEach { error ->
                        FirebaseCrashlytics.getInstance().recordException(error.exception)
                    }

                    // Notify success
                    _refreshSuccess.postValue(Unit)
                }, { throwable ->
                    // Log and emit error
                    FirebaseCrashlytics.getInstance().recordException(throwable)
                    _refreshError.postValue(throwable)
                })
        )
    }

    private fun fetchCollectServers(dataSource: DataSource): Observable<List<CollectServer>> {
        return dataSource.listCollectServers().toObservable()
    }

    private fun handleServerResults(servers: List<CollectServer>): Observable<ListFormResult> {
        return if (servers.isEmpty()) {
            Observable.just(ListFormResult())
        } else {
            val singles = servers.map { odkRepository.formList(it).toObservable() }
            Observable.zip(singles) { results ->
                val allResults = ListFormResult()
                results.filterIsInstance<ListFormResult>().forEach {
                    allResults.forms.addAll(it.forms)
                    allResults.errors.addAll(it.errors)
                }
                allResults
            }
        }
    }

}