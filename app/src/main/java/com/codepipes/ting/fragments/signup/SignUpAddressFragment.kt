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
import com.codepipes.ting.customclasses.LockableViewPager
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.UtilData
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.codepipes.ting.R
import com.codepipes.ting.dialogs.messages.*
import com.codepipes.ting.interfaces.MapAddressChangedListener
import com.codepipes.ting.interfaces.SuccessDialogCloseListener
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.codepipes.ting.utils.Settings
import com.codepipes.ting.utils.UtilsFunctions
import com.livefront.bridge.Bridge
import java.util.*


class SignUpAddressFragment : Fragment() {

    lateinit var mAppNameText: TextView
    lateinit var mNextSignUpBtn: Button
    lateinit var mSignUpAddressInput: EditText
    lateinit var mSignUpAddressTypeInput: EditText

    lateinit var mViewPager: LockableViewPager
    private val mProgressOverlay: ProgressOverlay =
        ProgressOverlay()
    private val routes: Routes = Routes()
    private val utilData: UtilData = UtilData()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val REQUEST_FINE_LOCATION = 1

    private lateinit var mUtilFunctions: UtilsFunctions

    private lateinit var settings: Settings
    private lateinit var signUpUserData: MutableMap<String, String>
    private lateinit var gson: Gson

    private var selectedAddressType: String = ""
    private var inputOtherAddressType: String = ""

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
        val view = inflater.inflate(R.layout.fragment_sign_up_address, container, false)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity!!)
        mViewPager = activity!!.findViewById(R.id.pager) as LockableViewPager

        mAppNameText = view.findViewById<TextView>(R.id.appNameText) as TextView
        mNextSignUpBtn = view.findViewById<Button>(R.id.nextSignUpBtn) as Button

        mSignUpAddressInput = view.findViewById<EditText>(R.id.signUpAddressInput) as EditText
        mSignUpAddressTypeInput = view.findViewById<EditText>(R.id.signUpAddressTypeInput) as EditText

        mUtilFunctions = UtilsFunctions(activity!!)

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

        val types = utilData.addressType
        mSignUpAddressTypeInput.setText(types[0])
        this.selectedAddressType = types[0]

        mSignUpAddressTypeInput.setOnClickListener {
            val alertDialogBuilder = AlertDialog.Builder(activity!!)
            alertDialogBuilder.setTitle(activity!!.resources.getString(R.string.signup_address_type))
            alertDialogBuilder.setItems(types, DialogInterface.OnClickListener { _, which ->
                val selectedItem = types[which]
                mSignUpAddressTypeInput.setText(selectedItem)
                this.selectedAddressType = selectedItem
                if(which == types.size - 1){
                    val otherAddressInputLayout = LayoutInflater.from(activity).inflate(R.layout.layout_other_address_type, null)
                    val otherAddressAlertDialog = AlertDialog.Builder(activity!!).create()
                    otherAddressAlertDialog.setTitle(activity!!.resources.getString(R.string.signup_other_address_type))
                    otherAddressAlertDialog.setCancelable(false)

                    val otherAddressType = otherAddressInputLayout.findViewById<EditText>(R.id.otherAddressTypeInput) as EditText

                    otherAddressAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", DialogInterface.OnClickListener { _, _ ->
                        if(otherAddressType.text.toString().isNotEmpty()){
                            mSignUpAddressTypeInput.setText(otherAddressType.text.toString())
                            this.inputOtherAddressType = otherAddressType.text.toString()
                            otherAddressAlertDialog.dismiss()
                        }
                    })

                    otherAddressAlertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "CANCEL", DialogInterface.OnClickListener { _, _ ->
                        mSignUpAddressTypeInput.setText(types[0])
                        otherAddressAlertDialog.dismiss()
                    })
                    otherAddressAlertDialog.setView(otherAddressInputLayout)
                    otherAddressAlertDialog.show()
                }
            })
            val alertDialog: AlertDialog = alertDialogBuilder.create()
            alertDialog.show()
        }

        mSignUpAddressTypeInput.setOnKeyListener(null)

        if(signUpUserData["address"].isNullOrEmpty() && signUpUserData["latitude"].isNullOrEmpty() && signUpUserData["longitude"].isNullOrEmpty()){

            mProgressOverlay.show(fragmentManager!!, mProgressOverlay.tag)

            if(mUtilFunctions.checkLocationPermissions()){
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener {
                        val geocoder = Geocoder(activity, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                        activity!!.runOnUiThread {
                            mSignUpAddressInput.setText(addresses[0].getAddressLine(0))
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
        } else { mSignUpAddressInput.setText(signUpUserData["address"]) }

        mSignUpAddressInput.setOnClickListener {
            val mapDialog = UserAddressMapFragment()
            mapDialog.show(activity!!.supportFragmentManager, mapDialog.tag)
            val onMapDialogDismiss = object : MapAddressChangedListener{
                override fun handleMapAddressChanged(dialog: DialogInterface?) {
                    val userString = settings.getSettingFromSharedPreferences("signup_data")
                    if(!userString.isNullOrEmpty()){
                        val user = gson.fromJson<MutableMap<String, String>>(userString, object : TypeToken<MutableMap<String, String>>() {}.type)
                        mSignUpAddressInput.setText(user["address"])
                    }
                }
            }
            mapDialog.dismissListener(onMapDialogDismiss)
        }

        mNextSignUpBtn.setOnClickListener {
            if (mUtilFunctions.isConnectedToInternet()) {
                if (!mSignUpAddressInput.text.isNullOrEmpty() && !mSignUpAddressTypeInput.text.isNullOrEmpty()) {
                    signUpUserData["address_type"] = this.selectedAddressType
                    signUpUserData["address_type_other"] = this.inputOtherAddressType
                    settings.saveSettingToSharedPreferences("signup_data", gson.toJson(signUpUserData))
                    mViewPager.currentItem = mViewPager.currentItem + 1
                } else {
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
