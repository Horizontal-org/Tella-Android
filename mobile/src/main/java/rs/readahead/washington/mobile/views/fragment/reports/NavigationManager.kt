package rs.readahead.washington.mobile.views.fragment.reports

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.fragment.reports.di.NavControllerProvider
import javax.inject.Inject


class NavigationManager(
    private val navControllerProvider: NavControllerProvider,
    val bundle: Bundle
) {

    fun navigateTo(destinationId: Int) {
        navControllerProvider.navController.navigate(destinationId)
    }

    fun navigateTo(directions: NavDirections) {
        navControllerProvider.navController.navigate(directions)
    }

    private fun navigateToWithBundle(destinationId: Int) {
        navControllerProvider.navController.navigate(destinationId, bundle)
    }

    fun navigateFromEnterUploadServerFragmentToLoginReportsFragment() {
        navigateToWithBundle(R.id.action_enterUploadServerFragment_to_loginReportsFragment)
    }

    fun navigateToMicro() {
        navControllerProvider.navController.navigate(R.id.mic, bundle)
    }

    fun navigateFromReportsScreenToNewReportScreen() {
        navigateToWithBundle(R.id.action_reportsScreen_to_newReport_screen)
    }

    fun navigateFromLoginToReportsScreenToEditTellaServerFragment() {
        navigateToWithBundle(R.id.action_loginToReportsScreen_to_editTellaServerFragment)
    }

    fun navigateFromEditTellaServerFragmentToSuccessfulSetServerFragment() {
        navigateToWithBundle(R.id.action_editTellaServerFragment_to_successfulSetServerFragment)
    }

    fun navigateFromSuccessfulLoginFragmentToServerAdvancedSettingsFragment() {
        navigateToWithBundle(R.id.action_successfulLoginFragment_to_serverAdvancedSettingsFragment)
    }

    fun navigateFromServerAdvancedSettingsFragmentToSuccessfulSetServerFragment() {
        navigateToWithBundle(R.id.action_serverAdvancedSettingsFragment_to_successfulSetServerFragment)
    }

    fun navigateFromReportsScreenToReportSendScreen() {
        navigateToWithBundle(R.id.action_reportsScreen_to_reportSendScreen)
    }

    fun navigateFromReportsScreenToReportSubmittedScreen() {
        navigateToWithBundle(R.id.action_reportsScreen_to_reportSubmittedScreen)
    }

    fun navigateFromNewReportsScreenToReportSendScreen() {
        navigateToWithBundle(R.id.action_newReport_to_reportSendScreen)
        navControllerProvider.navController.clearBackStack(R.id.action_newReport_to_reportSendScreen)
    }

    fun navigateToEnterUrlScreen() {
        navControllerProvider.navController.navigate(R.id.reports_settings)
    }

}