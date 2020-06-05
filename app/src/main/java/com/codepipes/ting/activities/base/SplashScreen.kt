package com.codepipes.ting.activities.base

import android.content.Intent
import android.location.Geocoder
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import com.codepipes.ting.R
import com.codepipes.ting.providers.LocalData
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.utils.UtilsFunctions
import com.google.android.gms.location.*
import com.livefront.bridge.Bridge
import java.lang.Exception
import java.util.*


class SplashScreen : AppCompatActivity() {

    lateinit var mAppNameText: TextView
    lateinit var mAnimation: Animation

    lateinit var userAuthentication: UserAuthentication

    private var handler: Handler? = null

    private lateinit var localData: LocalData
    private lateinit var utilsFunctions: UtilsFunctions

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private val runnable: Runnable = Runnable {
        if(userAuthentication.isLoggedIn()){
            startActivity(Intent(this@SplashScreen, TingDotCom::class.java))
        } else {
            startActivity(Intent(this@SplashScreen, LogIn::class.java))
        }
    }

    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        Bridge.restoreInstanceState(this, savedInstanceState)
        savedInstanceState?.clear()

        userAuthentication = UserAuthentication(this@SplashScreen)

        mAppNameText = findViewById<TextView>(R.id.appNameText) as TextView

        val spanText = SpannableString("Ting.com")
        spanText.setSpan(ForegroundColorSpan(resources.getColor(R.color.colorPrimaryDark)), 0, 4, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
        spanText.setSpan(ForegroundColorSpan(resources.getColor(R.color.colorPrimary)), 4, spanText.length, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
        mAppNameText.text = spanText

        mAnimation = AnimationUtils.loadAnimation(this@SplashScreen,
            R.anim.fade_in
        )
        mAppNameText.startAnimation(mAnimation)

        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = UPDATE_INTERVAL
        locationRequest.fastestInterval = FASTEST_INTERVAL

        utilsFunctions = UtilsFunctions(this@SplashScreen)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this@SplashScreen)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                if(locationResult != null) {
                    locationResult ?: return
                    for (location in locationResult.locations){ getCurrentLocation(location) }
                }
            }
        }

        if(utilsFunctions.checkLocationPermissions()) { startLocationUpdates() }

        handler = Handler()
        handler?.postDelayed(runnable, 3000)
    }

    private fun startLocationUpdates() {
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,
            locationCallback,
            Looper.getMainLooper())
    }

    private fun getCurrentLocation(location: Location) {
        try {
            val geocoder = Geocoder(this@SplashScreen, Locale.getDefault())
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            localData.saveUserCountry(addresses[0].countryName)
            localData.saveUserTown(addresses[0].locality)
        } catch (e: Exception){ }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        handler?.removeCallbacks(runnable)
    }

    companion object {
        private const val UPDATE_INTERVAL = (60 * 60 * 1000).toLong()
        private const val FASTEST_INTERVAL = (30 * 60 * 1000).toLong()
    }
}
