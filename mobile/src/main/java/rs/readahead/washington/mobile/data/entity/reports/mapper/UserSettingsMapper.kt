package rs.readahead.washington.mobile.data.entity.reports.mapper

import rs.readahead.washington.mobile.data.entity.reports.ReportsLoginResponse
import rs.readahead.washington.mobile.data.entity.reports.UserResponse
import rs.readahead.washington.mobile.domain.entity.reports.ReportsLoginResult
import rs.readahead.washington.mobile.domain.entity.reports.User

fun ReportsLoginResponse.mapToDomainModel() =
    user?.mapToDomainModel()
        ?.let { ReportsLoginResult(accessToken = access_token ?: "", user = it) }

fun UserResponse.mapToDomainModel() =
    User(id = id ?: "", note = note ?: "", role = role ?: "", username = username ?: "")