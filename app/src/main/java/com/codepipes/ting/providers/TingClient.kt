package com.codepipes.ting.providers

import android.annotation.SuppressLint
import android.content.Context
import com.codepipes.ting.models.Bill
import com.codepipes.ting.models.Order
import com.codepipes.ting.models.RestaurantMenu
import com.codepipes.ting.models.ServerResponse
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.UtilsFunctions
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.Retrofit
import javax.xml.datatype.DatatypeConstants.SECONDS
import io.reactivex.Observable
import okhttp3.*
import java.io.IOException
import java.lang.Exception
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
            .baseUrl(Routes.HOST_END_POINT)
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

    public fun getRestaurantMenusPlacement(branch: Int, type: Int, query: String) : Observable<MutableList<RestaurantMenu>> {
        return tingService.getRestaurantMenusPlacement(branch, type, query)
    }

    public fun getOrdersMenuPlacement(token: String, query: String, authorization: String) : Observable<MutableList<Order>> {
        return tingService.getOrdersMenuPlacement(token, query, authorization)
    }

    public fun rePlaceOrderPlacement(order: Int, quantity: Int, conditions: String) : Observable<ServerResponse> {
        return tingService.rePlaceOrderPlacement(order, quantity, conditions)
    }

    public fun cancelOrderPlacement(order: Int) : Observable<ServerResponse> {
        return tingService.cancelOrderPlacement(order, order)
    }

    public fun getPlacementBill(token: String) : Observable<Bill> {
        return tingService.getPlacementBill(token)
    }

    companion object {

        public fun getInstance(context: Context): TingClient = TingClient(context)

        public fun getRequest(url: String, interceptor: Interceptor?, auth: String?, requestResponse: (status: Int, isSuccess: Boolean, result: String) -> Unit) {
            val clientBuilder = OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .callTimeout(60 * 5, TimeUnit.SECONDS)

            if (interceptor != null) { clientBuilder.addInterceptor(interceptor) }
            val client = clientBuilder.build()

            val requestBuilder = Request.Builder().url(url).get()
            if (auth != null) { requestBuilder.header("Authorization", auth) }
            val request = requestBuilder.build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    requestResponse.invoke(700, false, e.localizedMessage)
                }

                override fun onResponse(call: Call, response: Response) {
                    val dataString = response.body!!.string()
                    requestResponse.invoke(response.code, true, dataString)
                }
            })
        }

        public fun postRequest(url: String, formData: HashMap<String, String>?, interceptor: Interceptor?, auth: String?, requestResponse: (status: Int, isSuccess: Boolean, result: String) -> Unit) {
            val clientBuilder = OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .callTimeout(60 * 5, TimeUnit.SECONDS)

            if (interceptor != null) { clientBuilder.addInterceptor(interceptor) }
            val client = clientBuilder.build()

            val formBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)

            if (formData != null) {
                for ((k, v) in formData) {
                    formBuilder.addFormDataPart(k, v)
                }
            }

            val form = formBuilder.build()
            val requestBuilder = Request.Builder().url(url).post(form)
            if (auth != null) { requestBuilder.header("Authorization", auth) }
            val request = requestBuilder.build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    requestResponse.invoke(700, false, e.message!!)
                }

                override fun onResponse(call: Call, response: Response) {
                    val dataString = response.body!!.string()
                    requestResponse.invoke(response.code, true, dataString)
                }
            })
        }
    }
}