package com.codepipes.ting

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.constraint.ConstraintLayout
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import com.codepipes.ting.dialogs.ErrorMessage
import com.codepipes.ting.dialogs.ProgressOverlay
import com.codepipes.ting.dialogs.SuccessOverlay
import com.codepipes.ting.interfaces.SuccessDialogCloseListener
import com.codepipes.ting.models.ServerResponse
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.Settings
import com.codepipes.ting.utils.UtilData
import com.codepipes.ting.utils.UtilsFunctions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit


class SplashScreen : AppCompatActivity() {

    lateinit var mAppNameText: TextView
    lateinit var mAnimation: Animation
    lateinit var mLoginForm: ConstraintLayout
    lateinit var mSignUpBtnLayout: RelativeLayout
    lateinit var mNavigateSignUpBtn: Button
    lateinit var mSignInWithGoogleButton: Button
    lateinit var mNavigateResetPasswordBtn: Button

    private lateinit var mEmailLoginInput: EditText
    private lateinit var mPasswordLoginInput: EditText
    private lateinit var mSubmitLoginFormButton: Button

    lateinit var settings: Settings
    lateinit var mGoogleSignInClient: GoogleSignInClient

    private val RC_SIGN_IN = 1
    private lateinit var geocoder: Geocoder
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val mProgressOverlay: ProgressOverlay = ProgressOverlay()
    private val routes: Routes = Routes()

    private lateinit var userAuthentication: UserAuthentication
    private lateinit var mUtilFunctions: UtilsFunctions

    private var handler: Handler? = null
    private val runnable: Runnable = Runnable {
        if(userAuthentication.isLoggedIn()){
            startActivity(Intent(this@SplashScreen, TingDotCom::class.java))
        } else {
            mLoginForm.visibility = View.VISIBLE
            mSignUpBtnLayout.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        mAppNameText = findViewById<TextView>(R.id.appNameText) as TextView
        mLoginForm = findViewById<ConstraintLayout>(R.id.loginForm) as ConstraintLayout
        mSignUpBtnLayout = findViewById<RelativeLayout>(R.id.signUpBtnLayout) as RelativeLayout
        mNavigateSignUpBtn = findViewById<Button>(R.id.navigateSignUpBtn) as Button
        mSignInWithGoogleButton = findViewById<Button>(R.id.signInWithGoogleButton) as Button
        mNavigateResetPasswordBtn = findViewById<Button>(R.id.navigateResetPasswordBtn) as Button

        mEmailLoginInput = findViewById<EditText>(R.id.loginEmailInput) as EditText
        mPasswordLoginInput = findViewById<EditText>(R.id.loginPasswordInput) as EditText
        mSubmitLoginFormButton = findViewById<Button>(R.id.submitLoginButton) as Button

        settings = Settings(this@SplashScreen)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this@SplashScreen, gso)
        geocoder = Geocoder(this@SplashScreen, Locale.getDefault())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this@SplashScreen)

        userAuthentication = UserAuthentication(this@SplashScreen)
        mUtilFunctions = UtilsFunctions(this@SplashScreen)

        val spanText = SpannableString("Ting.com")
        spanText.setSpan(ForegroundColorSpan(resources.getColor(R.color.colorPrimaryDark)), 0, 4, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
        spanText.setSpan(ForegroundColorSpan(resources.getColor(R.color.colorPrimary)), 4, spanText.length, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
        mAppNameText.text = spanText

        mAnimation = AnimationUtils.loadAnimation(this@SplashScreen, R.anim.fade_in)
        mAppNameText.startAnimation(mAnimation)

        handler = Handler()
        handler?.postDelayed(runnable, 3000)

        mSubmitLoginFormButton.setOnClickListener {
            mProgressOverlay.show(fragmentManager, mProgressOverlay.tag)
            this.authenticateUser()
        }

        mSignInWithGoogleButton.setOnClickListener{
            mGoogleSignInClient.signOut()
            val signInIntent = mGoogleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

        mNavigateSignUpBtn.setOnClickListener {
            startActivity(Intent(this@SplashScreen, SignUp::class.java))
        }

        mNavigateResetPasswordBtn.setOnClickListener {
            startActivity(Intent(this@SplashScreen, ResetPassword::class.java))
        }
    }

    private fun authenticateUser(){
        val url = this.routes.authLoginUser

        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val form = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("email", mEmailLoginInput.text.toString())
            .addFormDataPart("password", mPasswordLoginInput.text.toString())
            .build()

        val request = Request.Builder()
            .url(url)
            .post(form)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    mProgressOverlay.dismiss()
                    Toast.makeText(this@SplashScreen, e.message, Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body()!!.string()
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
                            successDialog.show(this@SplashScreen.fragmentManager, successDialog.tag)

                            val onDialogClosed = object : SuccessDialogCloseListener{
                                override fun handleDialogClose(dialog: DialogInterface?) {
                                    if(serverResponse.msgs?.size!! > 0){
                                        userAuthentication.set(serverResponse.msgs[0].toString())
                                        startActivity(Intent(this@SplashScreen, TingDotCom::class.java))
                                    } else { ErrorMessage(this@SplashScreen, "Unable To Fetch User Data").show() }
                                }
                            }
                            successDialog.dismissListener(onDialogClosed)

                        } else { ErrorMessage(this@SplashScreen, serverResponse.message).show() }
                    }
                } catch (e: Exception){
                    runOnUiThread {
                        mProgressOverlay.dismiss()
                        ErrorMessage(this@SplashScreen, "An Error Has Occurred").show()
                    }
                }
            }

        })
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            if (mUtilFunctions.checkLocationPermissions()) {
                fusedLocationClient.lastLocation.addOnSuccessListener {
                    val geocoder = Geocoder(this@SplashScreen, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)

                    mProgressOverlay.show(fragmentManager, mProgressOverlay.tag)
                    val idToken = mUtilFunctions.getToken(512)
                    val url = this.routes.submitGoogleSignUp

                    val client = OkHttpClient.Builder()
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .writeTimeout(60, TimeUnit.SECONDS)
                        .build()

                    val form = MultipartBody.Builder().setType(MultipartBody.FORM)
                        .addFormDataPart("name", account?.displayName!!)
                        .addFormDataPart("email", account.email!!)
                        .addFormDataPart("country", addresses[0].countryName)
                        .addFormDataPart("town", addresses[0].locality)
                        .addFormDataPart("token", "${account.id}-$idToken")
                        .addFormDataPart("address", addresses[0].getAddressLine(0))
                        .addFormDataPart("longitude", it.longitude.toString())
                        .addFormDataPart("latitude", it.latitude.toString())
                        .addFormDataPart("type", UtilData().addressType[0])
                        .build()

                    val request = Request.Builder()
                        .url(url)
                        .post(form)
                        .build()

                    client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            runOnUiThread {
                                mProgressOverlay.dismiss()
                                Toast.makeText(this@SplashScreen, e.message, Toast.LENGTH_LONG).show()
                            }
                        }

                        override fun onResponse(call: Call, response: Response) {
                            val responseBody = response.body()!!.string()
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
                                        successDialog.show(this@SplashScreen.fragmentManager, successDialog.tag)

                                        val onDialogClosed = object : SuccessDialogCloseListener{
                                            override fun handleDialogClose(dialog: DialogInterface?) {
                                                if(serverResponse.msgs?.size!! > 0){
                                                    userAuthentication.set(serverResponse.msgs[0].toString())
                                                    startActivity(Intent(this@SplashScreen, TingDotCom::class.java))
                                                } else { ErrorMessage(this@SplashScreen, "Unable To Fetch User Data").show() }
                                            }
                                        }
                                        successDialog.dismissListener(onDialogClosed)

                                    } else { ErrorMessage(this@SplashScreen, serverResponse.message).show() }
                                }
                            } catch (e: Exception){
                                runOnUiThread {
                                    mProgressOverlay.dismiss()
                                    ErrorMessage(this@SplashScreen, "An Error Has Occurred").show()
                                }
                            }
                        }

                    })

                }.addOnFailureListener {
                    runOnUiThread {
                        Toast.makeText(this@SplashScreen, it.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: ApiException) { Toast.makeText(this@SplashScreen, "Google Sign In Failed", Toast.LENGTH_LONG).show() }

    }

    override fun onRestart() {
        super.onRestart()
        mLoginForm.visibility = View.VISIBLE
        mSignUpBtnLayout.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        settings.removeSettingFromSharedPreferences("splash")
        handler?.removeCallbacks(runnable)
    }

    override fun onStop() {
        super.onStop()
        settings.removeSettingFromSharedPreferences("splash")
    }
}
