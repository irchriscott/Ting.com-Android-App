package com.codepipes.ting.dialogs.user.edit

import android.icu.util.TimeZone
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.codepipes.ting.R
import com.codepipes.ting.dialogs.messages.ErrorMessage
import com.codepipes.ting.dialogs.messages.ProgressOverlay
import com.codepipes.ting.dialogs.messages.TingToast
import com.codepipes.ting.dialogs.messages.TingToastType
import com.codepipes.ting.models.ServerResponse
import com.codepipes.ting.models.User
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.utils.Routes
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class EditUserEmailAddress : DialogFragment(){

    private lateinit var mEmailOldInput: EditText
    private lateinit var mEmailNewInput: EditText
    private lateinit var mEmailPasswordInput: EditText

    private lateinit var mUpdateEmailButton: Button
    private lateinit var mCancelUpdateButton: Button

    private lateinit var user: User
    private lateinit var userAuthentication: UserAuthentication

    private val mProgressOverlay = ProgressOverlay()

    override fun onCreate(savedInstanceState: Bundle?) {
        this.isCancelable = true
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_user_edit_email, container, false)

        mEmailOldInput = view.findViewById(R.id.user_profile_edit_email_old)
        mEmailNewInput = view.findViewById(R.id.user_profile_edit_email_new)
        mEmailPasswordInput = view.findViewById(R.id.user_profile_edit_email_password)

        mUpdateEmailButton = view.findViewById(R.id.update_user_email)
        mCancelUpdateButton = view.findViewById(R.id.cancel_user_email)

        userAuthentication = UserAuthentication(activity!!)
        user = userAuthentication.get()!!

        mCancelUpdateButton.setOnClickListener { dialog?.dismiss() }
        mUpdateEmailButton.setOnClickListener {
            if(!mEmailOldInput.text.isNullOrEmpty() && !mEmailNewInput.text.isNullOrEmpty() && !mEmailPasswordInput.text.isNullOrEmpty()){
                this.updateUserEmail()
            } else { TingToast(
                activity!!,
                "Fill All Fields !!!",
                TingToastType.ERROR
            ).showToast(Toast.LENGTH_LONG) }
        }

        return view
    }

    private fun updateUserEmail(){
        val url = Routes.updateProfileEmail
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val sdf = SimpleDateFormat("dd-M-yyyy hh:mm:ss", Locale.getDefault())
        val currentDate = sdf.format(Date())

        val tz = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            TimeZone.getDefault().displayName
        } else { "UTC" }

        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("old_email", mEmailOldInput.text.toString())
            .addFormDataPart("new_email", mEmailNewInput.text.toString())
            .addFormDataPart("password", mEmailPasswordInput.text.toString())
            .addFormDataPart("os", "Android")
            .addFormDataPart("tz", tz)
            .addFormDataPart("time", currentDate)
            .build()

        val request = Request.Builder()
            .header("Authorization", user.token!!)
            .url(url)
            .post(requestBody)
            .build()

        mProgressOverlay.show(fragmentManager!!, mProgressOverlay.tag)

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                activity!!.runOnUiThread {
                    TingToast(
                        activity!!,
                        e.message!!,
                        TingToastType.ERROR
                    ).showToast(Toast.LENGTH_LONG)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body!!.string()
                val gson = Gson()
                try{
                    val serverResponse = gson.fromJson(responseBody, ServerResponse::class.java)
                    activity!!.runOnUiThread {
                        mProgressOverlay.dismiss()
                        if (serverResponse.status == 200){
                            userAuthentication.set(gson.toJson(serverResponse.user))
                            TingToast(
                                activity!!,
                                serverResponse.message,
                                TingToastType.SUCCESS
                            ).showToast(Toast.LENGTH_LONG)
                            dialog?.dismiss()
                        } else { ErrorMessage(
                            activity!!,
                            serverResponse.message
                        ).show() }
                    }
                } catch (e: Exception){
                    activity!!.runOnUiThread {
                        mProgressOverlay.dismiss()
                        TingToast(
                            activity!!,
                            "An Error Has Occurred",
                            TingToastType.ERROR
                        ).showToast(Toast.LENGTH_LONG)
                    }
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(WindowManager.LayoutParams.FILL_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
    }
}