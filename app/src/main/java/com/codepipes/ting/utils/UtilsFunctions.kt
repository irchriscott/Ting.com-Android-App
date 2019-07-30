package com.codepipes.ting.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat

class UtilsFunctions(
    private val context: Context
){

    private val REQUEST_FINE_LOCATION = 1

    public fun getToken(length: Int): String{
        val chars: String = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        var result: String = ""
        for (i in 0..length) result += chars[Math.floor(Math.random() * chars.length).toInt()]
        return result
    }

    public fun checkLocationPermissions(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            true
        } else {
            requestLocationPermissions()
            false
        }
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            context as Activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_FINE_LOCATION
        )
    }
}