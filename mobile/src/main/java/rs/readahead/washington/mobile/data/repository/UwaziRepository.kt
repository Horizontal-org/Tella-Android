package rs.readahead.washington.mobile.data.repository

import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.SingleOnSubscribe
import io.reactivex.SingleSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.Response
import rs.readahead.washington.mobile.data.ParamsNetwork
import rs.readahead.washington.mobile.data.entity.XFormEntity
import rs.readahead.washington.mobile.data.entity.XFormsEntity
import rs.readahead.washington.mobile.data.entity.mapper.OpenRosaDataMapper
import rs.readahead.washington.mobile.data.entity.uwazi.LoginEntity
import rs.readahead.washington.mobile.data.entity.uwazi.TemplateResponse
import rs.readahead.washington.mobile.data.openrosa.OpenRosaService
import rs.readahead.washington.mobile.data.upload.TUSClient
import rs.readahead.washington.mobile.data.uwazi.UwaziService
import rs.readahead.washington.mobile.domain.entity.LoginResponse
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer
import rs.readahead.washington.mobile.domain.entity.collect.ListFormResult
import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate
import rs.readahead.washington.mobile.domain.entity.uwazi.ListTemplateResult
import rs.readahead.washington.mobile.domain.repository.uwazi.IUwaziUserRepository
import rs.readahead.washington.mobile.util.StringUtils
import rs.readahead.washington.mobile.util.Util
import java.lang.Exception
import java.util.ArrayList

class UwaziRepository : IUwaziUserRepository {

    private val uwaziApi by lazy {UwaziService.newInstance().services}


    override fun login(server: UWaziUploadServer): Single<LoginResponse> {
       return uwaziApi.login( loginEntity = LoginEntity(server.username,server.password),
           url = StringUtils.append(
               '/',
               server.url,
               ParamsNetwork.URL_LOGIN
           )).subscribeOn(Schedulers.io())
           .observeOn(AndroidSchedulers.mainThread())
           .map { LoginResponse(it.success) }

    }

    override fun getTemplates(server: UWaziUploadServer): Single<ListTemplateResult> {
        return uwaziApi.getTemplates(url = StringUtils.append(
            '/',
            server.url,
            ParamsNetwork.URL_TEMPLATES
        ), cookie = "connect.sid="+ "s%3A_tcG2esa0JgXAi7SOH-ZzSC77CF9V6LW.YsBo04ABLviq0FIjAg9yxxdtXyuicXMlOTtllhVNddE")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map{
                val listTemplates = mutableListOf<CollectTemplate>()
                it.rows.forEach { entity ->
                    val collectTemplate = CollectTemplate(server.id,entity)
                    collectTemplate.serverName = server.name
                    listTemplates.add(collectTemplate)
                }

                val listTemplateResult = ListTemplateResult()
                listTemplateResult.templates = listTemplates
                listTemplateResult
            }
            .onErrorResumeNext { throwable: Throwable? ->
                val listTemplateResult = ListTemplateResult()
                val errorBundle = ErrorBundle(throwable)
                errorBundle.serverId = server.id
                errorBundle.serverName = server.name
                listTemplateResult.errors = listOf(errorBundle)
                Single.just(listTemplateResult)
            }
    }

    /*

    .onErrorResumeNext { throwable: Throwable? ->
                val listFormResult = ListFormResult()
                val errorBundle = ErrorBundle(throwable)
                errorBundle.serverId = uWaziUploadServer.getId()
                errorBundle.serverName = uWaziUploadServer.getName()
                listFormResult.errors = listOf(errorBundle)
                Single.just(listFormResult)
            }

    override suspend fun login(uWaziUploadServer: UWaziUploadServer) =
            flow {
               val loginResponse =
                   uwaziApi.login(
                       loginEntity = LoginEntity(uWaziUploadServer.username,uWaziUploadServer.password),
                       url = StringUtils.append(
                           '/',
                           uWaziUploadServer.url,
                           ParamsNetwork.URL_LOGIN
                       )

                   )
                if(loginResponse.success){
                    emit(loginResponse)
                }

            }.flowOn(Dispatchers.IO)


    override suspend fun getTemplates(uWaziUploadServer: UWaziUploadServer): Flow<TemplateResponse> =
        flow {
            val templateResponse = uwaziApi.getTemplates(url = StringUtils.append(
                '/',
                uWaziUploadServer.url,
                ParamsNetwork.URL_TEMPLATES
            ), cookie = "connect.sid="+ "s%3A_tcG2esa0JgXAi7SOH-ZzSC77CF9V6LW.YsBo04ABLviq0FIjAg9yxxdtXyuicXMlOTtllhVNddE")
            emit(templateResponse)
        }.flowOn(Dispatchers.IO)*/

}