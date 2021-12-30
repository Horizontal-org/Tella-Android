package rs.readahead.washington.mobile.views.fragment.uwazi

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import rs.readahead.washington.mobile.data.entity.uwazi.UwaziEntityRow
import rs.readahead.washington.mobile.data.repository.UwaziRepository
import timber.log.Timber

class SharedUwaziViewModel : ViewModel() {
    private val uwaziRepository by lazy { UwaziRepository() }
    var error = MutableLiveData<String>()
    private val _templates = MutableLiveData<List<UwaziEntityRow>>()
    val templates: LiveData<List<UwaziEntityRow>> get() = _templates

    fun getTemplates() {
        viewModelScope.launch {
            uwaziRepository.getTemplates()
                .onStart {

                }
                .catch {
                    Timber.d(it)
                }
                .collect {
                    _templates.postValue(it.rows)
                }
        }
    }
}