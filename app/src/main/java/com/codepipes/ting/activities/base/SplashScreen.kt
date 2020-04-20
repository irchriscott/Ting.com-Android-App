package com.codepipes.ting.activities.base

import android.content.Intent
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
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

        utilsFunctions = UtilsFunctions(this@SplashScreen)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this@SplashScreen)

        if(utilsFunctions.checkLocationPermissions()) { getCurrentLocation() }

        handler = Handler()
        handler?.postDelayed(runnable, 3000)
    }

    private fun getCurrentLocation() {
        try {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener {
                if(it != null) {
                    try {
                        val geocoder = Geocoder(this@SplashScreen, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                        localData.saveUserCountry(addresses[0].countryName)
                        localData.saveUserTown(addresses[0].locality)
                    } catch (e: Exception) { }
                }
            }.addOnFailureListener {}
        } catch (e: Exception){ }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler?.removeCallbacks(runnable)
    }
}
