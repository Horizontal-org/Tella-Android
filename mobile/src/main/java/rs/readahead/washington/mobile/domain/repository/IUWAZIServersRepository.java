package rs.readahead.washington.mobile.domain.repository;

import java.util.List;

import io.reactivex.Single;
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer;

public interface IUWAZIServersRepository {
    Single<List<UWaziUploadServer>> listUwaziServers();
    Single<UWaziUploadServer> createUWAZIServer(UWaziUploadServer server);
}
