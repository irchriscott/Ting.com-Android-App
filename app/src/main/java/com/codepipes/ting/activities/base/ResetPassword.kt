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
import com.codepipes.ting.R.drawable.abc_ic_ab_back_material
import com.codepipes.ting.dialogs.messages.*
import com.codepipes.ting.interfaces.SuccessDialogCloseListener
import com.codepipes.ting.models.ServerResponse
import com.codepipes.ting.utils.Routes
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class ResetPassword : AppCompatActivity() {

    private lateinit var mAppNameText: TextView
    lateinit var mAnimation: Animation

    private lateinit var mResetPasswordEmailInput: EditText
    private lateinit var mResetPasswordBtn: Button

    private lateinit var mProgressOverlay: ProgressOverlay

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
                abc_ic_ab_back_material
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

        mProgressOverlay = ProgressOverlay()

        mResetPasswordEmailInput = findViewById<EditText>(R.id.resetPwdEmailInput) as EditText
        mResetPasswordBtn = findViewById<Button>(R.id.submitResetPasswordBtn) as Button

        mResetPasswordBtn.setOnClickListener {
            if(!mResetPasswordEmailInput.text.isNullOrEmpty()){
                mProgressOverlay.show(supportFragmentManager, mProgressOverlay.tag)
                this.submitResetPassword()
            } else {
                val successOverlay = SuccessOverlay()
                val bundle = Bundle()
                bundle.putString("message", "Enter Your Email Address")
                bundle.putString("type", "error")
                successOverlay.arguments = bundle
                successOverlay.show(supportFragmentManager, successOverlay.tag)
                successOverlay.dismissListener(object :
                    SuccessDialogCloseListener {
                    override fun handleDialogClose(dialog: DialogInterface?) {
                        successOverlay.dismiss()
                    }
                })
            }
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
                    TingToast(this@ResetPassword, e.localizedMessage, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
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

                        } else { 
							val successOverlay = SuccessOverlay()
							val bundle = Bundle()
							bundle.putString("message", "Enter Your Email Address")
							bundle.putString("type", "error")
							successOverlay.arguments = bundle
							successOverlay.show(supportFragmentManager, successOverlay.tag)
							successOverlay.dismissListener(object :
								SuccessDialogCloseListener {
								override fun handleDialogClose(dialog: DialogInterface?) {
									successOverlay.dismiss()
								}
							})
						}
                    }
                } catch (e: Exception){
                    runOnUiThread {
                        mProgressOverlay.dismiss()
                        val successOverlay = SuccessOverlay()
						val bundle = Bundle()
						bundle.putString("message", "Enter Your Email Address")
						bundle.putString("type", "error")
						successOverlay.arguments = bundle
						successOverlay.show(supportFragmentManager, successOverlay.tag)
						successOverlay.dismissListener(object :
							SuccessDialogCloseListener {
							override fun handleDialogClose(dialog: DialogInterface?) {
								successOverlay.dismiss()
							}
						})
                    }
                }
            }

        })
    }
}
