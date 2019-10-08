package com.codepipes.ting.fragments.signup


import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetDialogFragment
import android.support.design.widget.CoordinatorLayout
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.codepipes.ting.R
import com.codepipes.ting.dialogs.TingToast
import com.codepipes.ting.dialogs.TingToastType
import com.codepipes.ting.interfaces.MapAddressChangedListener
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.fragment_user_address_map.*
import kotlinx.android.synthetic.main.fragment_user_address_map.view.*
import com.codepipes.ting.utils.Settings
import com.codepipes.ting.utils.UtilsFunctions
import com.livefront.bridge.Bridge
import java.lang.Exception
import java.util.*


@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class UserAddressMapFragment : BottomSheetDialogFragment(), OnMapReadyCallback {

    private lateinit var mUseLocationBtn: Button
    private lateinit var mSearchAddressInput: EditText

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val REQUEST_FINE_LOCATION = 1
    private val GOOGLE_MAPS_ZOOM = 16.0f

    private lateinit var geocoder: Geocoder
    private lateinit var mUtilFunctions: UtilsFunctions

    private lateinit var settings: Settings
    private lateinit var signUpUserData: MutableMap<String, String>
    private lateinit var gson: Gson

    private lateinit var mMapChangedListener: MapAddressChangedListener

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

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

        val view = inflater.inflate(R.layout.fragment_user_address_map, container, false)

        val mapFragment = fragmentManager!!.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mapFragment.map.view!!.isClickable = true

        geocoder = Geocoder(activity, Locale.getDefault())
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

        mUseLocationBtn = view.findViewById<Button>(R.id.useLocationBtn) as Button
        mSearchAddressInput = view.findViewById<EditText>(R.id.searchAddressInput) as EditText

        if(signUpUserData["address"].isNullOrEmpty() && signUpUserData["latitude"].isNullOrEmpty() && signUpUserData["longitude"].isNullOrEmpty()) {

            if (mUtilFunctions.checkLocationPermissions()) {
                fusedLocationClient.lastLocation.addOnSuccessListener {
                    val geocoder = Geocoder(activity, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                    activity!!.runOnUiThread {
                        mSearchAddressInput.setText(addresses[0].getAddressLine(0))
                        signUpUserData["address"] = addresses[0].getAddressLine(0)
                        signUpUserData["latitude"] = it.latitude.toString()
                        signUpUserData["longitude"] = it.longitude.toString()
                        signUpUserData["country"] = addresses[0].countryName
                        signUpUserData["town"] = addresses[0].locality
                        settings.saveSettingToSharedPreferences("signup_data", gson.toJson(signUpUserData))
                    }
                }.addOnFailureListener {
                    activity!!.runOnUiThread {
                        TingToast(context!!, it.message!!, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                    }
                }
            }
        } else { mSearchAddressInput.setText(signUpUserData["address"]) }

        mUseLocationBtn.setOnClickListener {
            dialog.dismiss()
        }

        return view
    }

    override fun setCancelable(cancelable: Boolean) {
        val dialog = dialog
        val touchOutsideView = dialog.window!!.decorView.findViewById<View>(android.support.design.R.id.touch_outside)
        val bottomSheetView =
            dialog.window!!.decorView.findViewById<View>(android.support.design.R.id.design_bottom_sheet)

        if (cancelable) {
            touchOutsideView.setOnClickListener {
                if (dialog.isShowing) {
                    dialog.cancel()
                }
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

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if(mUtilFunctions.checkLocationPermissions()){ mMap.isMyLocationEnabled = true }

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
                    TingToast(context!!, activity!!.resources.getString(R.string.error_internet), TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                }
            }
            true
        }

        mMap.setOnMyLocationClickListener {
            mMap.clear()
            this.getLocation(it)
        }

        if(mUtilFunctions.checkLocationPermissions()){
            try {
                if(signUpUserData["address"].isNullOrEmpty() && signUpUserData["latitude"].isNullOrEmpty() && signUpUserData["longitude"].isNullOrEmpty()){
                    fusedLocationClient.lastLocation.addOnSuccessListener {
                        mMap.clear()
                        this.getLocation(it)
                    }.addOnFailureListener {
                        activity!!.runOnUiThread {
                            TingToast(context!!, it.message!!, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                        }
                    }
                } else {
                    val addresses = geocoder.getFromLocation(signUpUserData["latitude"]!!.toDouble(), signUpUserData["longitude"]!!.toDouble(), 1)
                    mMap.addMarker(MarkerOptions().position(LatLng(signUpUserData["latitude"]!!.toDouble(), signUpUserData["longitude"]!!.toDouble())).title(addresses[0].getAddressLine(0)))
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(signUpUserData["latitude"]!!.toDouble(), signUpUserData["longitude"]!!.toDouble()), GOOGLE_MAPS_ZOOM))
                }
            } catch (e: Exception){
                TingToast(context!!, e.message!!, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
            }
        }

        mMap.setOnMapClickListener {
            mMap.clear()
            try {
                val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                mMap.addMarker(MarkerOptions().position(it).title(addresses[0].getAddressLine(0)))
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it, GOOGLE_MAPS_ZOOM))
                this.setLocationVariable(addresses[0].getAddressLine(0), it.latitude, it.longitude, addresses[0].countryName, addresses[0].locality)
            } catch (e: Exception){
                TingToast(context!!, activity!!.resources.getString(R.string.error_internet), TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
            }
        }
    }

    private fun getLocation(location: Location){
        try {
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            mMap.addMarker(MarkerOptions().position(LatLng(location.latitude, location.longitude)).title(addresses[0].getAddressLine(0)))
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), GOOGLE_MAPS_ZOOM))
            this.setLocationVariable(addresses[0].getAddressLine(0), location.latitude, location.longitude, addresses[0].countryName, addresses[0].locality)
        } catch (e: Exception){
            TingToast(context!!, activity!!.resources.getString(R.string.error_internet), TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
        }
    }

    private fun setLocationVariable(address: String, latitude: Double, longitude: Double, country: String, town: String){
        activity!!.runOnUiThread {
            mSearchAddressInput.setText(address)
            signUpUserData["address"] = address
            signUpUserData["latitude"] = latitude.toString()
            signUpUserData["longitude"] = longitude.toString()
            signUpUserData["country"] = country
            signUpUserData["town"] = town
            settings.saveSettingToSharedPreferences("signup_data", gson.toJson(signUpUserData))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val f = fragmentManager!!.findFragmentById(R.id.map)
        fragmentManager!!.beginTransaction().remove(f!!).commit()
    }

    fun dismissListener(closeListener: MapAddressChangedListener) {
        this.mMapChangedListener = closeListener
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

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        mMapChangedListener.handleMapAddressChanged(null)
    }
}
