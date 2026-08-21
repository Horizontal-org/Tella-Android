package org.horizontal.tella.mobile.domain.repository;

import io.reactivex.Completable;


public interface IServersRepository {
    Completable deleteAllServers();
}
