package org.horizontal.tella.mobile.data.database;

import android.content.Context;

import androidx.annotation.NonNull;

import io.reactivex.Completable;
import io.reactivex.Single;
import org.horizontal.tella.mobile.domain.entity.dropbox.DropBoxServer;
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstance;
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstanceBundle;
import org.horizontal.tella.mobile.domain.repository.dropbox.ITellaDropBoxRepository;
import org.horizontal.tella.mobile.domain.repository.reports.ITellaReportsRepository;

import java.util.Collections;
import java.util.List;

/**
 * Stub implementation of DropBoxDataSource for F-Droid builds.
 * 
 * This class exists to satisfy compile-time dependencies but throws
 * UnsupportedOperationException for all operations since Dropbox
 * is not available in F-Droid builds.
 */
public class DropBoxDataSource implements ITellaDropBoxRepository, ITellaReportsRepository {

    private static DropBoxDataSource dataSource;

    private DropBoxDataSource(Context context, byte[] key) {
        // Stub implementation - no database initialization needed
    }

    public static synchronized DropBoxDataSource getInstance(Context context, byte[] key) {
        if (dataSource == null) {
            dataSource = new DropBoxDataSource(context.getApplicationContext(), key);
        }
        return dataSource;
    }

    @NonNull
    @Override
    public Single<DropBoxServer> saveDropBoxServer(@NonNull DropBoxServer server) {
        return Single.error(new UnsupportedOperationException("Dropbox is not available in F-Droid builds"));
    }

    @NonNull
    @Override
    public Single<DropBoxServer> updateDropBoxServer(@NonNull DropBoxServer server) {
        return Single.error(new UnsupportedOperationException("Dropbox is not available in F-Droid builds"));
    }

    @NonNull
    @Override
    public Single<List<DropBoxServer>> listDropBoxServers() {
        return Single.just(Collections.<DropBoxServer>emptyList());
    }

    @NonNull
    @Override
    public Completable removeDropBoxServer(long id) {
        return Completable.error(new UnsupportedOperationException("Dropbox is not available in F-Droid builds"));
    }

    @NonNull
    @Override
    public Single<ReportInstance> saveInstance(@NonNull ReportInstance instance) {
        return Single.error(new UnsupportedOperationException("Dropbox is not available in F-Droid builds"));
    }

    @NonNull
    @Override
    public Completable deleteReportInstance(long id) {
        return Completable.error(new UnsupportedOperationException("Dropbox is not available in F-Droid builds"));
    }

    @NonNull
    @Override
    public Single<List<ReportInstance>> listAllReportInstances() {
        return Single.just(Collections.emptyList());
    }

    @NonNull
    @Override
    public Single<List<ReportInstance>> listDraftReportInstances() {
        return Single.just(Collections.emptyList());
    }

    @NonNull
    @Override
    public Single<List<ReportInstance>> listOutboxReportInstances() {
        return Single.just(Collections.emptyList());
    }

    @NonNull
    @Override
    public Single<List<ReportInstance>> listSubmittedReportInstances() {
        return Single.just(Collections.emptyList());
    }

    @NonNull
    @Override
    public Single<ReportInstanceBundle> getReportBundle(long id) {
        return Single.error(new UnsupportedOperationException("Dropbox is not available in F-Droid builds"));
    }
}




