package org.horizontal.tella.mobile.views.activity.viewer

import com.hzontal.tella_vault.VaultFile
import org.horizontal.tella.mobile.R
import org.junit.Assert.assertEquals
import org.junit.Test

class VerificationHelpRowsTest {

    @Test
    fun titles_matchEachCategory() {
        assertEquals(R.string.verification_help_file_bar, VerificationHelpRows.titleRes(VerificationCategory.FILE))
        assertEquals(R.string.verification_help_device_bar, VerificationHelpRows.titleRes(VerificationCategory.DEVICE))
        assertEquals(R.string.verification_help_network_bar, VerificationHelpRows.titleRes(VerificationCategory.NETWORK))
        assertEquals(R.string.verification_help_location_bar, VerificationHelpRows.titleRes(VerificationCategory.LOCATION))
        assertEquals(R.string.verification_help_other_bar, VerificationHelpRows.titleRes(VerificationCategory.ENVIRONMENT))
    }

    @Test
    fun fileHelp_listsFileFieldsInOrder() {
        assertEquals(
            listOf(
                R.string.verification_info_field_filename,
                R.string.Vault_FileSize,
                R.string.Vault_FileFormat,
                R.string.Vault_FileCreated,
                R.string.verification_file_last_modified,
                R.string.Vault_FileResolution,
                R.string.verification_file_folder,
                R.string.verification_info_field_hash,
                R.string.verification_file_info_created
            ),
            labels(VerificationCategory.FILE)
        )
    }

    @Test
    fun deviceHelp_listsDeviceFieldsInOrder() {
        assertEquals(
            listOf(
                R.string.verification_info_field_manufacturer,
                R.string.verification_device_model,
                R.string.verification_info_field_device_id,
                R.string.verification_info_field_screen_size,
                R.string.verification_device_wifi_mac_address,
                R.string.verification_info_field_language,
                R.string.verification_device_region
            ),
            labels(VerificationCategory.DEVICE)
        )
    }

    @Test
    fun networkHelp_listsNetworkFieldsInOrder() {
        assertEquals(
            listOf(
                R.string.verification_info_field_connection_status,
                R.string.verification_network_connected_to,
                R.string.verification_network_mobile_type,
                R.string.verification_network_ipv4_address,
                R.string.verification_network_ipv6_address,
                R.string.verification_network_nearby_cells,
                R.string.verification_network_nearby_wifi
            ),
            labels(VerificationCategory.NETWORK)
        )
    }

    @Test
    fun locationHelp_listsLocationFieldsInOrder() {
        assertEquals(
            listOf(
                R.string.verification_info_field_location_provider,
                R.string.verification_info_field_location_latitude,
                R.string.verification_info_field_location_longitude,
                R.string.verification_info_field_location_altitude,
                R.string.verification_info_field_location_accuracy,
                R.string.verification_location_speed,
                R.string.verification_location_recorded_at
            ),
            labels(VerificationCategory.LOCATION)
        )
    }

    @Test
    fun environmentHelp_listsTemperatureAndLight() {
        assertEquals(
            listOf(
                R.string.verification_info_field_ambient_temperature,
                R.string.verification_info_field_light
            ),
            labels(VerificationCategory.ENVIRONMENT)
        )
    }

    @Test
    fun helpRows_matchMetadataRowsForEveryCategory() {
        VerificationCategory.values().forEach { category ->
            assertEquals(
                "Help and metadata rows differ for $category",
                VerificationMetadataRows.rows(VaultFile(), category).map { it.labelRes },
                labels(category)
            )
        }
    }

    private fun labels(category: VerificationCategory): List<Int> =
        VerificationHelpRows.rows(category).map { it.labelRes }
}
