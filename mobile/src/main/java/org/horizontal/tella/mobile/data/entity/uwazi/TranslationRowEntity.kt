package org.horizontal.tella.mobile.data.entity.uwazi

data class TranslationRowEntity(
    val _id: String?,
    val contexts: List<TranslationContextEntity>?,
    val locale: String?
)