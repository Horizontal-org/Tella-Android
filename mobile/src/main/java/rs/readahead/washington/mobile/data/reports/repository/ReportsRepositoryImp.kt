package rs.readahead.washington.mobile.data.reports.repository

import android.annotation.SuppressLint
import android.text.TextUtils
import android.webkit.MimeTypeMap
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.MutableLiveData
import com.hzontal.tella_vault.VaultFile
import io.reactivex.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import retrofit2.Response
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

    override fun submitReport(
        server: TellaReportServer,
        instance: ReportInstance,
        backButtonPressed: Boolean
    ) {

        if (backButtonPressed){
            instance.status = EntityStatus.SUBMISSION_IN_PROGRESS
            dataSource.saveInstance(instance).subscribe()
        }

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
        status: EntityStatus
    ) {
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

    /**
     * Prepares and starts the file upload process.
     *
     * @param vaultFile The file to be uploaded.
     * @param skipBytes The number of bytes to skip when reading the file.
     * @param baseUrl The base URL for the API endpoint.
     * @param accessToken The access token for authentication.
     * @return a Flowable that emits UploadProgressInfo as the file upload progresses.
     */
    @VisibleForTesting
    private fun appendFile(
        vaultFile: VaultFile,
        skipBytes: Long,
        baseUrl: String,
        accessToken: String
    ): Flowable<UploadProgressInfo> {
        return Flowable.create({ emitter: FlowableEmitter<UploadProgressInfo> ->
            val size = vaultFile.size
            val uploadEmitter = UploadEmitter()
            val fileToUpload =
                prepareFileToUpload(vaultFile, skipBytes, emitter, uploadEmitter, size)

            uploadFile(fileToUpload, baseUrl, accessToken, emitter, vaultFile, size)

        }, BackpressureStrategy.LATEST)
    }

    /**
     * Prepares the file to be uploaded.
     *
     * @param vaultFile The file to be uploaded.
     * @param skipBytes The number of bytes to skip when reading the file.
     * @param emitter The FlowableEmitter to emit progress updates.
     * @param uploadEmitter The UploadEmitter to use for emitting upload progress.
     * @param size The size of the file.
     * @return a SkippableMediaFileRequestBody that's ready for upload.
     */
    @VisibleForTesting
    private fun prepareFileToUpload(
        vaultFile: VaultFile,
        skipBytes: Long,
        emitter: FlowableEmitter<UploadProgressInfo>,
        uploadEmitter: UploadEmitter,
        size: Long
    ): SkippableMediaFileRequestBody {
        emitter.onNext(
            UploadProgressInfo(
                vaultFile,
                skipBytes,
                UploadProgressInfo.Status.STARTED
            )
        )

        return SkippableMediaFileRequestBody(
            vaultFile, skipBytes
        ) { current: Long, _: Long ->
            uploadEmitter.emit(
                emitter,
                vaultFile,
                skipBytes + current,
                size
            )
        }
    }

    /**
     * Uploads the file to the server.
     *
     * @param fileToUpload The file to be uploaded.
     * @param baseUrl The base URL for the API endpoint.
     * @param accessToken The access token for authentication.
     * @param emitter The FlowableEmitter to emit progress updates.
     * @param vaultFile The file to be uploaded.
     * @param size The size of the file.
     */
    @VisibleForTesting
    private fun uploadFile(
        fileToUpload: SkippableMediaFileRequestBody,
        baseUrl: String,
        accessToken: String,
        emitter: FlowableEmitter<UploadProgressInfo>,
        vaultFile: VaultFile,
        size: Long
    ) {
        val disposable = apiService.putFile(fileToUpload, baseUrl, accessToken)
            .flatMap { response ->
                if (!response.isSuccessful) {
                    Single.error(UploadError(response.code()))
                } else {
                    apiService.postFile(baseUrl, accessToken)
                }
            }
            .subscribe({ response ->
                handleUploadResponse(response, emitter, vaultFile, size)
            }, { error ->
                emitter.onError(UploadError(error))
            })

        disposables.add(disposable)
    }

    /**
     * Handles the response from the server after file upload.
     *
     * @param response The response from the server.
     * @param emitter The FlowableEmitter to emit progress updates.
     * @param vaultFile The file that was uploaded.
     * @param size The size of the file.
     */
    @VisibleForTesting
    private fun handleUploadResponse(
        response: Response<Void>,
        emitter: FlowableEmitter<UploadProgressInfo>,
        vaultFile: VaultFile,
        size: Long
    ) {
        if (!response.isSuccessful) {
            emitter.onError(UploadError(response.code()))
        } else {
            emitter.onNext(
                UploadProgressInfo(
                    vaultFile,
                    size,
                    UploadProgressInfo.Status.FINISHED
                )
            )
            emitter.onComplete()
        }
    }

    override fun cleanup() {
        disposables.clear()
    }

    /**
     * Maps the provided throwable to an appropriate UploadProgressInfo.
     *
     * This method is used to translate exceptions into UploadProgressInfo instances with
     * appropriate statuses, allowing the upload process to communicate what kind of error occurred.
     *
     * @param throwable The exception to map.
     * @param vaultFile The file associated with the upload attempt.
     * @return An UploadProgressInfo instance corresponding to the provided exception.
     */
    private fun mapThrowable(throwable: Throwable?, vaultFile: VaultFile): UploadProgressInfo {
        val status: UploadProgressInfo.Status = when (throwable) {
            is UploadError -> toStatus(throwable.code)
            is UnknownHostException -> UploadProgressInfo.Status.UNKNOWN_HOST
            else -> UploadProgressInfo.Status.ERROR
        }

        Timber.d(throwable)

        return UploadProgressInfo(vaultFile, 0, status)
    }

    /**
     * Maps the HTTP status code to an appropriate UploadProgressInfo.Status.
     *
     * This method is used to translate HTTP status codes into UploadProgressInfo statuses,
     * allowing the upload process to communicate the status of the upload.
     *
     * @param code The HTTP status code to map.
     * @return An UploadProgressInfo.Status instance corresponding to the provided HTTP status code.
     */
    private fun toStatus(code: Int): UploadProgressInfo.Status {
        return when {
            HttpStatus.isSuccess(code) -> UploadProgressInfo.Status.OK
            code == HttpStatus.UNAUTHORIZED_401 -> UploadProgressInfo.Status.UNAUTHORIZED
            code == HttpStatus.CONFLICT_409 -> UploadProgressInfo.Status.CONFLICT
            code == -1 || HttpStatus.isClientError(code) || HttpStatus.isServerError(code) -> UploadProgressInfo.Status.ERROR
            else -> UploadProgressInfo.Status.UNKNOWN
        }
    }

    /**
     * Class responsible for emitting UploadProgressInfo updates.
     *
     * This class helps to control the rate of emission of upload progress updates.
     * It only emits progress updates if more than REFRESH_TIME_MS milliseconds have passed since the last emission.
     * This is useful to avoid flooding the observer with too many updates, which might not be necessary and could negatively impact performance.
     */
    private class UploadEmitter {
        private var lastEmissionTime: Long = 0

        /**
         * Emits an UploadProgressInfo update if sufficient time has passed.
         *
         * @param emitter The emitter to emit the progress updates to.
         * @param file The file being uploaded.
         * @param current The number of bytes uploaded so far.
         * @param total The total size of the file.
         */
        fun emit(
            emitter: Emitter<UploadProgressInfo>,
            file: VaultFile,
            current: Long,
            total: Long
        ) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastEmissionTime >= REFRESH_TIME_MS) {
                lastEmissionTime = currentTime
                emitter.onNext(UploadProgressInfo(file, current, total))
            }
        }

        companion object {
            // The minimum time between emissions in milliseconds.
            private const val REFRESH_TIME_MS: Long = 500
        }
    }

    /**
     * Exception class representing an error occurred during the upload process.
     *
     * This class extends Exception and adds a property for an HTTP response code.
     * It provides two constructors: one for when an HTTP response code is available, and another for when there's a
     * Throwable that caused the error.
     *
     * @property code The HTTP status code associated with this error, or -1 if not applicable.
     */
    private class UploadError : Exception {

        val code: Int

        /**
         * Creates an UploadError with the specified HTTP response code.
         *
         * @param code The HTTP status code that caused the error.
         */
        constructor(code: Int) : super("Request failed, response code: $code") {
            this.code = code
        }

        /**
         * Creates an UploadError with the specified cause.
         *
         * This constructor is used when the error is caused by another Throwable instance. In this case, the HTTP
         * response code is not known and is set to -1.
         *
         * @param cause The Throwable that caused the error.
         */
        constructor(cause: Throwable?) : super(cause) {
            this.code = -1
        }
    }


}