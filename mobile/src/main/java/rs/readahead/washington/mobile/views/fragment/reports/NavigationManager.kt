package rs.readahead.washington.mobile.views.fragment.reports

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import rs.readahead.washington.mobile.R


object NavigationManager {
    fun navigateTo(navController: NavController,destinationId: Int) {
        navController.navigate(destinationId)
    }

    fun navigateTo(navController: NavController,directions: NavDirections) {
        navController.navigate(directions)
    }
    fun navigateTo(navController: NavController,destinationId: Int,bundle:Bundle) {
        navController.navigate(destinationId,bundle)
    }

    fun navigateToMicro(navController: NavController,bundle:Bundle) {
        navController.navigate(R.id.mic,bundle)
    }
    fun navigateToNewReportScreen(navController: NavController,bundle:Bundle?) {
        navController.navigate(R.id.action_reportsScreen_to_newReport_screen,bundle)
    }

}