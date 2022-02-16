package rs.readahead.washington.mobile.data.entity.uwazi.mapper

import rs.readahead.washington.mobile.data.entity.uwazi.TranslationContextEntity
import rs.readahead.washington.mobile.data.entity.uwazi.TranslationResponse
import rs.readahead.washington.mobile.data.entity.uwazi.TranslationRowEntity
import rs.readahead.washington.mobile.domain.entity.uwazi.TranslationContext
import rs.readahead.washington.mobile.domain.entity.uwazi.TranslationRow

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