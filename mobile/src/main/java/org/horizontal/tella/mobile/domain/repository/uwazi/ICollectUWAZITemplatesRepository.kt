package org.horizontal.tella.mobile.domain.repository.uwazi

import kotlinx.coroutines.flow.Flow
import org.horizontal.tella.mobile.domain.entity.uwazi.UwaziTemplate
import org.horizontal.tella.mobile.domain.entity.uwazi.ListTemplateResult

interface ICollectUWAZITemplatesRepository {
    suspend fun listBlankTemplates(): Flow<List<UwaziTemplate>>
    suspend fun listFavoriteTemplates(): Flow<List<UwaziTemplate>>
    suspend fun updateBlankTemplates(listTemplateResult: ListTemplateResult): Flow<ListTemplateResult>
    suspend fun updateBlankTemplatesIfNeeded(listTemplateResult: ListTemplateResult): Flow<ListTemplateResult>
    suspend fun updateBlankTemplate(template: UwaziTemplate?): Flow<ListTemplateResult>
}