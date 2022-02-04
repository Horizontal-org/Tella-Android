package rs.readahead.washington.mobile.domain.repository.uwazi;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate;
import rs.readahead.washington.mobile.domain.entity.uwazi.ListTemplateResult;
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityInstance;

public interface ICollectUwaziTemplatesRepository {
     Single<List<CollectTemplate>> listBlankTemplates();
     Single<List<CollectTemplate>> listFavoriteTemplates();
     Single<ListTemplateResult> updateBlankTemplates(ListTemplateResult listTemplateResult);
     Single<ListTemplateResult> updateBlankTemplatesIfNeeded(ListTemplateResult listTemplateResult);
     Single<CollectTemplate> updateBlankTemplate(CollectTemplate template);
     Single<CollectTemplate> saveBlankTemplate(CollectTemplate template);
     Single<CollectTemplate> getBlankCollectTemplateById(String templateID);
     Single<UwaziEntityInstance> saveEntityInstance(UwaziEntityInstance instance);
     Completable deleteTemplate(final long id);
     Completable deleteEntityInstance(final long id);
     Single<CollectTemplate> toggleFavorite(CollectTemplate template);
     Single<List<UwaziEntityInstance>> listDraftInstances();
     Single<List<UwaziEntityInstance>> listOutboxInstances();
     Single<List<UwaziEntityInstance>> listSubmittedInstances();
}
