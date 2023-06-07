package rs.readahead.washington.mobile.views.fragment.vault.attachements

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hzontal.tella_vault.VaultFile
import com.hzontal.tella_vault.filter.FilterType
import com.hzontal.tella_vault.filter.Sort
import io.reactivex.disposables.CompositeDisposable
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.database.KeyDataSource

class AttachmentsViewModel  : ViewModel() {
    private val disposables = CompositeDisposable()
    private var keyDataSource: KeyDataSource = MyApplication.getKeyDataSource()
    private val _filesData = MutableLiveData<List<VaultFile?>>()
    val filesData: LiveData<List<VaultFile?>> = _filesData

    fun getFiles(parent: String?, filterType: FilterType?, sort: Sort?){

    }

    override fun onCleared() {
        disposables.dispose()
        super.onCleared()
    }
}