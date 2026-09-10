package org.horizontal.tella.mobile.views.activity.viewer

import androidx.annotation.StringRes
import com.hzontal.tella_vault.Metadata
import com.hzontal.tella_vault.MyLocation
import com.hzontal.tella_vault.VaultFile
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.util.Util

data class VerificationField(
    @StringRes val labelRes: Int,
    val value: String?
)

object VerificationMetadataRows {
    private const val DATE_FORMAT = "dd-MM-yyyy HH:mm:ss Z"

    fun rows(vaultFile: VaultFile, category: VerificationCategory): List<VerificationField> {
        val metadata = vaultFile.metadata
        return when (category) {
            VerificationCategory.FILE -> fileRows(vaultFile, metadata)
            VerificationCategory.DEVICE -> deviceRows(metadata)
            VerificationCategory.NETWORK -> networkRows(metadata)
            VerificationCategory.LOCATION -> locationRows(metadata?.myLocation)
            VerificationCategory.OTHER -> otherRows(metadata)
        }
    }

    private fun fileRows(vaultFile: VaultFile, metadata: Metadata?): List<VerificationField> {
        return listOf(
            VerificationField(
                R.string.verification_info_field_hash_sha256,
                vaultFile.hash ?: metadata?.fileHashSHA256
            ),
            VerificationField(
                R.string.verification_info_field_file_created,
                formatTimestamp(vaultFile.created)
            ),
            VerificationField(
                R.string.verification_info_field_file_modified,
                formatTimestamp(vaultFile.created)
            ),
            VerificationField(
                R.string.verification_info_field_file_path,
                vaultFile.path
            ),
            VerificationField(
                R.string.verification_info_field_proof_generated,
                metadata?.timestamp?.takeIf { it > 0 }?.let { formatTimestamp(it) }
            )
        )
    }

    private fun deviceRows(metadata: Metadata?): List<VerificationField> {
        return listOf(
            VerificationField(R.string.verification_info_field_hardware, metadata?.hardware),
            VerificationField(R.string.verification_info_field_manufacturer, metadata?.manufacturer),
            VerificationField(R.string.verification_info_field_device_id, metadata?.deviceID),
            VerificationField(R.string.verification_info_field_wifi_mac, metadata?.wifiMac),
            VerificationField(R.string.verification_info_field_screen_size, metadata?.screenSize)
        )
    }

    private fun networkRows(metadata: Metadata?): List<VerificationField> {
        val cells = metadata?.cells
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(", ")
        val wifis = metadata?.wifis
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(", ")
        return listOf(
            VerificationField(R.string.verification_info_field_network, metadata?.network),
            VerificationField(R.string.verification_info_field_network_type, metadata?.networkType),
            VerificationField(R.string.verification_info_field_data_type, metadata?.dataType),
            VerificationField(R.string.verification_info_field_cell_towers, cells),
            VerificationField(R.string.verification_info_field_ipv4, metadata?.getIPv4()),
            VerificationField(R.string.verification_info_field_ipv6, metadata?.getIPv6()),
            VerificationField(R.string.verification_info_wifi, wifis)
        )
    }

    private fun locationRows(location: MyLocation?): List<VerificationField> {
        if (location == null || location.isEmpty) {
            return listOf(
                VerificationField(R.string.verification_info_field_location_provider, null),
                VerificationField(R.string.verification_info_field_location_latitude, null),
                VerificationField(R.string.verification_info_field_location_longitude, null),
                VerificationField(R.string.verification_info_field_location_accuracy, null),
                VerificationField(R.string.verification_info_field_location_speed, null),
                VerificationField(R.string.verification_info_field_location_altitude, null),
                VerificationField(R.string.verification_info_field_location_time_value, null)
            )
        }
        return listOf(
            VerificationField(R.string.verification_info_field_location_provider, location.provider),
            VerificationField(
                R.string.verification_info_field_location_latitude,
                location.latitude.toString()
            ),
            VerificationField(
                R.string.verification_info_field_location_longitude,
                location.longitude.toString()
            ),
            VerificationField(
                R.string.verification_info_field_location_accuracy,
                numberString(location.accuracy)
            ),
            VerificationField(
                R.string.verification_info_field_location_speed,
                numberString(location.speed)
            ),
            VerificationField(
                R.string.verification_info_field_location_altitude,
                numberString(location.altitude)
            ),
            VerificationField(
                R.string.verification_info_field_location_time_value,
                location.timestamp.takeIf { it > 0 }?.toString()
            )
        )
    }

    private fun otherRows(metadata: Metadata?): List<VerificationField> {
        return listOf(
            VerificationField(R.string.verification_info_field_language, metadata?.language),
            VerificationField(R.string.verification_info_field_locale, metadata?.locale),
            VerificationField(R.string.verification_info_field_notes, metadata?.notes)
        )
    }

    private fun formatTimestamp(timestamp: Long): String? {
        if (timestamp <= 0) {
            return null
        }
        return Util.getDateTimeString(timestamp, DATE_FORMAT)
    }

    private fun numberString(value: Number?): String? {
        if (value == null) {
            return null
        }
        val asDouble = value.toDouble()
        return if (asDouble % 1.0 == 0.0) {
            asDouble.toLong().toString()
        } else {
            value.toString()
        }
    }
}
