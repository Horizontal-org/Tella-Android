package rs.readahead.washington.mobile.data.repository

import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import rs.readahead.washington.mobile.data.ParamsNetwork
import rs.readahead.washington.mobile.data.entity.uwazi.Language
import rs.readahead.washington.mobile.data.entity.uwazi.LanguageSettingsEntity
import rs.readahead.washington.mobile.data.entity.uwazi.LoginEntity
import rs.readahead.washington.mobile.data.entity.uwazi.SettingsResponse
import rs.readahead.washington.mobile.data.uwazi.UwaziService
import rs.readahead.washington.mobile.domain.entity.LoginResponse
import rs.readahead.washington.mobile.domain.entity.UWaziUploadServer
import rs.readahead.washington.mobile.domain.entity.uwazi.CollectTemplate
import rs.readahead.washington.mobile.domain.entity.uwazi.ListTemplateResult
import rs.readahead.washington.mobile.domain.repository.uwazi.IUwaziUserRepository
import rs.readahead.washington.mobile.util.StringUtils

class UwaziRepository : IUwaziUserRepository {

    private val uwaziApi by lazy { UwaziService.newInstance().services }


    override fun login(server: UWaziUploadServer): Single<LoginResponse> {
        return uwaziApi.login(
            loginEntity = LoginEntity(server.username, server.password),
            url = StringUtils.append(
                '/',
                server.url,
                ParamsNetwork.URL_LOGIN
            )
        ).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { LoginResponse(it.success) }

    }

    override fun getTemplates(server: UWaziUploadServer): Single<ListTemplateResult> {
        return uwaziApi.getTemplates(
            url = StringUtils.append(
                '/',
                server.url,
                ParamsNetwork.URL_TEMPLATES
            ),
            cookie = "connect.sid=s%3A_Y7aLlCk0R1BGiqou27XijwG8MUb89kf.3LMlgwQx%2FOUofldt3orGmWloySylOOzNBbnfG0kL5H0"
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                val listTemplates = mutableListOf<CollectTemplate>()
                it.rows.forEach { entity ->
                    val collectTemplate = CollectTemplate(
                        serverId = server.id,
                        entityRow = entity,
                        serverName = server.name
                    )
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

    override fun getSettings(server: UWaziUploadServer): Single<List<Language>> {
        return uwaziApi.getSettings(
            url = StringUtils.append(
                '/',
                server.url,
                ParamsNetwork.URL_SETTINGS
            ),
            cookie = "connect.sid=s%3A_Y7aLlCk0R1BGiqou27XijwG8MUb89kf.3LMlgwQx%2FOUofldt3orGmWloySylOOzNBbnfG0kL5H0"
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { result -> result.languages }
    }

    override fun updateDefaultLanguage(languageSettingsEntity: LanguageSettingsEntity,server: UWaziUploadServer): Single<SettingsResponse> {
        return uwaziApi.updateDefaultLanguage(
            languageSettingsEntity = languageSettingsEntity,
            url = StringUtils.append(
                '/',
                server.url,
                ParamsNetwork.URL_TRANSLATE_SETTINGS
            ),
            cookie = "connect.sid=s%3A_Y7aLlCk0R1BGiqou27XijwG8MUb89kf.3LMlgwQx%2FOUofldt3orGmWloySylOOzNBbnfG0kL5H0"
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

}