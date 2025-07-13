package org.horizontal.tella.mobile.util

import android.os.Bundle
import androidx.navigation.navOptions
import org.horizontal.tella.mobile.R
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

    fun navigateFromGoogleDriveScreenToNewGoogleDriveScreen() {
        navigateToWithBundle(R.id.action_googleDriveScreen_to_newGoogleDriveScreen)
    }

    fun navigateFromNextCloudScreenToNewNextCloudScreen() {
        navigateToWithBundle(R.id.action_nextCloudScreen_to_newNextCloudScreen)
    }

    fun navigateFromGoogleDriveEntryScreenToGoogleDriveSendScreen() {
        navigateToWithBundle(R.id.action_newGoogleDriveScreen_to_googleDriveSendScreen)
    }

    fun navigateFromNextCloudEntryScreenToNextCloudSendScreen() {
        navigateToWithBundle(R.id.action_newNextCloudScreen_to_nextCloudSendScreen)
    }

    fun navigateFromGoogleDriveMainScreenToGoogleDriveSendScreen() {
        navigateToWithBundle(R.id.action_googleDriveScreen_to_googleDriveSendScreen)
    }

    fun navigateFromNextCloudMainScreenToNextCloudSendScreen() {
        navigateToWithBundle(R.id.action_nextCloudScreen_to_nextCloudSendScreen)
    }

    fun navigateFromGoogleDriveScreenToGoogleDriveSubmittedScreen() {
        navigateToWithBundle(R.id.action_googleDriveScreen_to_googleDriveSubmittedScreen)
    }

    fun navigateFromNextCloudScreenToNextCloudSubmittedScreen() {
        navigateToWithBundle(R.id.action_nextCloudScreen_to_nextCloudSubmittedScreen)
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

    fun navigateFromHomeScreenToUwaziEntryScreen() {
        navigateToWithBundle(R.id.action_homeScreen_to_uwaziEntryScreen)
    }

    fun navigateToEnterNextCloudLoginScreen() {
        navigateToWithBundle(R.id.action_enterNextCloudUrlScreen_to_loginNextCloudScreen)
    }

    fun navigateToNextCloudCreateFolderScreen() {
        navigateToWithBundle(R.id.action_loginNextCloudScreen_to_nextCloudNewFolderScreen)
    }

    fun actionNextCloudNewFolderScreenToSuccessfulScreen() {
        navigateToWithBundle(R.id.action_nextCloudNewFolderScreen_to_successfulSetServerFragment)
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

    fun navigateFromStartNearBySharingFragmentToConnectHotspotFragment() {
        navigateToWithBundle(R.id.action_startNearBySharingFragment_to_connectHotspotFragment)
    }

    fun navigateFromActionConnectHotspotScreenToQrCodeScreen() {
        navigateToWithBundle(R.id.action_connectHotspotScreen_to_qrCodeScreen)
    }

    fun navigateFromActionConnectHotspotScreenToScanQrCodeScreen() {
        navigateToWithBundle(R.id.action_startNearBySharingFragment_to_scanQrCodeScreen)
    }

    fun navigateFromScanQrCodeToDeviceInfo() {
        navigateToWithBundle(R.id.action_qrCodeScreen_to_deviceInfoScreen)
    }

    fun navigateFromScanQrCodeToPrepareUploadFragment() {
        navigateToWithBundle(R.id.action_scanQrCodeScreen_to_prepareUploadFragment)
    }

    fun navigateFromScanQrCodeToSenderManualConnectionScreen() {
        navigateToWithBundle(R.id.action_scanQrCodeScreen_to_senderManualConnectionScreen)
    }

    fun navigateFromSenderManualConnectionToConnectManuallyVerification() {
        navigateToWithBundle(R.id.action_senderManualConnectionScreen_to_connectManuallyVerificationScreen)
    }

    fun navigateFromDeviceInfoScreenTRecipientVerificationScreen() {
        navigateToWithBundle(R.id.action_deviceInfoScreen_to_recipientVerificationScreen)
    }

    fun navigateFromQrCodeScreenToWaitingReceiverFragment() {
        navigateToWithBundle(R.id.action_qrCodeScreen_to_waitingReceiverFragment)
    }

    fun navigateFromPrepareUploadFragmentToWaitingSenderFragment() {
        navigateToWithBundle(R.id.action_prepareUploadFragment_to_waitingSenderFragment)
    }

    fun navigateConnectManuallyVerificationFragmentToprepareUploadFragment() {
        navigateToWithBundle(R.id.action_connectManuallyVerificationFragment_to_prepareUploadFragment)
    }

    fun navigateFromRecipientVerificationScreenToWaitingReceiverFragment() {
        navigateToWithBundle(R.id.action_recipientVerificationScreen_to_waitingReceiverFragment)
    }

    fun navigateFromWaitingReceiverFragmentToRecipientSuccessFragment() {
        navigateToWithBundle(R.id.action_waitingReceiverFragment_to_recipientSuccessFragment)
    }

    fun navigateFromWaitingSenderFragmentToUploadFilesFragment() {
        navigateToWithBundle(R.id.action_waitingSenderFragment_to_uploadFilesFragment)
    } 

    fun navigateFromRecipientSuccessFragmentToRecipientUploadFilesFragment() {
        navigateToWithBundle(R.id.action_recipientSuccessFragment_to_recipientUploadFilesFragment)
    }

    fun navigateBackToStartNearBySharingFragmentAndClearBackStack() {
        navControllerProvider.navController.navigate(
            R.id.startNearBySharingFragment,
            bundle,
            navOptions {
                popUpTo(R.id.startNearBySharingFragment) {
                    inclusive = true
                }
            }
        )
    }
}