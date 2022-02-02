package rs.readahead.washington.mobile.views.fragment.uwazi.send

import rs.readahead.washington.mobile.presentation.uwazi.*
import rs.readahead.washington.mobile.presentation.uwazi.SendEntityRequest

object  MockUwaziData {

    fun getEntityVictimRowMock() : SendEntityRequest {
        return SendEntityRequest(
            attachments = null,
            metadata = getVictimEntityMetaData(),
            template = "618eae19c2397c2604f9aec0",
            title = "Test victim from device Ahlem 222",
            type = "entity"
        )
    }

    private fun getVictimEntityMetaData() : Metadata {
        return Metadata(
            age = listOf(UwaziValue(10)),
            comments = listOf(UwaziValue("This is a test from Ahlem device with metadata")),
            country_of_origin = listOf(UwaziValue("nnp9g81v7ft")),
            date_of_birth = listOf(UwaziValue(727488000)),
            gender = listOf(UwaziValue("zdtrr564qz")),
            internal_code = listOf(UwaziValue("FTF7424-6551")),
            multiple_periods = listOf(),
            period_1 = listOf(),
            picture = listOf(),
            place_where_found_geolocation = listOf(),
            profile = listOf(UwaziValue("mye8embmvlm")),
            video = listOf(),
            visiting_dates = listOf()
        )
    }
}