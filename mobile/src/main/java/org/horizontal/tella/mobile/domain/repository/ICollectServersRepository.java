package org.horizontal.tella.mobile.domain.repository;

import java.util.List;
import io.reactivex.Completable;
import io.reactivex.Single;
import org.horizontal.tella.mobile.domain.entity.collect.CollectServer;


public interface ICollectServersRepository {
    Single<List<CollectServer>> listCollectServers();
    Single<CollectServer> createCollectServer(CollectServer server);
    Single<CollectServer> updateCollectServer(CollectServer server);
    Single<CollectServer> getCollectServer(long id);
    Completable removeCollectServer(long id);
    Single<Long> countCollectServers();
}
