package org.horizontal.tella.mobile.views.fragment.reports

import android.os.Bundle
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.util.navigateSafe
import org.horizontal.tella.mobile.views.fragment.reports.di.NavControllerProvider


class NavigationManager(
    private val navControllerProvider: NavControllerProvider,
    val bundle: Bundle
) {

    fun navigateTo(destinationId: Int) {
        navControllerProvider.navController.navigateSafe(destinationId)
    }

    private fun navigateToWithBundle(destinationId: Int) {
        navControllerProvider.navController.navigateSafe(destinationId, bundle)
    }

    fun navigateFromEnterUploadServerFragmentToLoginReportsFragment() {
        navigateToWithBundle(R.id.action_enterUploadServerFragment_to_loginReportsFragment)
    }

    fun navigateToMicro() {
        navControllerProvider.navController.navigateSafe(R.id.mic, bundle)
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
        navigateWithBundleAndClearBackStack(R.id.action_newReport_to_reportSendScreen)
    }

    private fun navigateWithBundleAndClearBackStack(destinationId: Int) {
        navigateToWithBundle(destinationId)
        navControllerProvider.navController.clearBackStack(destinationId)
    }

    fun navigateToEnterUrlScreen() {
        navControllerProvider.navController.navigateSafe(R.id.reports_settings)
    }

    fun navigateToEnterNextCloudLoginScreen(){
        navigateToWithBundle(R.id.action_enterNextCloudUrlScreen_to_loginNextCloudScreen)
    }

}