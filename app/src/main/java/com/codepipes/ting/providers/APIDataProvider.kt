package com.codepipes.ting.providers

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.AsyncTask
import android.widget.Toast
import com.codepipes.ting.dialogs.messages.TingToast
import com.codepipes.ting.dialogs.messages.TingToastType
import com.codepipes.ting.models.Branch
import com.codepipes.ting.utils.Routes
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request

@SuppressLint("StaticFieldLeak")
class APILoadGlobalRestaurants(val context: Context) : AsyncTask<Void, Void, MutableList<Branch>?>() {

    private val routes: Routes = Routes()
    private val gson: Gson = Gson()

    override fun doInBackground(vararg params: Void?): MutableList<Branch>? {
        val url = routes.restaurantsGlobal
        val client = OkHttpClient()

        val request = Request.Builder().url(url).get().build()
        return try {
            val response = client.newCall(request).execute()
            val dataString = response.body()!!.string()
            gson.fromJson<MutableList<Branch>>(dataString, object : TypeToken<MutableList<Branch>>(){}.type)
        } catch (e: Exception){
            val activity = context as Activity
            activity.runOnUiThread {
                TingToast(
                    context,
                    e.message!!,
                    TingToastType.ERROR
                ).showToast(Toast.LENGTH_LONG)
            }
            null
        }
    }
}