package com.codepipes.ting.fragments.signup

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.location.Geocoder
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.codepipes.ting.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.codepipes.ting.customclasses.LockableViewPager
import com.codepipes.ting.dialogs.messages.*
import com.codepipes.ting.interfaces.SuccessDialogCloseListener
import com.codepipes.ting.models.ServerResponse
import okhttp3.*
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.Settings
import com.codepipes.ting.utils.UtilsFunctions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.livefront.bridge.Bridge
import java.io.IOException
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit


class SignUpIdentityFragment : Fragment() {

    private lateinit var mAppNameText: TextView
    private lateinit var mNextSignUpBtn: Button
    private lateinit var mSignUpNameInput: EditText
    private lateinit var mSignUpUsernameInput: EditText
    private lateinit var mSignUpEmailInput: EditText

    private lateinit var mViewPager: LockableViewPager
    private val mProgressOverlay: ProgressOverlay =
        ProgressOverlay()

    private lateinit var settings: Settings
    private lateinit var signUpUserData: MutableMap<String, String>
    private lateinit var gson: Gson

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mUtilFunctions: UtilsFunctions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Bridge.restoreInstanceState(this, savedInstanceState)
        savedInstanceState?.clear()
    }

    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sign_up_identity, container, false)

        mViewPager = activity!!.findViewById(R.id.pager) as LockableViewPager

        mAppNameText = view.findViewById<TextView>(R.id.appNameText) as TextView
        mNextSignUpBtn = view.findViewById<Button>(R.id.nextSignUpBtn) as Button

        mSignUpNameInput = view.findViewById<EditText>(R.id.signUpNameInput) as EditText
        mSignUpUsernameInput = view.findViewById<EditText>(R.id.signUpUsernameInput) as EditText
        mSignUpEmailInput = view.findViewById<EditText>(R.id.signUpEmailInput) as EditText

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity!!)
        mUtilFunctions = UtilsFunctions(activity!!)

        settings = Settings(activity!!)
        gson = Gson()

        val userDataString = settings.getSettingFromSharedPreferences("signup_data")

        signUpUserData = if(!userDataString.isNullOrEmpty()){
            gson.fromJson(userDataString, object : TypeToken<MutableMap<String, String>>() {}.type)
        } else {
            mutableMapOf()
        }

        if(signUpUserData.isNotEmpty() && !signUpUserData.isNullOrEmpty()){
            mSignUpNameInput.setText(signUpUserData["name"])
            mSignUpUsernameInput.setText(signUpUserData["username"])
            mSignUpEmailInput.setText(signUpUserData["email"])
        }

        val spanText = SpannableString("Ting.com")
        spanText.setSpan(ForegroundColorSpan(resources.getColor(R.color.colorPrimaryDark)), 0, 4, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
        spanText.setSpan(ForegroundColorSpan(resources.getColor(R.color.colorPrimary)), 4, spanText.length, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
        mAppNameText.text = spanText

        if(mUtilFunctions.checkLocationPermissions()){
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener {
                    val geocoder = Geocoder(activity, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                    activity!!.runOnUiThread {
                        signUpUserData["address"] = addresses[0].getAddressLine(0)
                        signUpUserData["latitude"] = it.latitude.toString()
                        signUpUserData["longitude"] = it.longitude.toString()
                        signUpUserData["country"] = addresses[0].countryName
                        signUpUserData["town"] = addresses[0].locality
                        settings.saveSettingToSharedPreferences("signup_data", gson.toJson(signUpUserData))
                    }
                }.addOnFailureListener {
                    activity!!.runOnUiThread {
                        TingToast(
                            context!!,
                            it.message!!,
                            TingToastType.ERROR
                        ).showToast(Toast.LENGTH_LONG)
                    }
                }
            } catch (e: Exception){
                TingToast(
                    context!!,
                    e.message!!,
                    TingToastType.ERROR
                ).showToast(Toast.LENGTH_LONG)
            }
        }

        mNextSignUpBtn.setOnClickListener {
            mProgressOverlay.show(fragmentManager!!, mProgressOverlay.tag)
            this.checkEmailUsername()
        }

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onDetach() {
        super.onDetach()
    }

    private fun checkEmailUsername(){
        val url = Routes.checkEmailUsername
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val form = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("name", mSignUpNameInput.text.toString())
            .addFormDataPart("email", mSignUpEmailInput.text.toString())
            .addFormDataPart("username", mSignUpUsernameInput.text.toString())
            .build()

        val request = Request.Builder()
            .url(url)
            .post(form)
            .build()

        client.newCall(request).enqueue(object : Callback{
            override fun onFailure(call: Call, e: IOException) {
                activity!!.runOnUiThread {
                    mProgressOverlay.dismiss()
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
                try {
                    val serverResponse = gson.fromJson(responseBody, ServerResponse::class.java)
                    activity!!.runOnUiThread {
                        mProgressOverlay.dismiss()
                        if (serverResponse.status != 200){
                            val successOverlay = SuccessOverlay()
                            val bundle = Bundle()
                            bundle.putString("message", "Fill All The Fields")
                            bundle.putString("type", "error")
                            successOverlay.arguments = bundle
                            successOverlay.show(fragmentManager!!, successOverlay.tag)
                            successOverlay.dismissListener(object :
                                SuccessDialogCloseListener {
                                override fun handleDialogClose(dialog: DialogInterface?) {
                                    successOverlay.dismiss()
                                }
                            })
                        } else {
                            signUpUserData["name"] = mSignUpNameInput.text.toString()
                            signUpUserData["username"] = mSignUpUsernameInput.text.toString()
                            signUpUserData["email"] = mSignUpEmailInput.text.toString()
                            settings.saveSettingToSharedPreferences("signup_data", gson.toJson(signUpUserData))
                            mProgressOverlay.dismiss()
                            mViewPager.currentItem = mViewPager.currentItem + 1
                        }
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Bridge.saveInstanceState(this, outState)
        outState.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        Bridge.clear(this)
    }
}
