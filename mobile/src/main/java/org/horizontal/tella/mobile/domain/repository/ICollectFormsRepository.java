package org.horizontal.tella.mobile.domain.repository;

import com.hzontal.tella_vault.Metadata;
import com.hzontal.tella_vault.VaultFile;

import org.javarosa.core.model.FormDef;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.horizontal.tella.mobile.domain.entity.collect.CollectForm;
import org.horizontal.tella.mobile.domain.entity.collect.CollectFormInstance;
import org.horizontal.tella.mobile.domain.entity.collect.ListFormResult;


public interface ICollectFormsRepository {
    Single<List<CollectForm>> listBlankForms();
    Single<List<CollectForm>> listFavoriteCollectForms();
    Single<ListFormResult> updateBlankForms(ListFormResult listFormResult);
    Maybe<FormDef> getBlankFormDef(CollectForm form);
    Single<FormDef> updateBlankFormDef(CollectForm form, FormDef formDef);
    Single<FormDef> updateBlankCollectFormDef(CollectForm form, FormDef formDef);
    Single<CollectForm> toggleFavorite(CollectForm form);
    Single<CollectForm> getBlankCollectFormById(String formID);
    Completable removeBlankFormDef(CollectForm form);

    Single<List<CollectFormInstance>> listDraftForms();
    Single<List<CollectFormInstance>> listSentForms();
    Single<List<CollectFormInstance>> listPendingForms();
    Single<CollectFormInstance> saveInstance(CollectFormInstance instance);
    Single<CollectFormInstance> getInstance(long id);
    Completable deleteInstance(long id);
    Single<VaultFile> attachMetadata(String mediaFileId, Metadata metadata);
}
