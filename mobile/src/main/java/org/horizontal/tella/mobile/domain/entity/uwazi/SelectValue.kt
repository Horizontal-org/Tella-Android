package org.horizontal.tella.mobile.domain.entity.uwazi

data class SelectValue(
    val _id: String? = null,
    val id: String,
    val label: String,
    var translatedLabel: String = "",
    val values : List<NestedSelectValue>
)