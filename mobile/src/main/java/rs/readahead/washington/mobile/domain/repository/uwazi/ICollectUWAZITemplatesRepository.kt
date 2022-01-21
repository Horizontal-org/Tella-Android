package rs.readahead.washington.mobile.domain.repository.uwazi

import kotlinx.coroutines.flow.Flow
import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate
import rs.readahead.washington.mobile.domain.entity.uwazi.ListTemplateResult

interface ICollectUWAZITemplatesRepository {
    suspend fun listBlankTemplates(): Flow<List<CollectTemplate>>
    suspend fun listFavoriteTemplates(): Flow<List<CollectTemplate>>
    suspend fun updateBlankTemplates(listTemplateResult: ListTemplateResult): Flow<ListTemplateResult>
    suspend fun updateBlankTemplatesIfNeeded(listTemplateResult: ListTemplateResult): Flow<ListTemplateResult>
    suspend fun updateBlankTemplate(template: CollectTemplate?): Flow<ListTemplateResult>
}