package com.codepipes.ting.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.util.Base64
import android.graphics.drawable.Drawable
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.caverock.androidsvg.SVG
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.floor
import kotlin.math.round


class UtilsFunctions(
    private val context: Context
){

    private val REQUEST_FINE_LOCATION = 1

    public fun getToken(length: Int): String{
        val chars: String = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        var result: String = ""
        for (i in 0..length) result += chars[floor(Math.random() * chars.length).toInt()]
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

    public fun base64ToBitmap(data: String) : Bitmap {
        val imageBytes = Base64.decode(data, Base64.DEFAULT)
        return  BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    public fun isConnectedToInternet(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
    }

    public fun isConnected(): Boolean {
        return this.isConnectedToInternet()
    }

    private fun getDrawableFromUrl(url: URL): Drawable? {
        try {
            val input = url.openStream()
            return Drawable.createFromStream(input, "src")
        } catch (e: MalformedURLException) {
        } catch (e: IOException) { }
        return null
    }

    public fun vectorToBitmap(vectorDrawable: SVG): BitmapDescriptor {
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.renderToCanvas(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    public fun calculateDistance(from: LatLng, to: LatLng) : Double {
        return "%.2f".format((SphericalUtil.computeDistanceBetween(from, to) / 1000)).toDouble()
    }

    @SuppressLint("SimpleDateFormat")
    public fun statusWorkTime(open: String, close: String): MutableMap<String, String>?{
        val today = Date()
        val sdf = SimpleDateFormat("yyyy-MM-dd")
        val td = sdf.format(today)

        val now = SimpleDateFormat("yyyy-MM-dd hh:mm").parse("$td ${today.hours}:${today.minutes}").time
        val openTime = SimpleDateFormat("yyyy-MM-dd hh:mm").parse("$td $open").time
        val closeTime = SimpleDateFormat("yyyy-MM-dd hh:mm").parse("$td $close").time

        val response = mutableMapOf<String, String>()

        if(openTime>= now){
            if(((openTime - now) / (1000 * 60)) < 120){
                val r = if ((openTime - now) / (1000 * 60) > 60 ){
                    "${round(((openTime - now) / (1000 * 60 * 60)).toDouble()).toInt()} hr"
                } else {
                    "${round(((openTime - now) / (1000 * 60)).toDouble()).toInt()} min"
                }

                response["clr"] = "orange"
                response["msg"] = "Opening in $r"
                response["st"] = "closed"
                return response
            } else {
                response["clr"] = "red"
                response["msg"] = "Closed"
                response["st"] = "closed"
                return response
            }
        } else if (now > openTime){
            if(now > closeTime) {
                response["clr"] = "red"
                response["msg"] = "Closed"
                response["st"] = "closed"
                return response
            } else {
                if(((closeTime - now) / (1000 * 60)) < 120){
                    val r = if ((closeTime - now) / (1000 * 60) > 60 ){
                        "${round(((closeTime - now) / (1000 * 60 * 60)).toDouble()).toInt()} hr"
                    } else {
                        "${round(((closeTime - now) / (1000 * 60)).toDouble()).toInt()} min"
                    }

                    response["clr"] = "orange"
                    response["msg"] = "Closing in $r"
                    response["st"] = "opened"
                    return response
                } else {
                    response["clr"] = "green"
                    response["msg"] = "Opened"
                    response["st"] = "opened"
                    return response
                }
            }
        }
        return null
    }
}