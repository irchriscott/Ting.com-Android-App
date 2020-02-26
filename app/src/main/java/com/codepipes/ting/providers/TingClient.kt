package com.codepipes.ting.providers

import android.annotation.SuppressLint
import android.content.Context
import com.codepipes.ting.models.Order
import com.codepipes.ting.models.RestaurantMenu
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.UtilsFunctions
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.Retrofit
import javax.xml.datatype.DatatypeConstants.SECONDS
import okhttp3.OkHttpClient
import io.reactivex.Observable
import okhttp3.Cache
import okhttp3.Interceptor
import java.util.concurrent.TimeUnit


class TingClient (val context: Context) {

    private val userAuthentication = UserAuthentication(context)
    private val session = userAuthentication.get()!!
    private val utilsFunctions = UtilsFunctions(context)

    private lateinit var tingService: TingService

    init { this.initClient() }

    private fun initClient() {

        val cacheSize = 10 * 1024 * 1024
        val cache = Cache(context.applicationContext.cacheDir, cacheSize.toLong())


        val interceptor = Interceptor { chain ->
            var request = chain.request()
            if (!utilsFunctions.isConnectedToInternet()) {
                if (request.header("No-Authentication") == null) {
                    val maxStale = 60 * 60 * 24 * 28
                    request = request
                        .newBuilder()
                        .header("Cache-Control", "public, only-if-cached, max-stale=$maxStale")
                        .header("Authorization", session.token!!)
                        .build()
                }
            } else {
                if (request.header("No-Authentication") == null) {
                    request = request
                        .newBuilder()
                        .header("Cache-Control", "public, max-age=" + 60)
                        .header("Authorization", session.token!!)
                        .build()
                }
            }
            chain.proceed(request)
        }

        val client = OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(interceptor)
            .addNetworkInterceptor(ResponseCacheInterceptor())
            .addInterceptor(OfflineResponseCacheInterceptor(context))
            .build()

        val retrofit = Retrofit.Builder()
            .client(client)
            .baseUrl(Routes().HOST_END_POINT)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        tingService = retrofit.create(TingService::class.java)
    }

    public fun getRestaurantTopMenus(branch: Int) : Observable<MutableList<RestaurantMenu>> {
        return tingService.getRestaurantTopMenus(branch)
    }

    public fun getPromotionPromotedMenus(promo: Int) : Observable<MutableList<RestaurantMenu>> {
        return tingService.getPromotionPromotedMenus(promo)
    }

    public fun getOrdersMenuPlacement(token: String, authorization: String) : Observable<MutableList<Order>> {
        return tingService.getOrdersMenuPlacement(token, authorization)
    }

    companion object {
        public fun getInstance(context: Context): TingClient = TingClient(context)
    }
}