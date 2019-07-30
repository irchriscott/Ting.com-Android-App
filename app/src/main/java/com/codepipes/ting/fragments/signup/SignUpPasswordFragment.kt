package com.codepipes.ting.fragments.signup

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.codepipes.ting.dialogs.ProgressOverlay
import com.codepipes.ting.R
import com.codepipes.ting.TingDotCom
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.codepipes.ting.customclasses.LockableViewPager
import com.codepipes.ting.dialogs.ErrorMessage
import com.codepipes.ting.dialogs.SuccessOverlay
import com.codepipes.ting.interfaces.SuccessDialogCloseListener
import com.codepipes.ting.models.ServerResponse
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.Settings
import okhttp3.*
import java.io.IOException
import java.lang.Exception
import java.util.concurrent.TimeUnit


class SignUpPasswordFragment : Fragment() {

    lateinit var mAppNameText: TextView
    lateinit var mFinishSignUpBtn: Button
    lateinit var mSignUpPasswordInput: EditText
    lateinit var mSignUpConfirmPasswordInput: EditText

    lateinit var mViewPager: LockableViewPager
    private val mProgressOverlay: ProgressOverlay = ProgressOverlay()
    private val routes: Routes = Routes()

    private lateinit var settings: Settings
    private lateinit var signUpUserData: MutableMap<String, String>
    private lateinit var gson: Gson

    private lateinit var userAuthentication: UserAuthentication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_sign_up_password, container, false)

        mAppNameText = view.findViewById<TextView>(R.id.appNameText) as TextView
        mFinishSignUpBtn = view.findViewById<Button>(R.id.finishSignUpBtn) as Button

        mSignUpPasswordInput = view.findViewById<EditText>(R.id.signUpPasswordInput) as EditText
        mSignUpConfirmPasswordInput = view.findViewById<EditText>(R.id.signUpConfirmPasswordInput) as EditText

        userAuthentication = UserAuthentication(activity!!)

        settings = Settings(activity!!)
        gson = Gson()

        val userDataString = settings.getSettingFromSharedPreferences("signup_data")

        signUpUserData = if(!userDataString.isNullOrEmpty()){
            gson.fromJson(userDataString, object : TypeToken<MutableMap<String, String>>() {}.type)
        } else {
            mutableMapOf()
        }

        val spanText = SpannableString("Ting.com")
        spanText.setSpan(ForegroundColorSpan(resources.getColor(R.color.colorPrimaryDark)), 0, 4, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
        spanText.setSpan(ForegroundColorSpan(resources.getColor(R.color.colorPrimary)), 4, spanText.length, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
        mAppNameText.text = spanText

        mFinishSignUpBtn.setOnClickListener {
            if(!mSignUpPasswordInput.text.isNullOrEmpty() && !mSignUpConfirmPasswordInput.text.isNullOrEmpty()){
                if(mSignUpPasswordInput.text.toString() == mSignUpConfirmPasswordInput.text.toString()){
                    mProgressOverlay.show(activity!!.fragmentManager, mProgressOverlay.tag)
                    this.submitSignUp()
                } else { ErrorMessage(activity, "Passwords Didn't Match").show() }
            } else { ErrorMessage(activity, "Passwords Cannot Be Empty").show() }
        }

        return view
    }

    private fun submitSignUp(){
        val url = this.routes.submitEmailSignUp
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val form = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("name", signUpUserData["name"]!!)
            .addFormDataPart("email", signUpUserData["email"]!!)
            .addFormDataPart("username", signUpUserData["username"]!!)
            .addFormDataPart("country", signUpUserData["country"]!!)
            .addFormDataPart("town", signUpUserData["town"]!!)
            .addFormDataPart("gender", signUpUserData["gender"]!!)
            .addFormDataPart("date_of_birth", signUpUserData["dob"]!!)
            .addFormDataPart("address", signUpUserData["address"]!!)
            .addFormDataPart("longitude", signUpUserData["longitude"]!!)
            .addFormDataPart("latitude", signUpUserData["latitude"]!!)
            .addFormDataPart("type",signUpUserData["address_type"]!!)
            .addFormDataPart("other_address_type", signUpUserData["address_type_other"]!!)
            .addFormDataPart("password", mSignUpPasswordInput.text.toString())
            .build()

        val request = Request.Builder()
            .url(url)
            .post(form)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity!!.runOnUiThread {
                    mProgressOverlay.dismiss()
                    Toast.makeText(activity, e.message, Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body()!!.string()
                val gson = Gson()
                try {
                    val serverResponse = gson.fromJson(responseBody, ServerResponse::class.java)
                    activity!!.runOnUiThread {
                        mProgressOverlay.dismiss()
                        if (serverResponse.status == 200){
                            val successDialog = SuccessOverlay()
                            val args: Bundle = Bundle()
                            args.putString("message", serverResponse.message)
                            args.putString("type", serverResponse.type)
                            successDialog.arguments = args
                            successDialog.show(activity!!.fragmentManager, successDialog.tag)

                            val onDialogClosed = object : SuccessDialogCloseListener {
                                override fun handleDialogClose(dialog: DialogInterface?) {
                                    if(serverResponse.msgs?.size!! > 0){
                                        userAuthentication.set(serverResponse.msgs[0].toString())
                                        settings.removeSettingFromSharedPreferences("signup_data")
                                        startActivity(Intent(activity, TingDotCom::class.java))
                                    } else { ErrorMessage(activity, "Unable To Fetch User Data").show() }
                                }
                            }
                            successDialog.dismissListener(onDialogClosed)

                        } else { ErrorMessage(activity, serverResponse.message).show() }
                    }
                } catch(e: Exception){
                    activity!!.runOnUiThread {
                        mProgressOverlay.dismiss()
                        ErrorMessage(activity!!, "An Error Has Occurred").show()
                    }
                }
            }

        })
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onDetach() {
        super.onDetach()
    }
}
