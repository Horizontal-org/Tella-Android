package rs.readahead.washington.mobile.data.reports.repository

import android.annotation.SuppressLint
import android.text.TextUtils
import android.webkit.MimeTypeMap
import androidx.lifecycle.MutableLiveData
import com.hzontal.tella_vault.VaultFile
import io.reactivex.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.MyApplication
import rs.readahead.washington.mobile.data.database.DataSource
import rs.readahead.washington.mobile.data.entity.reports.LoginEntity
import rs.readahead.washington.mobile.data.entity.reports.ReportBodyEntity
import rs.readahead.washington.mobile.data.entity.reports.mapper.mapToDomainModel
import rs.readahead.washington.mobile.data.http.HttpStatus
import rs.readahead.washington.mobile.data.reports.remote.ReportsApiService
import rs.readahead.washington.mobile.data.reports.utils.ParamsNetwork.URL_LOGIN
import rs.readahead.washington.mobile.data.reports.utils.ParamsNetwork.URL_PROJECTS
import rs.readahead.washington.mobile.data.repository.SkippableMediaFileRequestBody
import rs.readahead.washington.mobile.data.sharedpref.Preferences
import rs.readahead.washington.mobile.domain.entity.EntityStatus
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFileStatus
import rs.readahead.washington.mobile.domain.entity.reports.ReportInstance
import rs.readahead.washington.mobile.domain.entity.reports.ReportPostResult
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.domain.exception.NoConnectivityException
import rs.readahead.washington.mobile.domain.repository.reports.ReportsRepository
import rs.readahead.washington.mobile.util.StatusProvider
import rs.readahead.washington.mobile.util.StringUtils
import rs.readahead.washington.mobile.util.Util
import timber.log.Timber
import java.net.UnknownHostException
import java.util.*
import javax.inject.Inject


class ReportsRepositoryImp @Inject internal constructor(
    private val apiService: ReportsApiService,
    private val dataSource: DataSource,
    private val statusProvider: StatusProvider
) : ReportsRepository {

    private val reportProgress = MutableLiveData<Pair<UploadProgressInfo, ReportInstance>>()
    private val instanceProgress = MutableLiveData<ReportInstance>()

    private val disposables = CompositeDisposable()

    override fun login(server: TellaReportServer, slug: String): Single<TellaReportServer> {
        return apiService.login(
            loginEntity = LoginEntity(server.username, server.password),
            url = server.url + URL_LOGIN
        ).flatMap { response ->
            apiService.getProjectSlug(
                url = server.url + slug,
                access_token = "Bearer " + response.access_token
            ).map { result ->
                server.apply {
                    accessToken = response.access_token
                    projectId = result.id
                    projectName = result.name
                    projectSlug = result.slug
                    name = result.name
                }
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    override fun submitReport(server: TellaReportServer, instance: ReportInstance) {

        if (!statusProvider.isOnline()) {
            instance.status = EntityStatus.SUBMISSION_PENDING
            dataSource.saveInstance(instance).subscribe()
        }

        if (instance.reportApiId.isEmpty()) {
            disposables.add(
                submitReport(
                    server,
                    ReportBodyEntity(instance.title, instance.description)
                )
                    .doOnError { throwable -> handleSubmissionError(instance, throwable) }

                    .doOnDispose {
                        instance.status = EntityStatus.PAUSED
                        dataSource.saveInstance(instance).subscribe()

                        instanceProgress.postValue(instance)
                    }
                    .subscribe { reportPostResult ->
                        instance.apply {
                            reportApiId = reportPostResult.id
                        }
                        submitFiles(instance, server, reportPostResult.id)
                    })
        } else {
            submitFiles(instance, server, instance.reportApiId)
        }
    }

    private fun handleSubmissionError(instance: ReportInstance, throwable: Throwable) {
        instance.status = if (throwable is NoConnectivityException) {
            EntityStatus.SUBMISSION_PENDING
        } else {
            EntityStatus.SUBMISSION_ERROR
        }
        dataSource.saveInstance(instance).subscribe()
        instanceProgress.postValue(instance)
    }

    override fun submitFiles(
        instance: ReportInstance,
        server: TellaReportServer,
        reportApiId: String
    ) {
        if (instance.widgetMediaFiles.isEmpty()) {
            handleInstanceStatus(instance, EntityStatus.SUBMITTED)
            return
        }

        disposables.add(
            Flowable.fromIterable(instance.widgetMediaFiles)
                .flatMap { file ->
                    upload(file, server.url, reportApiId, server.accessToken)
                }
                .doOnEach {
                    instance.status = EntityStatus.SUBMISSION_IN_PROGRESS
                }
                .doOnTerminate { handleInstanceOnTerminate(instance) }
                .doOnCancel { handleInstanceStatus(instance, EntityStatus.PAUSED) }
                .doOnError {
                    handleInstanceStatus(
                        instance,
                        EntityStatus.SUBMISSION_ERROR
                    )
                }
                .doOnNext { progressInfo: UploadProgressInfo ->
                    updateFileStatus(instance, progressInfo)
                }
                .doAfterNext { progressInfo ->
                    reportProgress.postValue(Pair(progressInfo, instance))
                }
                .subscribeOn(Schedulers.io()) // Non-blocking operation
                .subscribe()
        )
    }

    private fun handleInstanceOnTerminate(instance: ReportInstance) {
        if (!instance.widgetMediaFiles.any { it.status == FormMediaFileStatus.SUBMITTED }) {
            handleInstanceStatus(instance, EntityStatus.SUBMISSION_PENDING)
        } else {
            handleAutoDeleteAndFinalStatus(instance)
        }
    }

    @SuppressLint("CheckResult")
    private fun handleInstanceStatus(
        instance: ReportInstance,
        status: EntityStatus) {
        instance.status = status
        dataSource.saveInstance(instance)
            .subscribeOn(Schedulers.io())
            .subscribe({}, { throwable ->
                throwable.printStackTrace()
            })
        instanceProgress.postValue(instance)
    }

    @SuppressLint("CheckResult")
    private fun handleAutoDeleteAndFinalStatus(instance: ReportInstance) {
        if (Preferences.isAutoDeleteEnabled() && instance.current == 1L) {
            instance.current = 0
            Observable.fromIterable(instance.widgetMediaFiles)
                .flatMapCompletable { formMediaFile ->
                    MyApplication.rxVault.delete(formMediaFile.vaultFile)
                        .subscribeOn(Schedulers.io())
                        .ignoreElement() // converts Single to Completable by ignoring the result
                }
                .andThen(dataSource.deleteReportInstance(instance.id).subscribeOn(Schedulers.io()))
                .subscribe({

                }, { throwable ->
                    throwable.printStackTrace()
                })
            handleInstanceStatus(instance, EntityStatus.DELETED)
        } else {
            handleInstanceStatus(instance, EntityStatus.SUBMITTED)
        }
    }

    private fun updateFileStatus(instance: ReportInstance, progressInfo: UploadProgressInfo) {
        val file = instance.widgetMediaFiles.first { it.id == progressInfo.fileId }
        file.apply {
            status =
                if (progressInfo.status == UploadProgressInfo.Status.FINISHED) FormMediaFileStatus.SUBMITTED else FormMediaFileStatus.NOT_SUBMITTED
            uploadedSize = progressInfo.current
        }
        instance.widgetMediaFiles.first { it.id == progressInfo.fileId }.apply {
            status = file.status
            uploadedSize = file.uploadedSize
        }
        dataSource.saveInstance(instance).subscribe()
    }

    override fun submitReport(
        server: TellaReportServer,
        reportBody: ReportBodyEntity
    ): Single<ReportPostResult> {
        return apiService.submitReport(
            reportBodyEntity = reportBody,
            url = server.url + URL_PROJECTS + "/${server.projectId}",
            access_token = server.accessToken
        ).map { it.mapToDomainModel() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError { }
    }

    private fun getFileName(vaultFile: VaultFile): String {
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(vaultFile.mimeType)
        return vaultFile.id + if (!TextUtils.isEmpty(ext)) ".$ext" else ""
    }

    override fun upload(
        vaultFile: VaultFile,
        urlServer: String,
        reportId: String,
        accessToken: String
    ): Flowable<UploadProgressInfo> {
        val url = StringUtils.append(
            '/',
            urlServer,
            "file/$reportId/${getFileName(vaultFile)}"
        )
        return getStatus(url, accessToken)
            .flatMapPublisher { skipBytes: Long ->
                appendFile(
                    vaultFile,
                    skipBytes,
                    url,
                    accessToken
                )
            }.onErrorReturn {
                mapThrowable(
                    it, vaultFile
                )
            }
    }

    override fun check(
        vaultFile: VaultFile,
        urlServer: String,
        reportId: String,
        accessToken: String
    ): Single<UploadProgressInfo> {

        val url = StringUtils.append(
            '/',
            urlServer,
            "$reportId${getFileName(vaultFile)}"
        )
        return getStatus(url, accessToken)
            .map { current: Long ->
                UploadProgressInfo(
                    vaultFile,
                    current,
                    vaultFile.size
                )
            }
            .onErrorReturn { throwable: Throwable? ->
                mapThrowable(
                    throwable, vaultFile
                )
            }
    }

    override fun getDisposable() = disposables

    override fun getReportProgress() = reportProgress

    override fun geInstanceProgress() = instanceProgress

    private fun getStatus(url: String, accessToken: String): Single<Long> {
        return apiService.getStatus(url, accessToken)
            .subscribeOn(Schedulers.io())
            .doOnError { throw (it) }
            .map {
                Util.parseLong(
                    it.headers()["size"],
                    0
                )
            }
    }

    private fun appendFile(
        vaultFile: VaultFile,
        skipBytes: Long,
        baseUrl: String,
        accessToken: String
    ): Flowable<UploadProgressInfo> {
        return Flowable.create({ emitter: FlowableEmitter<UploadProgressInfo> ->
            try {
                val size = vaultFile.size
                //  val fileName = vaultFile.name
                val uploadEmitter = UploadEmitter()
                emitter.onNext(
                    UploadProgressInfo(
                        vaultFile,
                        skipBytes,
                        UploadProgressInfo.Status.STARTED
                    )
                )

                val fileToUpload = SkippableMediaFileRequestBody(
                    vaultFile, skipBytes
                ) { current: Long, _ : Long ->
                    uploadEmitter.emit(
                        emitter,
                        vaultFile,
                        skipBytes + current,
                        size
                    )
                }

                var response = apiService.putFile(
                    file = fileToUpload,
                    url = baseUrl,
                    access_token = accessToken
                ).blockingGet()

                if (!response.isSuccessful) {
                    emitter.onError(UploadError(response.code()))
                    return@create
                }

                response = apiService.postFile(
                    //   file = fileToUpload,
                    url = baseUrl,
                    access_token = accessToken
                ).blockingGet()

                if (!response.isSuccessful) {
                    emitter.onError(UploadError(response.code()))
                    return@create
                }
                emitter.onNext(
                    UploadProgressInfo(
                        vaultFile,
                        size,
                        UploadProgressInfo.Status.FINISHED
                    )
                )
                emitter.onComplete()
            } catch (e: Exception) {
                emitter.onError(UploadError(e))
            }
        }, BackpressureStrategy.LATEST)
    }


    private fun mapThrowable(throwable: Throwable?, vaultFile: VaultFile): UploadProgressInfo {
        Timber.d(throwable)
        var status: UploadProgressInfo.Status? = UploadProgressInfo.Status.ERROR
        if (throwable is UploadError) {
            status = toStatus(throwable.code)
        } else if (throwable is UnknownHostException) {
            status = UploadProgressInfo.Status.UNKNOWN_HOST
        }
        return UploadProgressInfo(vaultFile, 0, status)
    }

    private fun toStatus(code: Int): UploadProgressInfo.Status {
        if (HttpStatus.isSuccess(code)) {
            return UploadProgressInfo.Status.OK
        }
        if (code == HttpStatus.UNAUTHORIZED_401) {
            return UploadProgressInfo.Status.UNAUTHORIZED
        }
        if (code == HttpStatus.CONFLICT_409) {
            return UploadProgressInfo.Status.CONFLICT
        }
        return if (code == -1 || HttpStatus.isClientError(code) || HttpStatus.isServerError(code)) {
            UploadProgressInfo.Status.ERROR
        } else UploadProgressInfo.Status.UNKNOWN
    }

    // maybe there is a better way to emit once per 500ms?
    private class UploadEmitter {
        private var time: Long = 0
        fun emit(
            emitter: Emitter<UploadProgressInfo?>,
            file: VaultFile?,
            current: Long,
            total: Long
        ) {
            val now = Util.currentTimestamp()
            if (now - time > REFRESH_TIME_MS) {
                time = now
                emitter.onNext(UploadProgressInfo(file, current, total))
            }
        }

        companion object {
            private const val REFRESH_TIME_MS: Long = 500
        }
    }

    private class UploadError : Exception {
        var code = -1

        constructor(code: Int) : super(
            String.format(
                Locale.ROOT, "Request failed, response code: %d", code
            )
        ) {
            this.code = code
        }

        constructor(cause: Throwable?) : super(cause)
    }

}