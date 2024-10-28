package rs.readahead.washington.mobile.util

import android.os.Bundle
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.fragment.reports.di.NavControllerProvider

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

    fun navigateFromGoogleDriveScreenToNewGoogleDriveScreen() {
        navigateToWithBundle(R.id.action_googleDriveScreen_to_newGoogleDriveScreen)
    }

    fun navigateFromGoogleDriveEntryScreenToGoogleDriveSendScreen() {
        navigateToWithBundle(R.id.action_newGoogleDriveScreen_to_googleDriveSendScreen)
    }

    fun navigateFromGoogleDriveMainScreenToGoogleDriveSendScreen() {
        navigateToWithBundle(R.id.action_googleDriveScreen_to_googleDriveSendScreen)
    }

    fun navigateFromGoogleDriveScreenToGoogleDriveSubmittedScreen() {
        navigateToWithBundle(R.id.action_googleDriveScreen_to_googleDriveSubmittedScreen)
    }

    fun navigateFromDropBoxScreenToDropBoxSubmittedScreen() {
        navigateToWithBundle(R.id.action_dropBoxScreen_to_dropBoxSubmittedScreen)
    }

    fun navigateFromDropBoxScreenToNewDropBoxScreen() {
        navigateToWithBundle(R.id.action_dropBoxScreen_to_newdropBoxScreen)
    }

    fun navigateFromDropBoxEntryScreenToDropBoxSendScreen() {
        navigateToWithBundle(R.id.action_newDropBoxScreen_to_dropBoxSendScreen)
    }

    fun navigateFromDropBoxMainScreenToDropBoxSendScreen() {
        navigateToWithBundle(R.id.action_dropBoxScreen_to_dropBoxSendScreen)
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

    fun navigateFromUwaziEntryToSelectEntities() {
        navigateToWithBundle(R.id.action_uwaziEntryScreen_to_uwaziSelectEntitiesScreen)
    }

    fun navigateFromUwaziEntryToSendScreen() {
        navigateToWithBundle(R.id.action_uwaziEntryScreen_to_uwaziSendScreen)
    }

    fun navigateFromUwaziScreenToDownloadScreen() {
        navigateTo(R.id.action_uwaziScreen_to_uwaziDownloadScreen)
    }

    fun navigateFromUwaziScreenToUwaziEntryScreen() {
        navigateToWithBundle(R.id.action_uwaziScreen_to_uwaziEntryScreen)
    }

    fun navigateFromUwaziScreenToUwaziSubmitedPreview() {
        navigateToWithBundle(R.id.action_uwaziScreen_to_uwaziSubmittedPreview)
    }

    fun navigateFromHomeScreenToUwaziScreen() {
        navigateTo(R.id.action_homeScreen_to_uwazi_screen)
    }

    fun navigateToEnterNextCloudLoginScreen() {
        navigateTo(R.id.action_enterNextCloudUrlScreen_to_loginNextCloudScreen)
    }

    fun navigateFromGoogleDriveConnectFragmentToSelectGoogleDriveFragment() {
        navigateToWithBundle(R.id.action_googleDriveConnectFragment_to_selectGoogleDriveFragment)
    }

    fun navigateFromSelectGoogleDriveToCreateFolderFragment() {
        navigateToWithBundle(R.id.action_selectGoogleDriveFragment_to_createFolderFragment)
    }

    fun navigateFromSelectGoogleDriveFragmentToSelectSharedDriveFragment() {
        navigateToWithBundle(R.id.action_selectGoogleDriveFragment_to_selectSharedDriveFragment)
    }

    fun navigateFromCreateFolderFragmentToGoogleDriveConnectedServerFragment() {
        navigateToWithBundle(R.id.action_createFolderFragment_to_googleDriveConnectedServerFragment)
    }

    fun navigateFromSelectSharedDriveFragmentToGoogleDriveConnectedServerFragment() {
        navigateToWithBundle(R.id.action_selectSharedDriveFragment_to_googleDriveConnectedServerFragment)
    }

}