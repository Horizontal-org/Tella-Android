package rs.readahead.washington.mobile.data.rest

import rs.readahead.washington.mobile.data.ParamsNetwork.UWAZI_BASE_URL

object UwaziApi : BaseApi() {

    private var builder = Builder().build(UWAZI_BASE_URL)

    private val api = getBaseRetrofit()
        .newBuilder()
        .baseUrl(UWAZI_BASE_URL)
        .build()
        .create(IUwaziApi::class.java)

    fun getApi(): IUwaziApi {
        return api
    }
}