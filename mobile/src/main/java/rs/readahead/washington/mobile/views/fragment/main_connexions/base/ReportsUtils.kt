package rs.readahead.washington.mobile.views.fragment.main_connexions.base

import android.content.Context
import com.google.gson.Gson
import org.hzontal.shared_ui.utils.DialogUtils
import rs.readahead.washington.mobile.domain.entity.googledrive.Config
import rs.readahead.washington.mobile.views.base_ui.BaseActivity
import java.io.InputStreamReader

object ReportsUtils {

    fun showReportDeletedSnackBar(message: String, activity: BaseActivity) {
        DialogUtils.showBottomMessage(
            activity,
            message,
            false
        )
    }

    fun loadConfig(context: Context): Config {
        val inputStream = context.assets.open("config.json")
        val reader = InputStreamReader(inputStream)
        return Gson().fromJson(reader, Config::class.java)
    }
}