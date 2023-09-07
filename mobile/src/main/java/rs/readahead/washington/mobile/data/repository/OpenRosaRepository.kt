package rs.readahead.washington.mobile.data.repository

import android.content.Context
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.apache.commons.io.IOUtils
import org.javarosa.core.model.FormDef
import org.javarosa.core.model.IDataReference
import org.javarosa.core.services.transport.payload.ByteArrayPayload
import org.javarosa.model.xform.XFormSerializingVisitor
import org.javarosa.model.xform.XPathReference
import retrofit2.Response
import rs.readahead.washington.mobile.data.entity.OpenRosaResponseEntity
import rs.readahead.washington.mobile.data.entity.XFormsEntity
import rs.readahead.washington.mobile.data.entity.mapper.OpenRosaDataMapper
import rs.readahead.washington.mobile.data.openrosa.OpenRosaService
import rs.readahead.washington.mobile.domain.entity.IErrorBundle
import rs.readahead.washington.mobile.domain.entity.IProgressListener
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFileStatus
import rs.readahead.washington.mobile.domain.entity.collect.ListFormResult
import rs.readahead.washington.mobile.domain.entity.collect.NegotiatedCollectServer
import rs.readahead.washington.mobile.domain.entity.collect.OpenRosaPartResponse
import rs.readahead.washington.mobile.domain.entity.collect.OpenRosaResponse
import rs.readahead.washington.mobile.domain.repository.IOpenRosaRepository
import rs.readahead.washington.mobile.util.C
import rs.readahead.washington.mobile.util.StringUtils
import java.io.IOException
import java.util.Locale

class OpenRosaRepository : IOpenRosaRepository {

    /**
     * Fetch list of forms from given [server].
     *
     * @param server CollectServer from which we fetch the forms.
     */
    override fun formList(server: CollectServer): Single<ListFormResult> {
        return OpenRosaService.newInstance(server.username, server.password)
            .services.formList(null, StringUtils.append('/', server.url, FORM_LIST_PATH))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { formsEntity: XFormsEntity ->
                val mapper = OpenRosaDataMapper()
                val forms = formsEntity.xforms.map { form ->
                    CollectForm(server.id, mapper.transform(form))
                }
                ListFormResult().apply { this.forms = forms }
            }
            .onErrorResumeNext { throwable: Throwable? ->
                val listFormResult = ListFormResult()
                listFormResult.apply {
                    val errorBundle = ErrorBundle(throwable)
                    errorBundle.serverId = server.id
                    errorBundle.serverName = server.name
                    this.errors = listOf<IErrorBundle>(errorBundle)
                }
                Single.just(listFormResult)
            }
    }

    /**
     * Download specific FormDef of a [form] from given [server].
     *
     * @param server CollectServer from which we download the FormDef.
     * @param form CollectForm for which we retrieve the FormDef.
     */
    override fun getFormDef(server: CollectServer, form: CollectForm): Single<FormDef> {
        return OpenRosaService.newInstance(server.username, server.password)
            .services.getFormDef(null, form.form.downloadUrl)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { response: ResponseBody? ->
                OpenRosaDataMapper().transform(
                    response
                )
            }
            .onErrorResumeNext { throwable: Throwable? ->
                val errorBundle = XErrorBundle(throwable)
                Single.error(errorBundle)
            }
    }

    /**
     * Retrieves the NegotiatedCollectServer specified [server]
     *
     * @param server Specified CollectServer
     */
    override fun submitFormNegotiate(server: CollectServer): Single<NegotiatedCollectServer> {
        // todo: InstanceColumns.SUBMISSION_URI? url in form
        return OpenRosaService.newInstance(server.username, server.password)
            .services.submitFormNegotiate(
                null,
                StringUtils.append('/', server.url, SUBMISSION_PATH)
            )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { response: Response<Void?>? ->
                OpenRosaDataMapper().transform(
                    server,
                    response
                )
            }
            .onErrorResumeNext { exception: Throwable? ->
                Single.error(
                    exception
                )
            }
    }

    /**
     * Submits [instance] to the ODK [server]
     *
     * @param context Context
     * @param server NegotiatedCollectServer to which the CollectFormInstance is submitted.
     * @param instance CollectFormInstance being submitted.
     */
    override fun submitForm(
        context: Context,
        server: NegotiatedCollectServer,
        instance: CollectFormInstance
    ): Single<OpenRosaResponse> {
        val parts: MutableMap<String, RequestBody>
        try {
            parts = getSerializedFormData(instance.formDef)
            parts += getMediaAttachmentsForSubmission(instance)
        } catch (e: IOException) {
            return Single.error(e)
        }
        val url = if (server.isUrlNegotiated) server.url else StringUtils.append(
            '/',
            server.url,
            SUBMISSION_PATH
        )
        return OpenRosaService.newInstance(server.username, server.password)
            .services.submitForm(null, url, parts)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { response: Response<OpenRosaResponseEntity?>? ->
                OpenRosaDataMapper().transform(
                    response
                )
            }
            .onErrorResumeNext { exception: Throwable? ->
                Single.error(
                    exception
                )
            }
    }

    private fun getSerializedFormData(formDef: FormDef): MutableMap<String, RequestBody> {
        val parts: MutableMap<String, RequestBody> = LinkedHashMap()

        val serializer = XFormSerializingVisitor()
        val formInstance = formDef.instance
        val payload = serializer.createSerializedPayload(
            formInstance,
            getSubmissionDataReference(formDef)
        ) as ByteArrayPayload

        parts[getPartKey(C.OPEN_ROSA_XML_PART_NAME, "${C.OPEN_ROSA_XML_PART_NAME}.xml")] =
            RequestBody.create(
                MediaType.parse("text/xml"),
                IOUtils.toByteArray(payload.payloadStream)
            )

        return parts
    }

    private fun getMediaAttachmentsForSubmission(instance: CollectFormInstance): List<Pair<String, RequestBody>> {
        return instance.widgetMediaFiles
            .filter { attachment -> attachment.uploading && attachment.status != FormMediaFileStatus.SUBMITTED }
            .map { attachment ->
                val filename = attachment.name
                String.format(
                    Locale.ROOT,
                    "%s\"; filename=\"%s",
                    filename,
                    filename
                ) to MediaFileRequestBody(attachment)
            }
    }

    /**
     * Submits part of the [instance] to the ODK [server]
     *
     * @param server NegotiatedCollectServer to which the CollectFormInstance is submitted.
     * @param instance CollectFormInstance being submitted.
     * @param attachment Optional FormMediaFile as a part of submission.
     * @param progressListener Optional IProgressListener.
     * @return Single<OpenRosaPartResponse> representing the server response for the specific part.
     */
    override fun submitFormGranular(
        server: NegotiatedCollectServer,
        instance: CollectFormInstance,
        attachment: FormMediaFile?,
        progressListener: IProgressListener?
    ): Single<OpenRosaPartResponse> {
        val parts: MutableMap<String, RequestBody>
        try {
            parts = getSerializedFormData(instance.formDef)
            parts += getSerializedFormData(instance.formDef, attachment, progressListener)
        } catch (e: IOException) {
            return Single.error(e)
        }
        val url = if (server.isUrlNegotiated) server.url else StringUtils.append(
            '/',
            server.url,
            SUBMISSION_PATH
        )
        val partName = if (attachment != null) attachment.partName else C.OPEN_ROSA_XML_PART_NAME
        return OpenRosaService.newInstance(server.username, server.password)
            .services.submitForm(null, url, parts)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { response: Response<OpenRosaResponseEntity?>? ->
                OpenRosaDataMapper().transform(
                    response,
                    partName
                )
            }
            .onErrorResumeNext { exception: Throwable? ->
                Single.error(
                    exception
                )
            }
    }

    private fun getSerializedFormData(
        formDef: FormDef,
        attachment: FormMediaFile?,
        progressListener: IProgressListener?
    ): Map<String, RequestBody> {
        val parts: MutableMap<String, RequestBody> = LinkedHashMap()

        val serializer = XFormSerializingVisitor()
        val formInstance = formDef.instance
        val payload = serializer.createSerializedPayload(
            formInstance,
            getSubmissionDataReference(formDef)
        ) as ByteArrayPayload

        parts[getPartKey(C.OPEN_ROSA_XML_PART_NAME, "${C.OPEN_ROSA_XML_PART_NAME}.xml")] =
            RequestBody.create(
                MediaType.parse("text/xml"),
                IOUtils.toByteArray(payload.payloadStream)
            )

        attachment?.let {
            parts[getPartKey(it.partName, it.id)] = MediaFileRequestBody(it, progressListener)
        }

        return parts
    }

    private fun getPartKey(partName: String, filename: String): String {
        return String.format(
            Locale.ROOT,
            "%s\"; filename=\"%s",
            partName,
            filename
        ) // wtf, OkHttp3 :)
    }

    // todo: submission profile for SMS? check it out
    private fun getSubmissionDataReference(formDef: FormDef): IDataReference {
        // Determine the information about the submission...
        val p = formDef.submissionProfile
        return if (p == null || p.ref == null) {
            XPathReference("/")
        } else {
            p.ref
        }
    }

    companion object {
        private const val FORM_LIST_PATH = "formList"
        private const val SUBMISSION_PATH = "submission"
    }
}