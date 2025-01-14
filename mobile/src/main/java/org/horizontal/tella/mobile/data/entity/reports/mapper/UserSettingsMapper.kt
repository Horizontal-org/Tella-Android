package org.horizontal.tella.mobile.data.entity.reports.mapper

import org.horizontal.tella.mobile.data.entity.reports.ProjectSlugResponse
import org.horizontal.tella.mobile.data.entity.reports.ReportsLoginResponse
import org.horizontal.tella.mobile.data.entity.reports.UserResponse
import org.horizontal.tella.mobile.domain.entity.reports.ProjectSlugResult
import org.horizontal.tella.mobile.domain.entity.reports.ReportsLoginResult
import org.horizontal.tella.mobile.domain.entity.reports.User

fun ProjectSlugResponse.mapToDomainModel() = ProjectSlugResult(
    id = id ?: "",
    name = name ?: "",
    slug = slug ?: ""
)

fun ReportsLoginResponse.mapToDomainModel() =
    user?.mapToDomainModel()
        ?.let { ReportsLoginResult(accessToken = access_token ?: "", user = it) }


fun UserResponse.mapToDomainModel() =
    User(id = id ?: "", note = note ?: "", role = role ?: "", username = username ?: "")