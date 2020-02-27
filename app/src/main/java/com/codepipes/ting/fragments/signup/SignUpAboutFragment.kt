package com.codepipes.ting.fragments.signup

import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.codepipes.ting.dialogs.messages.ProgressOverlay
import com.codepipes.ting.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.codepipes.ting.customclasses.LockableViewPager
import com.codepipes.ting.dialogs.messages.ErrorMessage
import com.codepipes.ting.dialogs.messages.TingToast
import com.codepipes.ting.dialogs.messages.TingToastType
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.Settings
import com.codepipes.ting.utils.UtilData
import com.codepipes.ting.utils.UtilsFunctions
import com.livefront.bridge.Bridge
import java.text.SimpleDateFormat
import java.util.*


class SignUpAboutFragment : Fragment() {

    lateinit var mAppNameText: TextView
    lateinit var mNextSignUpBtn: Button
    lateinit var mSignUpGenderInput: EditText
    lateinit var mSignUpDobInput: EditText

    lateinit var mViewPager: LockableViewPager
    private val mProgressOverlay: ProgressOverlay =
        ProgressOverlay()
    private val routes: Routes = Routes()
    private val utilData: UtilData = UtilData()

    private lateinit var settings: Settings
    private lateinit var signUpUserData: MutableMap<String, String>
    private lateinit var gson: Gson

    private lateinit var mUtilFunctions: UtilsFunctions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Bridge.restoreInstanceState(this, savedInstanceState)
        savedInstanceState?.clear()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sign_up_about, container, false)

        mViewPager = activity!!.findViewById(R.id.pager) as LockableViewPager
        mUtilFunctions = UtilsFunctions(activity!!)

        mAppNameText = view.findViewById<TextView>(R.id.appNameText) as TextView
        mNextSignUpBtn = view.findViewById<Button>(R.id.nextSignUpBtn) as Button

        mSignUpGenderInput = view.findViewById<EditText>(R.id.signUpGenderInput) as EditText
        mSignUpDobInput = view.findViewById<EditText>(R.id.signUpDobInput) as EditText

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

        val genders = utilData.genders
        if(!signUpUserData.isEmpty() && !signUpUserData.isNullOrEmpty()){
            if(!signUpUserData["dob"].isNullOrEmpty()){
                mSignUpGenderInput.setText(signUpUserData["gender"])
                mSignUpDobInput.setText(signUpUserData["dob"])
            } else { mSignUpGenderInput.setText(genders[0]) }
        } else { mSignUpGenderInput.setText(genders[0]) }

        mSignUpGenderInput.setOnClickListener {
            val alertDialogBuilder = AlertDialog.Builder(activity!!)
            alertDialogBuilder.setTitle("Select Gender")
            alertDialogBuilder.setItems(genders, DialogInterface.OnClickListener{ _, i ->
                val selectedText = genders[i]
                mSignUpGenderInput.setText(selectedText) })
            val alertDialog: AlertDialog = alertDialogBuilder.create()
            alertDialog.show()
        }

        mSignUpGenderInput.setOnKeyListener(null)

        val calendar = Calendar.getInstance()

        val date = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            val format = "yyyy-MM-dd"
            val sdf = SimpleDateFormat(format, Locale.US)
            mSignUpDobInput.setText(sdf.format(calendar.time))
        }

        mSignUpDobInput.setOnClickListener {
            DatePickerDialog(activity!!,
                R.style.DatePickerAppTheme, date, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        mSignUpDobInput.setOnKeyListener(null)

        mNextSignUpBtn.setOnClickListener {
            if(mUtilFunctions.isConnectedToInternet() && mUtilFunctions.isConnected()) {
                if (mSignUpGenderInput.text.toString() != "" && mSignUpDobInput.text.toString() != "") {
                    signUpUserData["gender"] = mSignUpGenderInput.text.toString()
                    signUpUserData["dob"] = mSignUpDobInput.text.toString()
                    settings.saveSettingToSharedPreferences("signup_data", gson.toJson(signUpUserData))
                    mViewPager.currentItem = mViewPager.currentItem + 1
                } else {
                    ErrorMessage(
                        activity,
                        "Fill All The Fields"
                    ).show()
                }
            } else { TingToast(
                context!!,
                "You are not connected to the internet",
                TingToastType.ERROR
            ).showToast(Toast.LENGTH_LONG) }
        }

        return view
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onDetach() {
        super.onDetach()
    }
}
