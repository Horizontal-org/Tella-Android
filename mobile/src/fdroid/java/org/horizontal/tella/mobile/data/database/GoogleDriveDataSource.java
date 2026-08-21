package org.horizontal.tella.mobile.data.database;

import android.content.Context;

import androidx.annotation.NonNull;

import io.reactivex.Completable;
import io.reactivex.Single;
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile;
import org.horizontal.tella.mobile.domain.entity.googledrive.GoogleDriveServer;
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstance;
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstanceBundle;
import org.horizontal.tella.mobile.domain.repository.googledrive.ITellaGoogleDriveRepository;
import org.horizontal.tella.mobile.domain.repository.reports.ITellaReportsRepository;

import java.util.Collections;
import java.util.List;

/**
 * Stub implementation of GoogleDriveDataSource for F-Droid builds.
 * 
 * This class exists to satisfy compile-time dependencies but throws
 * UnsupportedOperationException for all operations since Google Drive
 * is not available in F-Droid builds.
 */
public class GoogleDriveDataSource implements ITellaGoogleDriveRepository, ITellaReportsRepository {

    private static GoogleDriveDataSource dataSource;

    private GoogleDriveDataSource(Context context, byte[] key) {
        // Stub implementation - no database initialization needed
    }

    public static synchronized GoogleDriveDataSource getInstance(Context context, byte[] key) {
        if (dataSource == null) {
            dataSource = new GoogleDriveDataSource(context.getApplicationContext(), key);
        }
        return dataSource;
    }

    @NonNull
    @Override
    public Single<GoogleDriveServer> saveGoogleDriveServer(@NonNull GoogleDriveServer server) {
        return Single.error(new UnsupportedOperationException("Google Drive is not available in F-Droid builds"));
    }

    @NonNull
    @Override
    public Single<List<GoogleDriveServer>> listGoogleDriveServers(String googleDriveId) {
        return Single.just(Collections.<GoogleDriveServer>emptyList());
    }

    @NonNull
    @Override
    public Completable removeGoogleDriveServer(long id) {
        return Completable.error(new UnsupportedOperationException("Google Drive is not available in F-Droid builds"));
    }

    @NonNull
    @Override
    public Single<ReportInstance> saveInstance(@NonNull ReportInstance instance) {
        return Single.error(new UnsupportedOperationException("Google Drive is not available in F-Droid builds"));
    }

    @NonNull
    @Override
    public Completable deleteReportInstance(long id) {
        return Completable.error(new UnsupportedOperationException("Google Drive is not available in F-Droid builds"));
    }

    @NonNull
    @Override
    public Single<List<ReportInstance>> listAllReportInstances() {
        return Single.just(Collections.<ReportInstance>emptyList());
    }

    @NonNull
    @Override
    public Single<List<ReportInstance>> listDraftReportInstances() {
        return Single.just(Collections.<ReportInstance>emptyList());
    }

    @NonNull
    @Override
    public Single<List<ReportInstance>> listOutboxReportInstances() {
        return Single.just(Collections.<ReportInstance>emptyList());
    }

    @NonNull
    @Override
    public Single<List<ReportInstance>> listSubmittedReportInstances() {
        return Single.just(Collections.<ReportInstance>emptyList());
    }

    @NonNull
    @Override
    public Single<ReportInstanceBundle> getReportBundle(long id) {
        return Single.error(new UnsupportedOperationException("Google Drive is not available in F-Droid builds"));
    }

    public Single<List<FormMediaFile>> getReportMediaFiles(ReportInstance instance) {
        return Single.just(Collections.<FormMediaFile>emptyList());
    }
}




