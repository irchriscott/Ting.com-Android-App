package com.codepipes.ting.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import com.codepipes.ting.activities.moment.ShareMoment
import com.codepipes.ting.dialogs.messages.TingToast
import com.codepipes.ting.dialogs.messages.TingToastType
import com.codepipes.ting.models.ServerResponse
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.providers.UserPlacement
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.UtilsFunctions
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.TimeUnit

class UploadMomentService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("DefaultLocale")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val userAuthentication = UserAuthentication(this@UploadMomentService)
        val session = userAuthentication.get()!!

        val utilsFunctions = UtilsFunctions(this@UploadMomentService)
        val content = intent?.getStringExtra(ShareMoment.MOMENT_CONTENT_TEXT_KEY)
        val fileType = intent?.getIntExtra(ShareMoment.MOMENT_FILE_TYPE_KEY, 1)

        val placement = UserPlacement(this@UploadMomentService)

        if(placement.isPlacedIn()) {

            try {

                val image = File(intent?.getStringExtra(ShareMoment.MOMENT_FILE_PATH_KEY)?:"")

                val url = Routes.momentsSave
                val client = OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build()

                val imageName = "moment_${utilsFunctions.getToken(12).toLowerCase()}.png"

                val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("token", placement.getToken()!!)
                    .addFormDataPart("media_type", if(fileType == 1) { "image" } else { "video" })
                    .addFormDataPart("text", content?:"")
                    .addFormDataPart("media", imageName, RequestBody.create(
                        if(fileType == 1) { MEDIA_TYPE_PNG } else { MEDIA_TYPE_VIDEO }, image)).build()

                val request = Request.Builder()
                    .header("Authorization", session.token!!)
                    .url(url)
                    .post(requestBody)
                    .build()


                client.newCall(request).enqueue(object : Callback {

                    override fun onFailure(call: Call, e: IOException) {
                        Handler(Looper.getMainLooper()).post(Runnable {
                            TingToast(
                                this@UploadMomentService,
                                e.message!!,
                                TingToastType.ERROR
                            ).showToast(Toast.LENGTH_LONG)
                        })
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val responseBody = response.body!!.string()
                        val gson = Gson()
                        try {
                            val serverResponse = gson.fromJson(responseBody, ServerResponse::class.java)
                            Handler(Looper.getMainLooper()).post(Runnable {
                                if (serverResponse.status == 200){
                                    userAuthentication.set(gson.toJson(serverResponse.user))
                                    TingToast(
                                        this@UploadMomentService,
                                        serverResponse.message,
                                        TingToastType.SUCCESS
                                    ).showToast(Toast.LENGTH_LONG)
                                } else { TingToast(
                                    this@UploadMomentService,
                                    serverResponse.message,
                                    TingToastType.SUCCESS
                                ).showToast(Toast.LENGTH_LONG) }
                            })
                        } catch (e: Exception){
                            Handler(Looper.getMainLooper()).post(Runnable {
                                TingToast(
                                    this@UploadMomentService,
                                    "An Error Has Occurred",
                                    TingToastType.ERROR
                                ).showToast(Toast.LENGTH_LONG)
                            })
                        }
                    }
                })

            } catch (e: FileNotFoundException) {
                Handler(Looper.getMainLooper()).post(Runnable {
                    TingToast(
                        this,
                        e.message!!,
                        TingToastType.ERROR
                    ).showToast(Toast.LENGTH_LONG)
                })
            } catch (e: java.lang.Exception) {
                Handler(Looper.getMainLooper()).post(Runnable {
                    TingToast(
                        this,
                        e.message!!,
                        TingToastType.ERROR
                    ).showToast(Toast.LENGTH_LONG)
                })
            }
        }

        return Service.START_STICKY
    }

    companion object {
        private val MEDIA_TYPE_PNG = "image/png".toMediaTypeOrNull()
        private val MEDIA_TYPE_VIDEO = "video/*".toMediaTypeOrNull()
    }
}