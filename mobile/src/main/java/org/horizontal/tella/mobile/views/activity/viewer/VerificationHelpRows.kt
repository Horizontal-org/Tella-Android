package org.horizontal.tella.mobile.views.activity.viewer

import androidx.annotation.StringRes
import org.horizontal.tella.mobile.R

data class VerificationHelpField(
    @StringRes val labelRes: Int,
    @StringRes val explanationRes: Int
)

object VerificationHelpRows {

    @StringRes
    fun titleRes(category: VerificationCategory): Int {
        return when (category) {
            VerificationCategory.FILE -> R.string.verification_help_file_bar
            VerificationCategory.DEVICE -> R.string.verification_help_device_bar
            VerificationCategory.NETWORK -> R.string.verification_help_network_bar
            VerificationCategory.LOCATION -> R.string.verification_help_location_bar
            VerificationCategory.OTHER -> R.string.verification_help_other_bar
        }
    }

    fun rows(category: VerificationCategory): List<VerificationHelpField> {
        return when (category) {
            VerificationCategory.FILE -> fileRows()
            VerificationCategory.DEVICE -> deviceRows()
            VerificationCategory.NETWORK -> networkRows()
            VerificationCategory.LOCATION -> locationRows()
            VerificationCategory.OTHER -> otherRows()
        }
    }

    private fun fileRows(): List<VerificationHelpField> {
        return listOf(
            VerificationHelpField(
                R.string.verification_info_field_filename,
                R.string.verification_info_filename_expl
            ),
            VerificationHelpField(
                R.string.verification_info_field_file_path,
                R.string.verification_info_file_path_expl
            ),
            VerificationHelpField(
                R.string.verification_info_field_hash,
                R.string.verification_info_hash_expl
            ),
            VerificationHelpField(
                R.string.verification_info_field_file_modified,
                R.string.verification_info_date_time_modified_expl
            ),
            VerificationHelpField(
                R.string.verification_info_field_proof_generated,
                R.string.verification_info_proof_generated_expl
            )
        )
    }

    private fun deviceRows(): List<VerificationHelpField> {
        return listOf(
            VerificationHelpField(
                R.string.verification_info_field_manufacturer,
                R.string.verification_info_manufacturer_expl
            ),
            VerificationHelpField(
                R.string.verification_info_field_hardware,
                R.string.verification_info_device_model_expl
            ),
            VerificationHelpField(
                R.string.verification_info_field_device_id,
                R.string.verification_info_device_id_expl
            ),
            VerificationHelpField(
                R.string.verification_info_field_screen_size,
                R.string.verification_info_screen_size_expl
            ),
            VerificationHelpField(
                R.string.verification_info_field_wifi_mac,
                R.string.verification_info_wifi_mac_expl
            ),
            VerificationHelpField(
                R.string.verification_info_field_language,
                R.string.verification_info_language_expl
            ),
            VerificationHelpField(
                R.string.verification_info_field_locale,
                R.string.verification_info_locale_expl
            )
        )
    }

    private fun networkRows(): List<VerificationHelpField> {
        return listOf(
            VerificationHelpField(
                R.string.verification_info_field_connection_status,
                R.string.verification_info_connection_status_expl
            ),
            VerificationHelpField(
                R.string.verification_info_field_network_type,
                R.string.verification_info_network_type_expl
            ),
            VerificationHelpField(
                R.string.verification_info_field_data_type,
                R.string.verification_info_data_type_expl
            ),
            VerificationHelpField(
                R.string.verification_info_field_ipv4,
                R.string.verification_info_ipv4_expl
            ),
            VerificationHelpField(
                R.string.verification_info_field_ipv6,
                R.string.verification_info_ipv6_expl
            ),
            VerificationHelpField(
                R.string.verification_info_field_cell_towers,
                R.string.verification_info_cell_towers_expl
            ),
            VerificationHelpField(
                R.string.verification_info_wifi,
                R.string.verification_info_wifi_expl
            )
        )
    }

    private fun locationRows(): List<VerificationHelpField> {
        return listOf(
            VerificationHelpField(
                R.string.verification_info_field_location_provider,
                R.string.verification_info_location_provider_expl
            ),
            VerificationHelpField(
                R.string.verification_info_field_location_latitude,
                R.string.verification_info_location_latitude_expl
            ),
            VerificationHelpField(
                R.string.verification_info_field_location_longitude,
                R.string.verification_info_location_longitude_expl
            ),
            VerificationHelpField(
                R.string.verification_info_field_location_altitude,
                R.string.verification_info_location_altitude_expl
            ),
            VerificationHelpField(
                R.string.verification_info_field_location_accuracy,
                R.string.verification_info_location_accuracy_expl
            ),
            VerificationHelpField(
                R.string.verification_info_field_location_speed,
                R.string.verification_info_location_speed_expl
            ),
            VerificationHelpField(
                R.string.verification_info_field_location_time_value,
                R.string.verification_info_location_time_expl
            )
        )
    }

    private fun otherRows(): List<VerificationHelpField> {
        return listOf(
            VerificationHelpField(
                R.string.verification_info_field_ambient_temperature,
                R.string.verification_info_ambient_temperature_expl
            ),
            VerificationHelpField(
                R.string.verification_info_field_light,
                R.string.verification_info_light_expl
            )
        )
    }
}
