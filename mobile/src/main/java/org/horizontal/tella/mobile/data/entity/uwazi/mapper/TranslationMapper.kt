package org.horizontal.tella.mobile.data.entity.uwazi.mapper

import org.horizontal.tella.mobile.data.entity.uwazi.TranslationContextEntity
import org.horizontal.tella.mobile.data.entity.uwazi.TranslationResponse
import org.horizontal.tella.mobile.data.entity.uwazi.TranslationRowEntity
import org.horizontal.tella.mobile.domain.entity.uwazi.TranslationContext
import org.horizontal.tella.mobile.domain.entity.uwazi.TranslationRow

fun TranslationResponse.mapToDomainModel() = rows?.map {
    it.mapToDomainModel()
} ?: emptyList()

fun TranslationRowEntity.mapToDomainModel() = TranslationRow(
    _id = _id?: "",
    contexts = contexts?.map { it.mapToDomainModel() } ?: emptyList(),
    locale = locale?:"")

fun TranslationContextEntity.mapToDomainModel() = TranslationContext(
    _id = _id?:"",
    id = id?:"",
    label = label?:"",
    type = type?:"",
    values = values ?: mapOf()
)