package rs.readahead.washington.mobile.data.feedback.repository

import io.reactivex.Single
import rs.readahead.washington.mobile.data.database.DataSource
import rs.readahead.washington.mobile.data.entity.feedback.FeedbackBodyEntity
import rs.readahead.washington.mobile.data.entity.feedback.mapToDomainModel
import rs.readahead.washington.mobile.data.feedback.remote.FeedbackApiService
import rs.readahead.washington.mobile.domain.entity.feedback.FeedbackInstance
import rs.readahead.washington.mobile.domain.entity.feedback.FeedbackPostResult
import rs.readahead.washington.mobile.domain.repository.feedback.FeedBackRepository
import javax.inject.Inject

class FeedbackRepositoryImp @Inject internal constructor(
    private val apiService: FeedbackApiService,
    private val dataSource: DataSource
) : FeedBackRepository {
    override fun submitFeedback(feedbackBody: FeedbackBodyEntity): Single<FeedbackPostResult> {
        return apiService.submitFeedback(
            data = feedbackBody,
            tellaPlatform = "wearehorizontal",
        ).map { it.mapToDomainModel() }
    }

    override fun submitFeedback(feedbackInstance: FeedbackInstance): Single<FeedbackPostResult> {
        //dataSource.saveInstance(feedbackInstance)
        return submitFeedback(FeedbackBodyEntity(feedbackInstance.platform,feedbackInstance.text ))
    }


//    override fun submitReport(
//            server: TellaReportServer,
//            reportBody: ReportBodyEntity
//    ): Single<ReportPostResult> {
//        return apiService.submitReport(
//                reportBodyEntity = reportBody,
//                url = server.url + ParamsNetwork.URL_PROJECTS + "/${server.projectId}",
//                access_token = server.accessToken
//        ).map { it.mapToDomainModel() }
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .doOnError { }
//    }
//    override fun submitReport(
//            server: TellaReportServer,
//            instance: ReportInstance,
//            backButtonPressed: Boolean
//    ) {
//
//        if (backButtonPressed) {
//            if (instance.status != EntityStatus.SUBMITTED) {
//                instance.status = EntityStatus.SUBMISSION_IN_PROGRESS
//                dataSource.saveInstance(instance).subscribe()
//            }
//        }
//
//        if (!statusProvider.isOnline()) {
//            instance.status = EntityStatus.SUBMISSION_PENDING
//            dataSource.saveInstance(instance).subscribe()
//        }
//
//        if (instance.reportApiId.isEmpty()) {
//            disposables.add(
//                    submitReport(
//                            server,
//                            ReportBodyEntity(instance.title, instance.description)
//                    )
//                            .doOnError { throwable -> handleSubmissionError(instance, throwable) }
//
//                            .doOnDispose {
//                                instance.status = EntityStatus.PAUSED
//                                dataSource.saveInstance(instance).subscribe()
//
//                                instanceProgress.postValue(instance)
//                            }
//                            .subscribe { reportPostResult ->
//                                instance.apply {
//                                    reportApiId = reportPostResult.id
//                                }
//                                submitFiles(instance, server, reportPostResult.id)
//                            })
//        } else {
//            if (instance.status != EntityStatus.SUBMITTED) {
//                submitFiles(instance, server, instance.reportApiId)
//            }
//        }
//    }
}