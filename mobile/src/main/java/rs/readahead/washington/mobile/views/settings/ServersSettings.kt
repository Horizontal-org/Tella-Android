package rs.readahead.washington.mobile.views.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils
import rs.readahead.washington.mobile.R


class ServersSettings : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_servers_settings, container, false)
        (activity as OnFragmentSelected?)?.setToolbarLabel(R.string.settings_servers_title_server_settings)

        val addServerButton = view.findViewById<LinearLayout>(R.id.add_servers)
        addServerButton.setOnClickListener {
            activity?.let {
                BottomSheetUtils.showDualChoiceTypeSheet(it.supportFragmentManager,
                    "Add Server",
                    descriptionText = "What type of server?",
                    buttonOneLabel = "FORMS (OPEN DATA KIT)",
                    buttonTwoLabel = "REPORTS (DIRECT UPLOAD)",
                    onActionOneClick = {},
                    onActionTwoClick = {},
                    onCancelClick = {})
            }
        }

        return view
    }
}