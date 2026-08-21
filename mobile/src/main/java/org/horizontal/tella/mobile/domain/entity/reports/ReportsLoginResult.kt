package org.horizontal.tella.mobile.domain.entity.reports

data class ReportsLoginResult(
    val accessToken: String = "",
    val user: User
)