package org.horizontal.tella.mobile.views.dialog

import org.horizontal.tella.mobile.domain.entity.reports.TellaReportServer

/**
 * Implemented by activities that host [TellaUploadServerDialogFragment] (playstore / fdroid).
 * Defined in main so shared code (e.g. onboarding) can reference it without depending on flavor sources.
 */
interface TellaUploadServerDialogHandler {
    fun onTellaUploadServerDialogCreate(server: TellaReportServer?)
    fun onTellaUploadServerDialogUpdate(server: TellaReportServer?)
    fun onDialogDismiss()
}
