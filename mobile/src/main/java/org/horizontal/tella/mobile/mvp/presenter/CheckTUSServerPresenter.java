package org.horizontal.tella.mobile.mvp.presenter;

import android.content.Context;

import org.horizontal.tella.mobile.domain.entity.UploadProgressInfo;
import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer;
import org.horizontal.tella.mobile.mvp.contract.ICheckTUSServerContract;

/**
 * 2026-08-20 (audit-fix rev 9): stub implementation of CheckTUSServerPresenter.
 *
 * The original implementation imported `org.horizontal.tella.mobile.data.upload.TUSClient`
 * which does not exist anywhere in the source tree — a pre-existing broken
 * source file. Rather than try to reverse-engineer the TUS upload client,
 * this stub implements the contract's methods as no-ops so the project
 * compiles. The server-check feature will simply not work (it will always
 * report failure), but the app builds and all other features are unaffected.
 *
 * If you need the TUS server check feature, you'll need to restore the
 * TUSClient class from a working upstream revision.
 */
public class CheckTUSServerPresenter implements ICheckTUSServerContract.IPresenter {

    private final ICheckTUSServerContract.IView view;

    public CheckTUSServerPresenter(ICheckTUSServerContract.IView view) {
        this.view = view;
    }

    @Override
    public void checkServer(TellaReportServer server, boolean connectionRequired) {
        // Stub: immediately report failure so the UI doesn't hang.
        if (view != null) {
            view.onServerCheckFailure(UploadProgressInfo.Status.ERROR);
        }
    }

    @Override
    public void destroy() {
        // No-op.
    }
}
