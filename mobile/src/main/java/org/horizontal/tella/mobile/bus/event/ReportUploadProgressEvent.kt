package org.horizontal.tella.mobile.bus.event

import org.horizontal.tella.mobile.bus.IEvent
import org.horizontal.tella.mobile.domain.entity.reports.ReportInstance

class ReportUploadProgressEvent constructor(reportInstance: ReportInstance): IEvent {

}