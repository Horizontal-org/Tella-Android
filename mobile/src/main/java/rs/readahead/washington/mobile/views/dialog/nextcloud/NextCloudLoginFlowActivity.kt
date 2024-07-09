package rs.readahead.washington.mobile.views.dialog.nextcloud

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity
import rs.readahead.washington.mobile.views.dialog.IS_UPDATE_SERVER

@AndroidEntryPoint
class NextCloudLoginFlowActivity : BaseLockActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_uwazi_connect_flow)

        if (!intent.getBooleanExtra(IS_UPDATE_SERVER, false)) {
            val navHostFragment =
                supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val navController = navHostFragment.navController
            navController.navigate(R.id.nextcloud_settings)
        } else {
            //TODO EDIT SCREEN
        }
    }

}