package rs.readahead.washington.mobile.mvvm.server

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import rs.readahead.washington.mobile.domain.usecases.reports.CheckReportsServerUseCase
import javax.inject.Inject

@HiltViewModel
class CheckTUSServerViewModel @Inject constructor(val checkReportsServerUseCase: CheckReportsServerUseCase) :
    ViewModel() {
        
}