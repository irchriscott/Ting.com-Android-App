package com.codepipes.ting.providers

import android.content.Context
import android.net.NetworkInfo
import android.content.Context.CONNECTIVITY_SERVICE
import androidx.core.content.ContextCompat.getSystemService
import android.net.ConnectivityManager
import androidx.annotation.NonNull
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException


internal class OfflineResponseCacheInterceptor(private val mContext: Context) : Interceptor {

    var maxStale = 60 * 60 * 24 * 28

    private val isNetworkAvailable: Boolean
        get() {
            val connectivityManager =
                mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }

    @Throws(IOException::class)
    override fun intercept(@NonNull chain: Interceptor.Chain): Response {

        var request = chain.request()
        if (java.lang.Boolean.valueOf(request.header("ApplyOfflineCache"))) {
            if (!isNetworkAvailable) {
                request = request.newBuilder()
                    .removeHeader("ApplyOfflineCache")
                    .header("Cache-Control", "public, only-if-cached, maxStale = $maxStale").build()
            }
        }

        return chain.proceed(request)
    }
}