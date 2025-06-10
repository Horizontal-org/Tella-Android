package org.horizontal.tella.mobile.domain.repository.uwazi;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import org.horizontal.tella.mobile.domain.entity.uwazi.UwaziTemplate;
import org.horizontal.tella.mobile.domain.entity.uwazi.EntityInstanceBundle;
import org.horizontal.tella.mobile.domain.entity.uwazi.ListTemplateResult;
import org.horizontal.tella.mobile.domain.entity.uwazi.UwaziEntityInstance;

public interface ICollectUwaziTemplatesRepository {
     Single<List<UwaziTemplate>> listBlankTemplates();
     Single<List<UwaziTemplate>> listFavoriteTemplates();
     Single<ListTemplateResult> updateBlankTemplates(ListTemplateResult listTemplateResult);
     Single<ListTemplateResult> updateBlankTemplatesIfNeeded(ListTemplateResult listTemplateResult);
     Single<UwaziTemplate> updateBlankTemplate(UwaziTemplate template);
     Single<UwaziTemplate> saveBlankTemplate(UwaziTemplate template);
     Single<UwaziTemplate> getBlankCollectTemplateById(String templateID);
     Single<UwaziEntityInstance> saveEntityInstance(UwaziEntityInstance instance);
     Completable deleteTemplate(final long id);
     Completable deleteEntityInstance(final long id);
     Single<UwaziTemplate> toggleFavorite(UwaziTemplate template);
     Single<List<UwaziEntityInstance>> listDraftInstances();
     Single<List<UwaziEntityInstance>> listOutboxInstances();
     Single<List<UwaziEntityInstance>> listSubmittedInstances();
     Single<EntityInstanceBundle> getBundle(final long id);
}
