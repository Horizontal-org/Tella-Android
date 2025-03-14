package org.horizontal.tella.mobile.domain.repository;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer;


public interface ITellaUploadServersRepository {
    Single<List<TellaReportServer>> listTellaUploadServers();
    Single<TellaReportServer> createTellaUploadServer(TellaReportServer server);
    Single<TellaReportServer> updateTellaUploadServer(TellaReportServer server);
    Single<TellaReportServer> getTellaUploadServer(long id);
    Completable removeTellaServerAndResources(long id);
    Single<Long> countTUServers();
}