package rs.readahead.washington.mobile.views.fragment.uwazi.download

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.database.DataSource
import rs.readahead.washington.mobile.data.database.KeyDataSource
import rs.readahead.washington.mobile.data.entity.uwazi.UwaziEntityRow
import rs.readahead.washington.mobile.data.repository.UwaziRepository
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate
import timber.log.Timber

class DownloadedTemplatesViewModel : ViewModel(){

    private val uwaziRepository by lazy { UwaziRepository() }
    var error = MutableLiveData<String>()
    private val _templates = MutableLiveData<List<CollectTemplate>>()
    val templates: LiveData<List<CollectTemplate>> get() = _templates
    private val keyDataSource: KeyDataSource = MyApplication.getKeyDataSource()
    private val disposables = CompositeDisposable()

    private fun getTemplates(servers: List<UWaziUploadServer>) {
        val list = mutableListOf<CollectTemplate>()
        viewModelScope.launch {
            if (!servers.isNullOrEmpty()) {
                servers.asFlow().map { server ->
                    uwaziRepository.getTemplates(server)
                        .catch {
                            Timber.d(it)
                        }
                        .collect {
                            it.rows.map { entity ->
                                val collectTemplate = CollectTemplate(server.id,entity)
                                collectTemplate.apply {
                                    serverName = server.name
                                }
                                list.add(collectTemplate)
                            }
                        }
                }.catch { e -> error.postValue(e.toString())  }
                    .collect {
                        _templates.postValue(list)
                    }
            }
        }

    }

    fun getTemplateInfo(template: CollectTemplate) {
    //TODO COMPLETE THIS
    //return CollectTemplate()
    }

    fun getServers() {
        disposables.add(keyDataSource.dataSource
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { }
            .flatMapSingle { obj: DataSource -> obj.listUwaziServers() }
            .subscribe(
                { list: List<UWaziUploadServer> -> getTemplates(list) }
            ) { throwable: Throwable? ->
                FirebaseCrashlytics.getInstance().recordException(throwable!!)
            }
        )
    }
}