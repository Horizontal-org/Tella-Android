package rs.readahead.washington.mobile.views.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import com.hzontal.tella_vault.Metadata
import com.hzontal.tella_vault.MyLocation
import com.hzontal.tella_vault.VaultFile
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.util.StringUtils
import rs.readahead.washington.mobile.util.Util
import rs.readahead.washington.mobile.views.base_ui.BaseFragment
import timber.log.Timber


const val METADATA_FILE_ARG = "METADATA_FILE_ARG"
class MetadataViewerFragment: BaseFragment() {
    lateinit var vaultFile: VaultFile
    lateinit var metadataList: LinearLayout
    //lateinit var metadata: Metadata

    private var metadata: Metadata? = null
    fun newInstance(vaultFile: VaultFile): MetadataViewerFragment {
        val args = Bundle()
        args.putSerializable(METADATA_FILE_ARG,vaultFile)
        val fragment = MetadataViewerFragment()
        fragment.arguments = args
        return fragment
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_metadata_viewer, container, false)
        initView(view)
        return view
    }

    override fun initView(view: View) {
        metadataList = view.findViewById(R.id.metadata_list)


        arguments?.getSerializable(METADATA_FILE_ARG)?.let {
            (it as VaultFile).apply {
                Timber.d("++++ metadata> %s, device id %s", it.path, it.name)
                vaultFile = it
                metadata = it.metadata
            }
        }
        showMetadata()
    }

    @SuppressLint("StringFormatInvalid")
    private fun showMetadata() {
        if (metadata == null) {
            Timber.d("++++ metadata is null")
            return
        }
        Timber.d("++++ metadata> %s, device id %s", metadata.toString(), metadata!!.deviceID)
        metadataList.addView(createMetadataTitle(R.string.verification_info_subheading_file_metadata))
        metadataList.addView(
            createMetadataItem(
                if (metadata!!.getFileName() != null) metadata!!.getFileName() else vaultFile.name,
                resources.getString(R.string.verification_info_field_filename)
            )
        )
        metadataList.addView(
            createMetadataItem(
                vaultFile.path,
                resources.getString(R.string.verification_info_field_file_path)
            )
        )
        metadataList.addView(
            createMetadataItem(
                if (vaultFile.hash != null) vaultFile.hash else metadata!!.getFileHashSHA256(),
                resources.getString(R.string.verification_info_field_hash)
            )
        )
        metadataList.addView(
            createMetadataItem(
                Util.getDateTimeString(vaultFile.created, "dd-MM-yyyy HH:mm:ss Z"),
                resources.getString(R.string.verification_info_field_file_modified)
            )
        )
        metadataList.addView(createMetadataLine())
        metadataList.addView(createMetadataTitle(R.string.verification_info_subheading_device_metadata))
        metadataList.addView(
            createMetadataItem(
                metadata!!.getManufacturer(),
                resources.getString(R.string.verification_info_field_manufacturer)
            )
        )
        metadataList.addView(
            createMetadataItem(
                metadata!!.getHardware(),
                resources.getString(R.string.verification_info_field_hardware)
            )
        )
        metadataList.addView(
            createMetadataItem(
                metadata!!.getDeviceID(),
                resources.getString(R.string.verification_info_field_device_id)
            )
        )
        metadataList.addView(
            createMetadataItem(
                metadata!!.getScreenSize() + resources.getString(R.string.inches),
                resources.getString(R.string.verification_info_field_screen_size)
            )
        )
        metadataList.addView(
            createMetadataItem(
                metadata!!.getLanguage(),
                resources.getString(R.string.verification_info_field_language)
            )
        )
        metadataList.addView(
            createMetadataItem(
                metadata!!.getLocale(),
                resources.getString(R.string.verification_info_field_locale)
            )
        )
        metadataList.addView(
            createMetadataItem(
                metadata!!.getNetwork(),
                resources.getString(R.string.verification_info_field_connection_status)
            )
        )
        metadataList.addView(
            createMetadataItem(
                metadata!!.getNetworkType(),
                resources.getString(R.string.verification_info_field_network_type)
            )
        )
        metadataList.addView(
            createMetadataItem(
                metadata!!.getWifiMac(),
                resources.getString(R.string.verification_info_field_wifi_mac)
            )
        )
        metadataList.addView(
            createMetadataItem(
                metadata!!.getIPv4(),
                resources.getString(R.string.verification_info_field_ipv4)
            )
        )
        metadataList.addView(
            createMetadataItem(
                metadata!!.getIPv6(),
                resources.getString(R.string.verification_info_field_ipv6)
            )
        )
        metadataList.addView(createMetadataLine())
        metadataList.addView(createMetadataTitle(R.string.verification_info_subheading_context_metadata))
        if (metadata!!.getMyLocation() != null) {
            metadataList.addView(
                createMetadataItem(
                    getLocationString(metadata!!.getMyLocation()),
                    resources.getString(R.string.verification_info_field_location)
                )
            )
            metadataList.addView(
                createMetadataItem(
                    metadata!!.getMyLocation().getProvider(),
                    resources.getString(R.string.verification_info_field_location_provider)
                )
            )
            metadataList.addView(
                createMetadataItem(
                    getString(R.string.meter_per_second, metadata!!.getMyLocation().getSpeed()),
                    resources.getString(R.string.verification_info_field_location_speed)
                )
            )
        } else {
            metadataList.addView(
                createMetadataItem(
                    getString(R.string.verification_info_field_metadata_not_available),
                    resources.getString(R.string.verification_info_field_location)
                )
            )
        }
        val cells = StringUtils.join(", ", metadata!!.getCells())
        metadataList.addView(
            createMetadataItem(
                cells,
                resources.getString(R.string.verification_info_field_cell_towers)
            )
        )
        metadataList.addView(
            createMetadataItem(
                if (metadata!!.getWifis() != null) TextUtils.join(
                    ", ",
                    metadata!!.getWifis()
                ) else getString(R.string.verification_info_field_metadata_not_available),
                getString(R.string.verification_info_wifi)
            )
        )
    }

    @SuppressLint("StringFormatInvalid")
    private fun getLocationString(myLocation: MyLocation): String? {
        return """
            ${getString(R.string.verification_info_field_latitude)}${myLocation.latitude}
            ${getString(R.string.verification_info_field_longitude)}${myLocation.longitude}
            ${getString(R.string.verification_info_field_altitude)}${
            getString(
                R.string.meter,
                myLocation.altitude
            )
        }
            ${getString(R.string.verification_info_field_accuracy)}${
            getString(
                R.string.meter,
                myLocation.accuracy
            )
        }
            ${getString(R.string.verification_info_field_location_time)}${
            Util.getDateTimeString(
                myLocation.timestamp,
                "dd-MM-yyyy HH:mm:ss Z"
            )
        }
            """.trimIndent()
    }

    private fun createMetadataTitle(@StringRes titleResId: Int): View {
        @SuppressLint("InflateParams") val textView = LayoutInflater.from(requireContext())
            .inflate(R.layout.metadata_header, null) as TextView
        textView.setText(titleResId)
        return textView
    }

    private fun createMetadataLine(): LinearLayout {
        return LayoutInflater.from(requireContext())
            .inflate(R.layout.metadata_line, null) as LinearLayout
    }

    private fun createMetadataItem(value: CharSequence?, name: String): View {
        @SuppressLint("InflateParams") val layout = LayoutInflater.from(requireContext())
            .inflate(R.layout.metadata_item, null) as LinearLayout
        val dataName = layout.findViewById<TextView>(R.id.name)
        val dataValue = layout.findViewById<TextView>(R.id.data)
        dataName.text = name
        if (value == null || value.length < 1) {
            dataValue.setText(R.string.verification_info_field_metadata_not_available)
        } else {
            dataValue.text = value
        }
        return layout
    }

    private fun startMetadataHelp() {
        // startActivity(Intent(this@MetadataViewerActivity, MetadataHelpActivity::class.java))
    }
}