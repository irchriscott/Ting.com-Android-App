package com.codepipes.ting.providers

import android.support.annotation.NonNull
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class ResponseCacheInterceptor : Interceptor {

    @Throws(IOException::class)
    override fun intercept(@NonNull chain: Interceptor.Chain): Response {
        val request = chain.request()
        return if (!java.lang.Boolean.valueOf(request.header("ApplyResponseCache"))) {
            val originalResponse = chain.proceed(chain.request())
            originalResponse.newBuilder()
                .removeHeader("ApplyResponseCache")
                .header("Cache-Control", "public, max-age=" + 60)
                .build()
        } else { chain.proceed(chain.request()) }
    }
}