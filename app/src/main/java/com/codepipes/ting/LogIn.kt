package com.codepipes.ting

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.widget.*
import com.codepipes.ting.dialogs.*
import com.codepipes.ting.interfaces.SuccessDialogCloseListener
import com.codepipes.ting.models.ServerResponse
import com.codepipes.ting.models.User
import com.codepipes.ting.providers.LocalData
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.Settings
import com.codepipes.ting.utils.UtilData
import com.codepipes.ting.utils.UtilsFunctions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class LogIn : AppCompatActivity() {

    private lateinit var mAppNameText: TextView
    private lateinit var mNavigateSignUpBtn: Button
    private lateinit var mSignInWithGoogleButton: Button
    private lateinit var mNavigateResetPasswordBtn: Button

    private lateinit var mEmailLogInInput: EditText
    private lateinit var mPasswordLogInInput: EditText
    private lateinit var mSubmitLogInFormButton: Button

    lateinit var settings: Settings
    lateinit var mGoogleSignInClient: GoogleSignInClient

    private val RC_SIGN_IN = 1
    private lateinit var geocoder: Geocoder
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val mProgressOverlay: ProgressOverlay = ProgressOverlay()
    private val routes: Routes = Routes()

    private lateinit var userAuthentication: UserAuthentication
    private lateinit var mUtilFunctions: UtilsFunctions

    private lateinit var localData: LocalData
    private lateinit var googleSignInTask: Task<GoogleSignInAccount>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_in)

        mUtilFunctions = UtilsFunctions(this@LogIn)

        mAppNameText = findViewById<TextView>(R.id.appNameText) as TextView
        mNavigateSignUpBtn = findViewById<Button>(R.id.navigateSignUpBtn) as Button
        mSignInWithGoogleButton = findViewById<Button>(R.id.signInWithGoogleButton) as Button
        mNavigateResetPasswordBtn = findViewById<Button>(R.id.navigateResetPasswordBtn) as Button

        mEmailLogInInput = findViewById<EditText>(R.id.loginEmailInput) as EditText
        mPasswordLogInInput = findViewById<EditText>(R.id.loginPasswordInput) as EditText
        mSubmitLogInFormButton = findViewById<Button>(R.id.submitLoginButton) as Button

        if (!mUtilFunctions.isConnectedToInternet() && !mUtilFunctions.isConnected()) { TingToast(this@LogIn, "You are not connected to the internet", TingToastType.ERROR).showToast(Toast.LENGTH_LONG) }

        settings = Settings(this@LogIn)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this@LogIn, gso)
        geocoder = Geocoder(this@LogIn, Locale.getDefault())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this@LogIn)

        userAuthentication = UserAuthentication(this@LogIn)
        mUtilFunctions = UtilsFunctions(this@LogIn)

        localData = LocalData(this@LogIn)

        val spanText = SpannableString("Ting.com")
        spanText.setSpan(ForegroundColorSpan(resources.getColor(R.color.colorPrimaryDark)), 0, 4, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
        spanText.setSpan(ForegroundColorSpan(resources.getColor(R.color.colorPrimary)), 4, spanText.length, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
        mAppNameText.text = spanText

        mSubmitLogInFormButton.setOnClickListener {
            mProgressOverlay.show(fragmentManager, mProgressOverlay.tag)
            this.authenticateUser()
        }

        mSignInWithGoogleButton.setOnClickListener{
            mProgressOverlay.show(fragmentManager, mProgressOverlay.tag)
            if (mUtilFunctions.isConnectedToInternet() && mUtilFunctions.isConnected()) {
                mGoogleSignInClient.signOut()
                val signInIntent = mGoogleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            } else { TingToast(this@LogIn, "You are not connected to the internet", TingToastType.ERROR).showToast(Toast.LENGTH_LONG) }
        }

        mNavigateSignUpBtn.setOnClickListener {
            if(mUtilFunctions.isConnectedToInternet() && mUtilFunctions.isConnected()){ startActivity(Intent(this@LogIn, SignUp::class.java)) }
            else { TingToast(this@LogIn, "You are not connected to the internet", TingToastType.ERROR).showToast(Toast.LENGTH_LONG) }
        }

        mNavigateResetPasswordBtn.setOnClickListener {
            startActivity(Intent(this@LogIn, ResetPassword::class.java))
        }
    }

    private fun authenticateUser(){
        val url = this.routes.authLoginUser

        val client = OkHttpClient()

        val form = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("email", mEmailLogInInput.text.toString())
            .addFormDataPart("password", mPasswordLogInInput.text.toString())
            .build()

        val request = Request.Builder()
            .url(url)
            .post(form)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    if (mProgressOverlay.dialog.isShowing) { mProgressOverlay.dismiss() }
                    TingToast(this@LogIn, e.message!!, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body()!!.string()
                val gson = Gson()
                try{
                    val serverResponse = gson.fromJson(responseBody, ServerResponse::class.java)
                    runOnUiThread {
                        if (mProgressOverlay.dialog.isShowing) { mProgressOverlay.dismiss() }
                        if (serverResponse.status == 200){
                            val successDialog = SuccessOverlay()
                            val args: Bundle = Bundle()
                            args.putString("message", serverResponse.message)
                            args.putString("type", serverResponse.type)
                            successDialog.arguments = args
                            successDialog.show(this@LogIn.fragmentManager, successDialog.tag)

                            val onDialogClosed = object : SuccessDialogCloseListener {
                                override fun handleDialogClose(dialog: DialogInterface?) {
                                    if(serverResponse.user != null){
                                        userAuthentication.set(gson.toJson(serverResponse.user))
                                        localData.updateUser(serverResponse.user)
                                        startActivity(Intent(this@LogIn, TingDotCom::class.java))
                                    } else { ErrorMessage(this@LogIn, "Unable To Fetch User Data").show() }
                                }
                            }
                            successDialog.dismissListener(onDialogClosed)

                        } else { ErrorMessage(this@LogIn, serverResponse.message).show() }
                    }
                } catch (e: Exception){
                    runOnUiThread {
                        if (mProgressOverlay.dialog.isShowing) { mProgressOverlay.dismiss() }
                        TingToast(this@LogIn, "An Error Has Occurred", TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                    }
                }
            }

        })
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            this.googleSignInTask = task
            handleSignInResult(task)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            mUtilFunctions.REQUEST_FINE_LOCATION -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    handleSignInResult(this.googleSignInTask)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            if (mUtilFunctions.checkLocationPermissions()) {
                fusedLocationClient.lastLocation.addOnSuccessListener {
                    if(it != null){
                        val geocoder = Geocoder(this@LogIn, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)

                        if(!mProgressOverlay.dialog.isShowing) {  mProgressOverlay.show(fragmentManager, mProgressOverlay.tag) }
                        val idToken = mUtilFunctions.getToken(512)
                        val url = this.routes.submitGoogleSignUp

                        val client = OkHttpClient()

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
                                    if (mProgressOverlay.dialog.isShowing) { mProgressOverlay.dismiss() }
                                    TingToast(this@LogIn, e.message!!, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                                }
                            }

                            override fun onResponse(call: Call, response: Response) {
                                val responseBody = response.body()!!.string()
                                val gson = Gson()
                                try{
                                    val serverResponse = gson.fromJson(responseBody, ServerResponse::class.java)
                                    runOnUiThread {
                                        if (mProgressOverlay.dialog.isShowing) { mProgressOverlay.dismiss() }
                                        if (serverResponse.status == 200){
                                            val successDialog = SuccessOverlay()
                                            val args: Bundle = Bundle()
                                            args.putString("message", serverResponse.message)
                                            args.putString("type", serverResponse.type)
                                            successDialog.arguments = args
                                            successDialog.show(this@LogIn.fragmentManager, successDialog.tag)

                                            val onDialogClosed = object : SuccessDialogCloseListener {
                                                override fun handleDialogClose(dialog: DialogInterface?) {
                                                    if(serverResponse.user != null){
                                                        userAuthentication.set(gson.toJson(serverResponse.user))
                                                        localData.updateUser(serverResponse.user)
                                                        startActivity(Intent(this@LogIn, TingDotCom::class.java))
                                                    } else { ErrorMessage(this@LogIn, "Unable To Fetch User Data").show() }
                                                }
                                            }
                                            successDialog.dismissListener(onDialogClosed)

                                        } else { ErrorMessage(this@LogIn, serverResponse.message).show() }
                                    }
                                } catch (e: Exception){
                                    runOnUiThread {
                                        if (mProgressOverlay.dialog.isShowing) { mProgressOverlay.dismiss() }
                                        TingToast(this@LogIn, "An Error Has Occurred", TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                                    }
                                }
                            }

                        })
                    } else {
                        runOnUiThread {
                            if (mProgressOverlay.dialog.isShowing) { mProgressOverlay.dismiss() }
                            TingToast(this@LogIn, "An Error Has Occurred", TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                        }
                    }

                }.addOnFailureListener {
                    runOnUiThread {
                        TingToast(this@LogIn, it.message!!, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                    }
                }
            }
        } catch (e: ApiException) { TingToast(this@LogIn, "Google Sign In Failed", TingToastType.ERROR).showToast(Toast.LENGTH_LONG) }
    }
}
