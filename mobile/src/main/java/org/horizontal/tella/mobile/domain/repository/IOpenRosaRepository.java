package org.horizontal.tella.mobile.domain.repository;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.javarosa.core.model.FormDef;

import io.reactivex.Single;
import org.horizontal.tella.mobile.domain.entity.IProgressListener;
import org.horizontal.tella.mobile.domain.entity.collect.CollectForm;
import org.horizontal.tella.mobile.domain.entity.collect.CollectFormInstance;
import org.horizontal.tella.mobile.domain.entity.collect.CollectServer;
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile;
import org.horizontal.tella.mobile.domain.entity.collect.ListFormResult;
import org.horizontal.tella.mobile.domain.entity.collect.NegotiatedCollectServer;
import org.horizontal.tella.mobile.domain.entity.collect.OpenRosaPartResponse;
import org.horizontal.tella.mobile.domain.entity.collect.OpenRosaResponse;


public interface IOpenRosaRepository {
    Single<ListFormResult> formList(CollectServer server);
    Single<FormDef> getFormDef(CollectServer server, CollectForm form);
    Single<NegotiatedCollectServer> submitFormNegotiate(CollectServer server);
    Single<OpenRosaResponse> submitForm(Context applicationContext, NegotiatedCollectServer server, CollectFormInstance instance);
    Single<OpenRosaPartResponse> submitFormGranular(
            @NonNull NegotiatedCollectServer server,
            @NonNull CollectFormInstance instance,
            @Nullable FormMediaFile attachment,
            @Nullable IProgressListener progressListener);
}
