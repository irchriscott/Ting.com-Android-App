package com.codepipes.ting.activities.base

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.graphics.PorterDuff
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.animation.Animation
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.codepipes.ting.R
import com.codepipes.ting.dialogs.messages.ErrorMessage
import com.codepipes.ting.dialogs.messages.ProgressOverlay
import com.codepipes.ting.dialogs.messages.SuccessOverlay
import com.codepipes.ting.interfaces.SuccessDialogCloseListener
import com.codepipes.ting.models.ServerResponse
import com.codepipes.ting.utils.Routes
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit

class ResetPassword : AppCompatActivity() {

    lateinit var mAppNameText: TextView
    lateinit var mAnimation: Animation

    lateinit var mResetPasswordEmailInput: EditText
    lateinit var mResetPasswordBtn: Button

    private val mProgressOverlay: ProgressOverlay =
        ProgressOverlay()

    @SuppressLint("PrivateResource")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.elevation = 0F
        supportActionBar!!.title = ""

        try {
            val upArrow = ContextCompat.getDrawable(this@ResetPassword,
                R.drawable.abc_ic_ab_back_material
            )
            upArrow!!.setColorFilter(ContextCompat.getColor(this@ResetPassword,
                R.color.colorPrimary
            ), PorterDuff.Mode.SRC_ATOP)
            supportActionBar!!.setHomeAsUpIndicator(upArrow)
        } catch (e: java.lang.Exception) {}

        mAppNameText = findViewById<TextView>(R.id.appNameText) as TextView

        val spanText = SpannableString("Ting.com")
        spanText.setSpan(ForegroundColorSpan(resources.getColor(R.color.colorPrimaryDark)), 0, 4, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
        spanText.setSpan(ForegroundColorSpan(resources.getColor(R.color.colorPrimary)), 4, spanText.length, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
        mAppNameText.text = spanText

        mResetPasswordEmailInput = findViewById<EditText>(R.id.resetPwdEmailInput) as EditText
        mResetPasswordBtn = findViewById<Button>(R.id.submitResetPasswordBtn) as Button

        mResetPasswordBtn.setOnClickListener {
            if(!mResetPasswordEmailInput.text.isNullOrEmpty()){
                mProgressOverlay.show(supportFragmentManager, mProgressOverlay.tag)
                this.submitResetPassword()
            } else { ErrorMessage(
                this@ResetPassword,
                "Enter Your Email Address"
            ).show() }
        }
    }

    private fun submitResetPassword(){

        val url = Routes.authResetPassword

        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60 * 5, TimeUnit.SECONDS)
            .build()

        val form = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("email", mResetPasswordEmailInput.text.toString())
            .build()

        val request = Request.Builder()
            .url(url)
            .post(form)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    mProgressOverlay.dismiss()
                    Toast.makeText(this@ResetPassword, e.message, Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body!!.string()
                val gson = Gson()
                try{
                    val serverResponse = gson.fromJson(responseBody, ServerResponse::class.java)
                    runOnUiThread {
                        mProgressOverlay.dismiss()
                        if (serverResponse.status == 200){
                            val successDialog = SuccessOverlay()
                            val args: Bundle = Bundle()
                            args.putString("message", serverResponse.message)
                            args.putString("type", serverResponse.type)
                            successDialog.arguments = args
                            successDialog.show(supportFragmentManager, successDialog.tag)

                            val onDialogClosed = object : SuccessDialogCloseListener {
                                override fun handleDialogClose(dialog: DialogInterface?) {
                                    startActivity(Intent(this@ResetPassword, SplashScreen::class.java))
                                }
                            }
                            successDialog.dismissListener(onDialogClosed)

                        } else { ErrorMessage(
                            this@ResetPassword,
                            serverResponse.message
                        ).show() }
                    }
                } catch (e: Exception){
                    runOnUiThread {
                        mProgressOverlay.dismiss()
                        ErrorMessage(
                            this@ResetPassword,
                            "An Error Has Occurred"
                        ).show()
                    }
                }
            }

        })
    }
}
