package rs.readahead.washington.mobile.data.entity.uwazi.mapper

import rs.readahead.washington.mobile.data.entity.uwazi.LanguageEntity
import rs.readahead.washington.mobile.data.entity.uwazi.SettingsResponse
import rs.readahead.washington.mobile.domain.entity.uwazi.Language
import rs.readahead.washington.mobile.domain.entity.uwazi.Settings

fun SettingsResponse.mapToDomainModel() = Settings(
    serverName = site_name ?: "",
    languages = languages?.map { it.mapToDomainModel() } ?: emptyList()
)

fun LanguageEntity.mapToDomainModel() = Language(
    _id = _id?: "",
    default = default?: false,
    key = key?: "",
    label = label?: ""
)