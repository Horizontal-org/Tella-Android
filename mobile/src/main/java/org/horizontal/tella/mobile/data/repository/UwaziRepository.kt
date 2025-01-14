package org.horizontal.tella.mobile.data.repository

import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.horizontal.tella.mobile.data.ParamsNetwork
import org.horizontal.tella.mobile.data.entity.uwazi.LanguageEntity
import org.horizontal.tella.mobile.data.entity.uwazi.LoginEntity
import org.horizontal.tella.mobile.data.entity.uwazi.LoginResult
import org.horizontal.tella.mobile.data.entity.uwazi.UwaziEntityRow
import org.horizontal.tella.mobile.data.entity.uwazi.mapper.mapToDomainModel
import org.horizontal.tella.mobile.data.uwazi.UwaziConstants.UWAZI_DATATYPE_TEMPLATE
import org.horizontal.tella.mobile.data.uwazi.UwaziService
import org.horizontal.tella.mobile.domain.entity.UWaziUploadServer
import org.horizontal.tella.mobile.domain.entity.uwazi.CollectTemplate
import org.horizontal.tella.mobile.domain.entity.uwazi.ListTemplateResult
import org.horizontal.tella.mobile.domain.entity.uwazi.RelationShipRow
import org.horizontal.tella.mobile.domain.entity.uwazi.RowDictionary
import org.horizontal.tella.mobile.domain.entity.uwazi.Settings
import org.horizontal.tella.mobile.domain.entity.uwazi.TranslationRow
import org.horizontal.tella.mobile.domain.entity.uwazi.UwaziRow
import org.horizontal.tella.mobile.domain.repository.uwazi.IUwaziUserRepository
import org.horizontal.tella.mobile.util.StringUtils
import timber.log.Timber

class UwaziRepository : IUwaziUserRepository {
    private val uwaziApi by lazy { UwaziService.newInstance().services }

    override fun login(server: UWaziUploadServer): Single<LoginResult> {
        return uwaziApi.login(
            loginEntity = LoginEntity(server.username, server.password, server.token),
            url = StringUtils.append(
                '/',
                server.url,
                ParamsNetwork.URL_LOGIN
            )
        ).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                val cookieList: List<String> = it.headers().values("Set-Cookie")
                var jsessionid = ""
                if (cookieList.isNotEmpty()) {
                    jsessionid = cookieList[0].split(";")[0]
                }

                LoginResult(it.isSuccessful, jsessionid, it.code())
            }
    }

    override fun getTemplatesResult(server: UWaziUploadServer): Single<ListTemplateResult> {
        return Single.zip(
            getTemplates(server),
            getRelationShipEntities(server),
            getDictionary(server),
            getTranslation(server),
            getFullSettings(server)
        ) { templates, relationShipEntities, dictionary, translations, settings ->

            templates.forEach { template ->
                template.properties.forEach { property ->
                    dictionary.forEach { dictionaryItem ->
                        if (dictionaryItem._id == property.content) {
                            property.values = dictionaryItem.values
                        }
                    }
                }
            }
            var resultTemplates = mutableListOf<UwaziRow>()

            if (server.username.isNullOrEmpty() || server.password.isNullOrEmpty()) {
                if (settings.allowedPublicTemplates.isNotEmpty()) {
                    templates.forEach { row ->
                        settings.allowedPublicTemplates.forEach { id ->
                            if (row._id == id) {
                                resultTemplates.add(row)
                            }
                        }
                    }
                }
            } else {
                resultTemplates = templates.toMutableList()
            }

            resultTemplates.forEach { template ->
                translations.filter { row -> row.locale == server.localeCookie }[0]
                    .contexts.forEach { context ->
                        if (context.id == template._id) {
                            template.properties.forEach { property ->
                                property.translatedLabel = context.values[property.label] ?: ""
                            }
                            template.commonProperties.forEach { property ->
                                property.translatedLabel = context.values[property.label] ?: ""
                            }

                            template.translatedName = context.values[template.name] ?: ""
                        } else {
                            template.properties.forEach { property ->
                                property.values?.forEach { selectValue ->
                                    if (context.id == property.content) {
                                        selectValue.translatedLabel =
                                            context.values[selectValue.label]
                                                ?: selectValue.label
                                    }

                                    selectValue.values.forEach { nestedSelectValue ->
                                        if (context.id == property.content) {
                                            nestedSelectValue.translatedLabel =
                                                context.values[nestedSelectValue.label]
                                                    ?: nestedSelectValue.label
                                        }

                                    }

                                }
                            }
                        }

                    }
            }

            val listTemplates = mutableListOf<CollectTemplate>()
            resultTemplates.forEach { template ->
                val relationShipEntity: List<RelationShipRow> =
                    relationShipEntities.filter { row -> row.type == UWAZI_DATATYPE_TEMPLATE }
                template.properties.forEach { property ->
                    val listRelationShipEntities =
                        relationShipEntity.find { (property.content == it.id) }
                    listRelationShipEntities.also { property.entities = it?.values }
                }
                val collectTemplate = CollectTemplate(
                    serverId = server.id,
                    entityRow = template,
                    serverName = server.name
                )
                listTemplates.add(collectTemplate)
            }

            val listTemplateResult = ListTemplateResult()
            listTemplateResult.templates = listTemplates
            listTemplateResult
        }.onErrorResumeNext { throwable: Throwable? ->
            val listTemplateResult = ListTemplateResult()
            val errorBundle = ErrorBundle(throwable)
            errorBundle.serverId = server.id
            errorBundle.serverName = server.name
            listTemplateResult.errors = listOf(errorBundle)
            Single.just(listTemplateResult)
        }
    }

    override fun getRelationShipEntities(server: UWaziUploadServer): Single<List<RelationShipRow>> {
        val cookies = server.connectCookie + ";locale=" + server.localeCookie
        return uwaziApi.getRelationShipEntities(
            url = StringUtils.append(
                '/',
                server.url,
                ParamsNetwork.URL_THESAURIS
            ), cookies = cookies
        )
            .subscribeOn(Schedulers.io())
            .map { result ->
                result.mapToDomainModel()
            }
            .onErrorReturn { error ->
                Timber.e(error, "getRelationShipEntities failed")
                emptyList()
            }
    }

    override fun getTemplates(server: UWaziUploadServer): Single<List<UwaziRow>> {
        val listCookies = ArrayList<String>()
        listCookies.add(server.connectCookie)
        listCookies.add(server.localeCookie)
        val url = StringUtils.append('/', server.url, ParamsNetwork.URL_TEMPLATES)
        return uwaziApi.getTemplates(
            url, cookies = listCookies
        )
            .subscribeOn(Schedulers.io())
            .map { result -> result.mapToDomainModel() }
    }

    override fun getTemplates(url: String): Single<List<UwaziRow>> {
        return uwaziApi.getTemplates(
            url = StringUtils.append(
                '/',
                url,
                ParamsNetwork.URL_TEMPLATES
            ), cookies = emptyList()
        )
            .subscribeOn(Schedulers.io())
            .map { result -> result.mapToDomainModel() }
    }

    override fun getSettings(server: UWaziUploadServer): Single<List<LanguageEntity>> {
        return uwaziApi.getSettings(
            url = StringUtils.append(
                '/',
                server.url,
                ParamsNetwork.URL_SETTINGS
            ),
            cookies = arrayListOf(server.connectCookie, server.localeCookie)
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { result -> result.languages }
    }

    override fun getSettings(url: String): Single<Settings> {
        return uwaziApi.getSettings(
            url = StringUtils.append(
                '/',
                url,
                ParamsNetwork.URL_SETTINGS
            ),
            cookies = arrayListOf()
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { result -> result.mapToDomainModel() }
    }

    override fun getFullSettings(server: UWaziUploadServer): Single<Settings> {
        return uwaziApi.getSettings(
            url = StringUtils.append(
                '/',
                server.url,
                ParamsNetwork.URL_SETTINGS
            ),
            cookies = arrayListOf(server.connectCookie, server.localeCookie)
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { result -> result.mapToDomainModel() }
    }

    override fun getDictionary(server: UWaziUploadServer): Single<List<RowDictionary>> {
        return uwaziApi.getDictionary(
            url = StringUtils.append(
                '/',
                server.url,
                ParamsNetwork.URL_DICTIONARIES
            ),
            cookies = arrayListOf(server.connectCookie, server.localeCookie)
        )
            .subscribeOn(Schedulers.io())
            .map { result -> result.mapToDomainModel() }
    }

    override fun getTranslation(server: UWaziUploadServer): Single<List<TranslationRow>> {
        return uwaziApi.getTranslations(
            url = StringUtils.append(
                '/',
                server.url,
                ParamsNetwork.URL_TRANSLATION
            ),
            cookies = arrayListOf(server.connectCookie, server.localeCookie)
        )
            .subscribeOn(Schedulers.io())
            .map { result -> result.mapToDomainModel() }
    }

    override fun submitEntity(
        server: UWaziUploadServer,
        entity: RequestBody,
        attachments: List<MultipartBody.Part?>,
        attachmentsOriginalName: List<String>,
        documents: List<MultipartBody.Part?>
    ): Single<UwaziEntityRow> {
        return uwaziApi.submitEntity(
            attachments = attachments,
            documents = documents,
            attachmentsOriginalName = attachmentsOriginalName,
            entity = entity,
            url = StringUtils.append(
                '/',
                server.url,
                ParamsNetwork.URL_ENTITIES
            ),
            cookies = arrayListOf(server.connectCookie, server.localeCookie),
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess { }
    }

    override fun submitWhiteListedEntity(
        server: UWaziUploadServer,
        entity: RequestBody,
        attachments: List<MultipartBody.Part?>,
        attachmentsOriginalName: List<String>,
        documents: List<MultipartBody.Part?>
    ): Single<UwaziEntityRow> {
        return uwaziApi.submitWhiteListedEntity(
            attachments = attachments,
            documents = documents,
            entity = entity,
            url = StringUtils.append(
                '/',
                server.url,
                ParamsNetwork.URL_WHITE_LISTED_ENTITIES
            ),
            cookies = arrayListOf(server.connectCookie, server.localeCookie),
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }


}

