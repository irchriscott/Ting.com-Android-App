package com.codepipes.ting.dialogs.user.add

import android.annotation.SuppressLint
import android.app.Dialog
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetDialogFragment
import android.support.design.widget.CoordinatorLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import com.caverock.androidsvg.SVG
import com.codepipes.ting.R
import com.codepipes.ting.dialogs.ErrorMessage
import com.codepipes.ting.dialogs.ProgressOverlay
import com.codepipes.ting.dialogs.TingToast
import com.codepipes.ting.dialogs.TingToastType
import com.codepipes.ting.interfaces.MapAddressChangedListener
import com.codepipes.ting.models.ServerResponse
import com.codepipes.ting.models.User
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.UtilData
import com.codepipes.ting.utils.UtilsFunctions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import kotlinx.android.synthetic.main.fragment_user_address_map.*
import kotlinx.android.synthetic.main.fragment_user_address_map.view.*
import okhttp3.*
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class AddUserAddress : BottomSheetDialogFragment(), OnMapReadyCallback{

    private lateinit var mUseLocationBtn: Button
    private lateinit var mSearchAddressInput: EditText
    private lateinit var mSearchAddressPlaceBtn: ImageButton

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val GOOGLE_MAPS_ZOOM = 16.0f

    private lateinit var geocoder: Geocoder
    private lateinit var mUtilFunctions: UtilsFunctions
    private lateinit var gson: Gson
    private lateinit var mMapChangedListener: MapAddressChangedListener

    private lateinit var user: User
    private lateinit var userAuthentication: UserAuthentication

    private lateinit var mapPin: BitmapDescriptor
    private var newAddress = mutableMapOf<String, String>()

    private val mProgressOverlay = ProgressOverlay()

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @SuppressLint("MissingPermission", "SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val mArgs = arguments
        val view = inflater.inflate(R.layout.fragment_user_address_map, container, false)

        val mapFragment = fragmentManager!!.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mapFragment.map.view!!.isClickable = true

        geocoder = Geocoder(activity, Locale.getDefault())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity!!)
        mUtilFunctions = UtilsFunctions(activity!!)

        userAuthentication = UserAuthentication(context!!)
        user = userAuthentication.get()!!

        mUseLocationBtn = view.findViewById<Button>(R.id.useLocationBtn) as Button
        mSearchAddressInput = view.findViewById<EditText>(R.id.searchAddressInput) as EditText
        mSearchAddressPlaceBtn = view.findViewById<ImageButton>(R.id.searchUserLocation) as ImageButton

        mUseLocationBtn.text = "ADD ADDRESS"

        if (mUtilFunctions.checkLocationPermissions()) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener {
                    val geocoder = Geocoder(activity, Locale.getDefault())
                    if(it != null) {
                        val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                        activity!!.runOnUiThread {
                            mSearchAddressInput.setText(addresses[0].getAddressLine(0))
                            newAddress["address"] = addresses[0].getAddressLine(0)
                            newAddress["longitude"] = it.longitude.toString()
                            newAddress["latitude"] = it.latitude.toString()
                            newAddress["type"] = UtilData().addressType[0]
                        }
                    } else {
                        activity!!.runOnUiThread {
                            TingToast(context!!, context?.resources!!.getString(R.string.error_internet), TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                        }
                    }
                }.addOnFailureListener {
                    activity!!.runOnUiThread {
                        TingToast(context!!, it.message!!, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                    }
                }
            } catch (e: Exception){
                TingToast(context!!, e.message!!, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
            }
        }

        mUseLocationBtn.setOnClickListener { this.addUserAddress() }

        val sharp = SVG.getFromString(user.pinImg)
        mapPin = mUtilFunctions.vectorToBitmap(sharp)

        return view
    }

    override fun setCancelable(cancelable: Boolean) {
        val dialog = dialog
        val touchOutsideView = dialog.window!!.decorView.findViewById<View>(android.support.design.R.id.touch_outside)
        val bottomSheetView =
            dialog.window!!.decorView.findViewById<View>(android.support.design.R.id.design_bottom_sheet)

        if (cancelable) {
            touchOutsideView.setOnClickListener {
                if (dialog.isShowing) { dialog.cancel() }
            }
            BottomSheetBehavior.from(bottomSheetView).setHideable(true)
        } else {
            touchOutsideView.setOnClickListener(null)
            BottomSheetBehavior.from(bottomSheetView).setHideable(false)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener {
            dialog.window.findViewById<View>(R.id.touch_outside).setOnClickListener(null)
            (dialog.window.findViewById<View>(R.id.design_bottom_sheet).layoutParams as CoordinatorLayout.LayoutParams).behavior = null
        }
        return dialog
    }

    @SuppressLint("MissingPermission", "StaticFieldLeak")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if(mUtilFunctions.checkLocationPermissions()){ mMap.isMyLocationEnabled = true }

        if(mUtilFunctions.checkLocationPermissions()){
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener {
                    if(it != null) {
                        mMap.clear()
                        this.getLocation(it)
                    } else { TingToast(context!!, activity!!.resources.getString(R.string.error_internet), TingToastType.ERROR).showToast(Toast.LENGTH_LONG) }
                }.addOnFailureListener {
                    activity!!.runOnUiThread {
                        TingToast(context!!, it.message!!, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                    }
                }
            } catch (e: java.lang.Exception){
                TingToast(context!!, activity!!.resources.getString(R.string.error_internet), TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
            }
        }

        mMap.setOnMyLocationButtonClickListener {
            if(mUtilFunctions.checkLocationPermissions()){
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener {
                        mMap.clear()
                        this.getLocation(it)
                    }.addOnFailureListener {
                        activity!!.runOnUiThread {
                            TingToast(context!!, it.message!!, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                        }
                    }
                } catch (e: Exception){
                    TingToast(context!!, activity!!.resources.getString(R.string.error_internet), TingToastType.ERROR).showToast(
                        Toast.LENGTH_LONG)
                }
            }
            true
        }

        mMap.setOnMyLocationClickListener {
            mMap.clear()
            this.getLocation(it)
        }

        mMap.setOnMapClickListener {
            mMap.clear()
            try {
                val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                mMap.addMarker(MarkerOptions().position(it).title(addresses[0].getAddressLine(0)).icon(mapPin))
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it, GOOGLE_MAPS_ZOOM))
                this.setLocationVariable(addresses[0].getAddressLine(0), it.latitude, it.longitude, addresses[0].countryName, addresses[0].locality)
            } catch(e: Exception){
                TingToast(context!!, activity!!.resources.getString(R.string.error_internet), TingToastType.ERROR).showToast(
                    Toast.LENGTH_LONG)
            }
        }
    }

    private fun getLocation(location: Location){
        try {
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            mMap.addMarker(MarkerOptions().position(LatLng(location.latitude, location.longitude)).title(addresses[0].getAddressLine(0)).icon(mapPin))
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), GOOGLE_MAPS_ZOOM))
            this.setLocationVariable(addresses[0].getAddressLine(0), location.latitude, location.longitude, addresses[0].countryName, addresses[0].locality)
        } catch (e: Exception){
            TingToast(context!!, activity!!.resources.getString(R.string.error_internet), TingToastType.ERROR).showToast(
                Toast.LENGTH_LONG)
        }
    }

    private fun setLocationVariable(address: String, latitude: Double, longitude: Double, country: String, town: String){
        activity!!.runOnUiThread {
            newAddress["address"] = address
            newAddress["longitude"] = longitude.toString()
            newAddress["latitude"] = latitude.toString()
            newAddress["type"] = UtilData().addressType[0]
            mSearchAddressInput.setText(address)
        }
    }

    private fun addUserAddress(){
        val url = Routes().addUserAddress
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val form = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("address", newAddress["address"]!!)
            .addFormDataPart("latitude", newAddress["latitude"].toString())
            .addFormDataPart("longitude", newAddress["longitude"].toString())
            .addFormDataPart("type", newAddress["type"]!!)
            .addFormDataPart("other_address_type", newAddress["type"]!!)
            .build()

        val request = Request.Builder()
            .header("Authorization", user.token!!)
            .url(url)
            .post(form)
            .build()

        mProgressOverlay.show(activity!!.fragmentManager, mProgressOverlay.tag)

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                activity!!.runOnUiThread {
                    TingToast(activity!!, e.message!!, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body()!!.string()
                val gson = Gson()
                try{
                    val serverResponse = gson.fromJson(responseBody, ServerResponse::class.java)
                    activity!!.runOnUiThread {
                        mProgressOverlay.dismiss()
                        if (serverResponse.status == 200){
                            dialog.dismiss()
                            userAuthentication.set(gson.toJson(serverResponse.user))
                            TingToast(activity!!, serverResponse.message, TingToastType.SUCCESS).showToast(
                                Toast.LENGTH_LONG)
                        } else { ErrorMessage(activity, serverResponse.message).show() }
                    }
                } catch (e: Exception){
                    activity!!.runOnUiThread {
                        mProgressOverlay.dismiss()
                        TingToast(activity!!, "An Error Has Occurred", TingToastType.ERROR).showToast(
                            Toast.LENGTH_LONG)
                    }
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val f = fragmentManager!!.findFragmentById(R.id.map)
        fragmentManager!!.beginTransaction().remove(f!!).commit()
    }

    fun dismissListener(closeListener: MapAddressChangedListener) {
        this.mMapChangedListener = closeListener
    }
}
