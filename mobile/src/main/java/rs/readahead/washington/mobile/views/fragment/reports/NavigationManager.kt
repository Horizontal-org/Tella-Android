package rs.readahead.washington.mobile.views.fragment.reports

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.fragment.reports.di.NavControllerProvider
import javax.inject.Inject


class NavigationManager (private val navControllerProvider: NavControllerProvider, val bundle: Bundle){

    fun navigateTo(destinationId: Int) {
        navControllerProvider.navController.navigate(destinationId)
    }

    fun navigateTo(directions: NavDirections) {
        navControllerProvider.navController.navigate(directions)
    }
    fun navigateToWithBundle(destinationId: Int) {
        navControllerProvider.navController.navigate(destinationId,bundle)
    }

    fun navigateToMicro() {
        navControllerProvider.navController.navigate(R.id.mic,bundle)
    }
    fun navigateToNewReportScreen() {
        navControllerProvider.navController.navigate(R.id.action_reportsScreen_to_newReport_screen,bundle)
    }

    fun navigateToEditTellaServerFragment() {
        navControllerProvider.navController.navigate(R.id.action_loginToReportsScreen_to_editTellaServerFragment,bundle)
    }

    fun navigateToEnterUrlScreen() {
        navControllerProvider.navController.navigate(R.id.reports_settings)
    }

}