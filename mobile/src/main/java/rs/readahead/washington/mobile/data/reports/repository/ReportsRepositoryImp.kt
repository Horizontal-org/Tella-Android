package rs.readahead.washington.mobile.data.reports.repository

import com.hzontal.tella_vault.VaultFile
import io.reactivex.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import rs.readahead.washington.mobile.data.entity.reports.LoginEntity
import rs.readahead.washington.mobile.data.entity.reports.ReportBodyEntity
import rs.readahead.washington.mobile.data.entity.reports.mapper.mapToDomainModel
import rs.readahead.washington.mobile.data.http.HttpStatus
import rs.readahead.washington.mobile.data.reports.remote.ReportsApiService
import rs.readahead.washington.mobile.data.reports.utils.ParamsNetwork.URL_LOGIN
import rs.readahead.washington.mobile.data.reports.utils.ParamsNetwork.URL_PROJECTS
import rs.readahead.washington.mobile.data.repository.SkippableMediaFileRequestBody
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo
import rs.readahead.washington.mobile.domain.entity.reports.ProjectResult
import rs.readahead.washington.mobile.domain.entity.reports.ReportPostResult
import rs.readahead.washington.mobile.domain.entity.reports.ReportsLoginResult
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.domain.repository.reports.ReportsRepository
import rs.readahead.washington.mobile.util.StringUtils
import rs.readahead.washington.mobile.util.Util
import timber.log.Timber
import java.net.URI
import java.net.UnknownHostException
import java.util.*
import javax.inject.Inject


class ReportsRepositoryImp @Inject internal constructor(
    private val apiService: ReportsApiService,
    private val okHttpClient: OkHttpClient
) :
    ReportsRepository {

    override fun login(server: TellaReportServer): Single<ReportsLoginResult> {
        return apiService.login(
            loginEntity = LoginEntity(server.username, server.password),
            url = StringUtils.append(
                '/',
                server.url,
                URL_LOGIN
            )
        ).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { result -> result.mapToDomainModel() }
    }

    override fun submitReport(
        server: TellaReportServer,
        reportBody: ReportBodyEntity,
        projectId: String
    ): Single<ReportPostResult> {
        return apiService.submitReport(
            reportBodyEntity = reportBody,
            url = StringUtils.append(
                '/',
                server.url,
                "$URL_PROJECTS/$projectId"
            ),
            access_token = server.accessToken
        )
    }

    override fun getProjects(
        limit: Int,
        offset: Int,
        servers: List<TellaReportServer>
    ): Single<List<ProjectResult>> {
        val projectsList = mutableListOf<ProjectResult>()
        return Observable
            .range(0, servers.size)
            .concatMap { serverIndex ->
                apiService.getProjects(
                    limit = limit,
                    offset = offset,
                    access_token = servers[serverIndex].accessToken,
                    url = StringUtils.append(
                        '/',
                        servers[serverIndex].url,
                        URL_PROJECTS
                    )
                ).toObservable()
            }
            .doOnNext { list ->
                projectsList.addAll(list)
            }
            .filter { list -> list.isNotEmpty() }
            .firstOrError()
    }

    override fun upload(
        mediaFile: VaultFile,
        server: TellaReportServer
    ): Flowable<UploadProgressInfo?> {
        return getStatus(mediaFile, server.url)
            .flatMapPublisher { skipBytes: Long ->
                appendFile(
                    mediaFile,
                    skipBytes,
                    server.url
                )
            }
            .onErrorReturn { throwable: Throwable? ->
                mapThrowable(
                    throwable, mediaFile
                )
            }
    }

    override fun check(baseUrl: String): Single<UploadProgressInfo?> {
        val vaultFile = VaultFile()
        vaultFile.name = "test"
        return getStatus(vaultFile, baseUrl)
            .map { aLong: Long? ->
                UploadProgressInfo(
                    vaultFile,
                    0,
                    0
                )
            }
            .onErrorReturn { throwable: Throwable? ->
                mapThrowable(
                    throwable!!, vaultFile
                )
            }
    }

    private fun getStatus(vaultFile: VaultFile, baseUrl: String): Single<Long> {
        val request: Request = Request.Builder()
            .url(getUploadUrl(vaultFile.name, URI.create(baseUrl)))
            .head()
            .build()
        return Single.create { emitter: SingleEmitter<Long> ->
            try {
                val response: Response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val skip = Util.parseLong(
                        response.header("content-length"),
                        0
                    )
                    emitter.onSuccess(skip)
                    return@create
                }
                emitter.onError(UploadError(response))
            } catch (e: Exception) {
                emitter.onError(UploadError(e))
            }
        }
    }

    private fun appendFile(
        vaultFile: VaultFile,
        skipBytes: Long,
        baseUrl: String
    ): Flowable<UploadProgressInfo?> {
        return Flowable.create({ emitter: FlowableEmitter<UploadProgressInfo?> ->
            try {
                val size = vaultFile.size
                val fileName = vaultFile.name
                val uploadEmitter = UploadEmitter()
                emitter.onNext(
                    UploadProgressInfo(
                        vaultFile,
                        skipBytes,
                        UploadProgressInfo.Status.STARTED
                    )
                )
                val appendRequest: Request = Request.Builder()
                    .url(getUploadUrl(fileName, URI.create(baseUrl)))
                    .put(
                        SkippableMediaFileRequestBody(
                            vaultFile, skipBytes
                        ) { current: Long, total: Long ->
                            uploadEmitter.emit(
                                emitter,
                                vaultFile,
                                skipBytes + current,
                                size
                            )
                        }
                    )
                    .build()
                var response: Response = okHttpClient.newCall(appendRequest).execute()
                if (!response.isSuccessful) {
                    emitter.onError(UploadError(response))
                    return@create
                }
                val closeRequest: Request = Request.Builder()
                    .url(getUploadUrl(fileName, URI.create(baseUrl)))
                    .header("content-length", "0")
                    .post(RequestBody.create(null, ByteArray(0)))
                    .build()
                response = okHttpClient.newCall(closeRequest).execute()
                if (!response.isSuccessful) {
                    emitter.onError(UploadError(response))
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

    private fun getUploadUrl(name: String, baseUrl: URI): String {
        return baseUrl.resolve("/").resolve(name).toString()
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

        constructor(response: Response) : super(
            String.format(
                Locale.ROOT, "Request failed, response code: %d", response.code()
            )
        ) {
            code = response.code()
        }

        constructor(cause: Throwable?) : super(cause)
    }


}