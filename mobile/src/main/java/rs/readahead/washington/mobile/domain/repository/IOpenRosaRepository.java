package rs.readahead.washington.mobile.domain.repository;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.javarosa.core.model.FormDef;

import io.reactivex.Single;
import rs.readahead.washington.mobile.domain.entity.IProgressListener;
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer;
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile;
import rs.readahead.washington.mobile.domain.entity.collect.ListFormResult;
import rs.readahead.washington.mobile.domain.entity.collect.NegotiatedCollectServer;
import rs.readahead.washington.mobile.domain.entity.collect.OpenRosaPartResponse;
import rs.readahead.washington.mobile.domain.entity.collect.OpenRosaResponse;


public interface IOpenRosaRepository {
    Single<ListFormResult> formList(CollectServer server);
    Single<FormDef> getFormDef(CollectServer server, CollectForm form);
    Single<NegotiatedCollectServer> submitFormNegotiate(CollectServer server);
    Single<OpenRosaResponse> submitForm(Context applicationContext, NegotiatedCollectServer server, CollectFormInstance instance);
    Single<OpenRosaPartResponse> submitFormGranular(
            @NonNull Context context,
            @NonNull NegotiatedCollectServer server,
            @NonNull CollectFormInstance instance,
            @Nullable FormMediaFile attachment,
            @Nullable IProgressListener progressListener);
}
