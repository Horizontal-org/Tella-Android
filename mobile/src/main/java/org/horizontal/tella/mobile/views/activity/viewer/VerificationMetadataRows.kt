package org.horizontal.tella.mobile.views.activity.viewer

import androidx.annotation.StringRes
import com.hzontal.tella_vault.Metadata
import com.hzontal.tella_vault.MyLocation
import com.hzontal.tella_vault.VaultFile
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.util.FileUtil
import java.util.Locale
import org.horizontal.tella.mobile.util.MetadataUtils
import org.horizontal.tella.mobile.util.Util
import org.horizontal.tella.mobile.util.VaultFolderPath

data class VerificationField(
    @StringRes val labelRes: Int,
    val value: String?,
    @StringRes val valueRes: Int? = null
)

object VerificationMetadataRows {
    private const val DATE_FORMAT = "dd-MM-yyyy HH:mm:ss Z"

    @JvmOverloads
    fun rows(
        vaultFile: VaultFile,
        category: VerificationCategory,
        fileLocation: String? = null,
        resolution: String? = null
    ): List<VerificationField> {
        val metadata = vaultFile.metadata
        return when (category) {
            VerificationCategory.FILE -> fileRows(vaultFile, metadata, fileLocation, resolution)
            VerificationCategory.DEVICE -> deviceRows(metadata)
            VerificationCategory.NETWORK -> networkRows(metadata)
            VerificationCategory.LOCATION -> locationRows(metadata?.myLocation)
            VerificationCategory.ENVIRONMENT -> environmentRows(metadata)
        }
    }

    private fun fileRows(
        vaultFile: VaultFile,
        metadata: Metadata?,
        fileLocation: String?,
        resolution: String?
    ): List<VerificationField> {
        return listOf(
            VerificationField(
                R.string.verification_info_field_filename,
                vaultFile.name?.takeIf { it.isNotBlank() } ?: metadata?.fileName
            ),
            VerificationField(R.string.Vault_FileSize, FileUtil.getFileSizeString(vaultFile.size)),
            VerificationField(R.string.Vault_FileFormat, fileFormat(vaultFile)),
            VerificationField(
                R.string.Vault_FileCreated,
                formatTimestamp(vaultFile.created)
            ),
            VerificationField(
                R.string.verification_file_last_modified,
                formatTimestamp(if (vaultFile.modified > 0) vaultFile.modified else vaultFile.created)
            ),
            VerificationField(R.string.Vault_FileResolution, resolution),
            VerificationField(
                R.string.verification_file_folder,
                fileLocation?.takeIf { it.isNotBlank() } ?: VaultFolderPath.ROOT
            ),
            VerificationField(
                R.string.verification_info_field_hash,
                vaultFile.hash ?: metadata?.fileHashSHA256
            ),
            VerificationField(
                R.string.verification_file_info_created,
                metadata?.timestamp?.let { formatTimestamp(it) }
            )
        )
    }

    private fun fileFormat(file: VaultFile): String? {
        val subtype = file.mimeType?.substringAfter('/', "")?.takeIf {
            it.isNotBlank() && it != "octet-stream"
        }
        return (subtype ?: file.name?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() })
            ?.uppercase(Locale.ROOT)
    }

    private fun deviceRows(metadata: Metadata?): List<VerificationField> {
        return listOf(
            VerificationField(R.string.verification_info_field_manufacturer, metadata?.manufacturer),
            VerificationField(R.string.verification_device_model, metadata?.hardware),
            VerificationField(R.string.verification_info_field_device_id, metadata?.deviceID),
            VerificationField(
                R.string.verification_info_field_screen_size,
                MetadataUtils.formatScreenSize(metadata?.screenSize?.takeUnless { it.isBlank() || it == "-1" })
            ),
            VerificationField(
                R.string.verification_device_wifi_mac_address,
                metadata?.wifiMac?.takeUnless {
                    it.isBlank() || it == "02:00:00:00:00:00" || it == "2:0:0:0:0:0"
                }
            ),
            VerificationField(R.string.verification_info_field_language, metadata?.language),
            VerificationField(R.string.verification_device_region, metadata?.locale)
        )
    }

    private fun networkRows(metadata: Metadata?): List<VerificationField> {
        val networkType = metadata?.networkType?.trim()
        val wifi = networkType.equals("WiFi", ignoreCase = true) ||
            networkType.equals("Wi-Fi", ignoreCase = true)
        val cellular = networkType?.contains("Mobile Data", ignoreCase = true) == true
        val notOnMobileData = metadata?.dataType.isNullOrBlank() &&
            (wifi || networkType.equals("No network", ignoreCase = true))
        val cells = metadata?.cells
            ?.filter { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(",\n") { "[${it.trim().removeSurrounding("[", "]")}]" }
        val wifis = metadata?.wifis
            ?.filter { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(", ")
        return listOf(
            VerificationField(R.string.verification_info_field_connection_status, metadata?.network),
            VerificationField(
                R.string.verification_network_connected_to,
                networkType,
                when {
                    wifi -> R.string.verification_network_wifi
                    cellular -> R.string.verification_network_cellular
                    else -> null
                }
            ),
            VerificationField(
                R.string.verification_network_mobile_type,
                metadata?.dataType,
                if (notOnMobileData) R.string.verification_network_not_mobile else null
            ),
            VerificationField(R.string.verification_network_ipv4_address, metadata?.getIPv4()),
            VerificationField(R.string.verification_network_ipv6_address, metadata?.getIPv6()),
            VerificationField(R.string.verification_network_nearby_cells, cells),
            VerificationField(R.string.verification_network_nearby_wifi, wifis)
        )
    }

    private fun locationRows(location: MyLocation?): List<VerificationField> {
        val loc = location?.takeUnless { it.isEmpty }
        return listOf(
            VerificationField(
                R.string.verification_info_field_location_provider,
                providerString(loc?.provider)
            ),
            VerificationField(
                R.string.verification_info_field_location_latitude,
                numberString(loc?.latitude)
            ),
            VerificationField(
                R.string.verification_info_field_location_longitude,
                numberString(loc?.longitude)
            ),
            VerificationField(
                R.string.verification_info_field_location_altitude,
                unitString(loc?.altitude, "m")
            ),
            VerificationField(
                R.string.verification_info_field_location_accuracy,
                unitString(loc?.accuracy, "m")
            ),
            VerificationField(
                R.string.verification_location_speed,
                unitString(loc?.speed, "m/s")
            ),
            VerificationField(
                R.string.verification_location_recorded_at,
                loc?.timestamp?.let { formatTimestamp(it) }
            )
        )
    }

    private fun environmentRows(metadata: Metadata?): List<VerificationField> {
        return listOf(
            VerificationField(
                R.string.verification_info_field_ambient_temperature,
                numberString(metadata?.ambientTemperature)
            ),
            VerificationField(
                R.string.verification_info_field_light,
                numberString(metadata?.light)
            )
        )
    }

    private fun formatTimestamp(timestamp: Long): String? {
        if (timestamp <= 0) {
            return null
        }
        return Util.getDateTimeString(timestamp, DATE_FORMAT)
    }

    private fun providerString(provider: String?): String? {
        return when {
            provider.isNullOrBlank() -> null
            provider.equals("gps", ignoreCase = true) -> "GPS"
            else -> provider
        }
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

    private fun unitString(value: Number?, unit: String): String? {
        val number = numberString(value) ?: return null
        return "$number $unit"
    }
}
