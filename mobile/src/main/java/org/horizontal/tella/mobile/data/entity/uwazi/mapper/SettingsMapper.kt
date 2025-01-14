package org.horizontal.tella.mobile.data.entity.uwazi.mapper

import org.horizontal.tella.mobile.data.entity.uwazi.LanguageEntity
import org.horizontal.tella.mobile.data.entity.uwazi.SettingsResponse
import org.horizontal.tella.mobile.domain.entity.uwazi.Language
import org.horizontal.tella.mobile.domain.entity.uwazi.Settings

fun SettingsResponse.mapToDomainModel() = Settings(
    serverName = site_name ?: "",
    languages = languages?.map { it.mapToDomainModel() } ?: emptyList(),
    allowedPublicTemplates = allowedPublicTemplates?.map { it } ?: emptyList()
)

fun LanguageEntity.mapToDomainModel() = Language(
    _id = _id?: "",
    default = default?: false,
    key = key?: "",
    label = label?: ""
)