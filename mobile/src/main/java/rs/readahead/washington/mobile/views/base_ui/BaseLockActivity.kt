package rs.readahead.washington.mobile.views.base_ui

import android.content.Intent
import com.hzontal.tella_locking_ui.ui.password.PasswordUnlockActivity
import com.hzontal.tella_locking_ui.ui.pattern.PatternSetActivity
import com.hzontal.tella_locking_ui.ui.pattern.PatternUnlockActivity
import com.hzontal.tella_locking_ui.ui.pin.PinUnlockActivity
import info.guardianproject.cacheword.SecretsManager
import org.hzontal.tella.keys.config.IUnlockRegistryHolder
import org.hzontal.tella.keys.config.UnlockRegistry
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.views.activity.PatternUpgradeActivity

abstract class BaseLockActivity : BaseActivity() {

    private val holder by lazy { applicationContext as IUnlockRegistryHolder }
    var isLocked = false
        private set

    private fun restrictActivity() {
        if (!MyApplication.getMainKeyStore().isStored) {
            startKeySetup()
        } else {
            isLocked = !MyApplication.getMainKeyHolder().exists()
            if (isLocked) {
                startUnlockingMainKey()
            }
        }
    }

    private fun startKeySetup() {
        val intent = Intent(this, if (SecretsManager.isInitialized(this)) PatternUpgradeActivity::class.java else PatternSetActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        this.startActivity(intent)
    }

    private fun startUnlockingMainKey() {
        val intent = when (holder.unlockRegistry.getActiveMethod(this)) {
            UnlockRegistry.Method.TELLA_PIN -> {
                Intent(this, PinUnlockActivity::class.java)
            }
            UnlockRegistry.Method.TELLA_PATTERN -> {
                Intent(this, PatternUnlockActivity::class.java)
            }
            UnlockRegistry.Method.TELLA_PASSWORD -> {
                Intent(this, PasswordUnlockActivity::class.java)
            }
            else -> {
                Intent(this, PatternUnlockActivity::class.java)
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        this.startActivity(intent)
        finish()
    }

    override fun onResume() {
        restrictActivity()
        super.onResume()
    }
}