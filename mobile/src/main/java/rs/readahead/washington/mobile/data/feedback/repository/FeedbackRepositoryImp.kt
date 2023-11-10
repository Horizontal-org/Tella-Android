package rs.readahead.washington.mobile.data.feedback.repository

import android.annotation.SuppressLint
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.data.database.DataSource
import rs.readahead.washington.mobile.data.entity.feedback.FeedbackBodyEntity
import rs.readahead.washington.mobile.data.entity.feedback.mapToDomainModel
import rs.readahead.washington.mobile.data.feedback.remote.FeedbackApiService
import rs.readahead.washington.mobile.domain.entity.feedback.FeedbackInstance
import rs.readahead.washington.mobile.domain.entity.feedback.FeedbackPostResult
import rs.readahead.washington.mobile.domain.entity.feedback.FeedbackStatus
import rs.readahead.washington.mobile.domain.exception.NoConnectivityException
import rs.readahead.washington.mobile.domain.repository.feedback.FeedBackRepository
import javax.inject.Inject

class FeedbackRepositoryImp @Inject internal constructor(
        private val apiService: FeedbackApiService, private val dataSource: DataSource) : FeedBackRepository {

    override fun submitFeedback(feedbackBody: FeedbackBodyEntity): Single<FeedbackPostResult> {
        return apiService.submitFeedback(
                data = feedbackBody,
                tellaPlatform = "wearehorizontal",
        ).map { it.mapToDomainModel() }
    }
// save should be done before submit and in submit success do


    @SuppressLint("CheckResult")
    override fun submitFeedback(feedbackInstance: FeedbackInstance): Single<FeedbackPostResult> {
        return submitFeedback(FeedbackBodyEntity(feedbackInstance.platform, feedbackInstance.text))
                .doOnError { throwable ->
                    handleSubmissionError(feedbackInstance, throwable)
                }.doOnSuccess {
                    dataSource.deleteFeedbackInstance(feedbackInstance.id)
                            .subscribeOn(Schedulers.io()).subscribe({
                    }, { throwable ->
                        throwable.printStackTrace()
                    })
                }
    }

    private fun handleSubmissionError(feedbackInstance: FeedbackInstance, throwable: Throwable) {
        feedbackInstance.status = if (throwable is NoConnectivityException) {
            FeedbackStatus.SUBMISSION_PENDING
        } else {
            FeedbackStatus.SUBMISSION_ERROR
        }
        dataSource.saveInstance(feedbackInstance).subscribe()
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