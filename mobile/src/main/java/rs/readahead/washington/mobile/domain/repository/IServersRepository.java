package rs.readahead.washington.mobile.domain.repository;

import io.reactivex.Completable;


public interface IServersRepository {
    Completable deleteAllServers();
}
