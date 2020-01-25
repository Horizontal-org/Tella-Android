package rs.readahead.washington.mobile.domain.repository;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import rs.readahead.washington.mobile.domain.entity.TellaUploadServer;


public interface ITellaUploadServersRepository {
    Single<List<TellaUploadServer>> listTellaUploadServers();
    Single<TellaUploadServer> createTellaUploadServer(TellaUploadServer server);
    Single<TellaUploadServer> updateTellaUploadServer(TellaUploadServer server);
    Single<TellaUploadServer> getTellaUploadServer(long id);
    Completable removeTUServer(long id);
    Single<Long> countTUServers();
}