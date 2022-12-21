package rs.readahead.washington.mobile.data.reports.repository

import com.hzontal.tella_vault.VaultFile
import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.data.entity.reports.LoginEntity
import rs.readahead.washington.mobile.data.entity.reports.ReportBodyEntity
import rs.readahead.washington.mobile.data.entity.reports.mapper.mapToDomainModel
import rs.readahead.washington.mobile.data.http.HttpStatus
import rs.readahead.washington.mobile.data.reports.remote.ReportsApiService
import rs.readahead.washington.mobile.data.reports.utils.ParamsNetwork.URL_LOGIN
import rs.readahead.washington.mobile.data.reports.utils.ParamsNetwork.URL_PROJECTS
import rs.readahead.washington.mobile.data.repository.SkippableMediaFileRequestBody
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo
import rs.readahead.washington.mobile.domain.entity.reports.ReportPostResult
import rs.readahead.washington.mobile.domain.entity.reports.TellaReportServer
import rs.readahead.washington.mobile.domain.repository.reports.ReportsRepository
import rs.readahead.washington.mobile.util.StringUtils
import rs.readahead.washington.mobile.util.Util
import timber.log.Timber
import java.net.UnknownHostException
import java.util.*
import javax.inject.Inject


class ReportsRepositoryImp @Inject internal constructor(
    private val apiService: ReportsApiService
) : ReportsRepository {

    override fun login(server: TellaReportServer, slug: String): Single<TellaReportServer> {
        return apiService.login(
            loginEntity = LoginEntity(server.username, server.password),
            url = server.url + URL_LOGIN
        ).flatMap { response ->
            apiService.getProjectSlug(
                url = server.url + slug,
                access_token = response.access_token
            ).map { result ->
                server.apply {
                    accessToken = response.access_token
                    projectId = result.id
                    projectName = result.name
                    projectSlug = result.slug
                }
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
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
            "file/$reportId/${vaultFile.name}"
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
            "$reportId${vaultFile.name}"
        )
        return getStatus(url, accessToken)
            .map { aLong: Long? ->
                UploadProgressInfo(
                    vaultFile,
                    0,
                    0
                )
            }
            .onErrorReturn { throwable: Throwable? ->
                mapThrowable(
                    throwable, vaultFile
                )
            }
    }

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
                val fileName = vaultFile.name
                val uploadEmitter = UploadEmitter()
                emitter.onNext(
                    UploadProgressInfo(
                        vaultFile,
                        skipBytes,
                        UploadProgressInfo.Status.STARTED
                    )
                )

                val file = SkippableMediaFileRequestBody(
                    vaultFile, skipBytes
                ) { current: Long, total: Long ->
                    uploadEmitter.emit(
                        emitter,
                        vaultFile,
                        skipBytes + current,
                        size
                    )
                }
                Timber.i("xf$baseUrl")
                var response = apiService.putFile(
                    file = file,
                    url = baseUrl,
                    access_token = accessToken
                ).blockingGet()

                if (!response.isSuccessful) {
                    emitter.onError(UploadError(response.code()))
                    return@create
                }

               response = apiService.postFile(
                    file = file,
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