package org.horizontal.tella.mobile.data.entity.reports

data class ReportsLoginResponse(
    val access_token: String,
    val user: UserResponse?
)