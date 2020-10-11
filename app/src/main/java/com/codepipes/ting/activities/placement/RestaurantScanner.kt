package com.codepipes.ting.activities.placement

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.budiyev.android.codescanner.*
import com.codepipes.ting.R
import com.codepipes.ting.dialogs.messages.ProgressOverlay
import com.codepipes.ting.dialogs.messages.TingToast
import com.codepipes.ting.dialogs.messages.TingToastType
import com.codepipes.ting.models.*
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.providers.UserPlacement
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.Constants
import com.codepipes.ting.utils.UtilsFunctions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.maps.android.SphericalUtil
import com.livefront.bridge.Bridge
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import com.pubnub.api.models.consumer.pubsub.PNSignalResult
import com.pubnub.api.models.consumer.pubsub.message_actions.PNMessageActionResult
import com.pubnub.api.models.consumer.pubsub.objects.PNMembershipResult
import com.pubnub.api.models.consumer.pubsub.objects.PNSpaceResult
import com.pubnub.api.models.consumer.pubsub.objects.PNUserResult
import kotlinx.android.synthetic.main.activity_restaurant_scanner.*
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class RestaurantScanner : AppCompatActivity() {

    private lateinit var codeScanner: CodeScanner

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var hasScanned: Boolean = false

    private lateinit var utilsFunctions: UtilsFunctions
    private lateinit var userPlacement: UserPlacement

    private lateinit var userAuthentication: UserAuthentication
    private lateinit var session: User

    private val progressOverlay = ProgressOverlay()

    private lateinit var pubnubConfig: PNConfiguration
    private lateinit var pubnub: PubNub

    private var hasShownToast: Boolean =  false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurant_scanner)

        Bridge.restoreInstanceState(this, savedInstanceState)
        savedInstanceState?.clear()

        ACTIVITY_ID = 1

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        utilsFunctions = UtilsFunctions(this@RestaurantScanner)
        userPlacement = UserPlacement(this@RestaurantScanner)

        userAuthentication = UserAuthentication(this@RestaurantScanner)
        session = userAuthentication.get()!!

        pubnubConfig = PNConfiguration()
        pubnubConfig.subscribeKey = Constants.PUBNUB_SUBSCRIBE_KEY
        pubnubConfig.publishKey = Constants.PUBNUB_PUBLISH_KEY
        pubnubConfig.isSecure = true

        pubnub = PubNub(pubnubConfig)
        pubnub.subscribe().channels(listOf(session.channel)).withPresence().execute()

        pubnub.addListener(object : SubscribeCallback() {

            override fun signal(pubnub: PubNub, pnSignalResult: PNSignalResult) {}

            override fun status(pubnub: PubNub, pnStatus: PNStatus) {}

            override fun user(pubnub: PubNub, pnUserResult: PNUserResult) {}

            override fun messageAction(pubnub: PubNub, pnMessageActionResult: PNMessageActionResult) {}

            override fun presence(pubnub: PubNub, pnPresenceEventResult: PNPresenceEventResult) {}

            override fun membership(pubnub: PubNub, pnMembershipResult: PNMembershipResult) {}

            override fun message(pubnub: PubNub, pnMessageResult: PNMessageResult) {
                runOnUiThread {
                    try {
                        if(ACTIVITY_ID == 1) {
                            val response = pnMessageResult.message.asJsonObject
                            when (response.get("type").asString) {
                                Constants.SOCKET_RESPONSE_RESTO_TABLE -> {
                                    val dataObject = response.getAsJsonObject("data")
                                    if(dataObject != null) {
                                        userPlacement.setToken(dataObject.get("token").asString)
                                        startActivity(Intent(this@RestaurantScanner, CurrentRestaurant::class.java))
                                    } else {
                                        codeScanner.startPreview()
                                        TingToast(
                                            this@RestaurantScanner,
                                            "Try Again",
                                            TingToastType.DEFAULT
                                        ).showToast(Toast.LENGTH_LONG)
                                    }
                                }
                                Constants.SOCKET_RESPONSE_ERROR -> {
                                    progressOverlay.dismiss()
                                    codeScanner.startPreview()
                                    TingToast(
                                        this@RestaurantScanner,
                                        if (!response.get("message").isJsonNull) {
                                            response.get("message").asString
                                        } else {
                                            "An Error Occurred"
                                        },
                                        TingToastType.ERROR
                                    ).showToast(Toast.LENGTH_LONG)
                                }
                                Constants.SOCKET_RESPONSE_PLACEMENT_DONE -> {
                                    progressOverlay.dismiss()
                                    userPlacement.placeOut()
                                    codeScanner.startPreview()
                                    TingToast(
                                        this@RestaurantScanner,
                                        "Placement Is Done. Try Again",
                                        TingToastType.DEFAULT
                                    ).showToast(Toast.LENGTH_LONG)
                                }
                                else -> {
                                    progressOverlay.dismiss()
                                    codeScanner.startPreview()
                                    TingToast(
                                        this@RestaurantScanner,
                                        "No Response. Try Again",
                                        TingToastType.DEFAULT
                                    ).showToast(Toast.LENGTH_LONG)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        progressOverlay.dismiss()
                        codeScanner.startPreview()
                        TingToast(
                            this@RestaurantScanner,
                            "An Error Occurred",
                            TingToastType.ERROR
                        ).showToast(Toast.LENGTH_LONG)
                    }
                }
            }

            override fun space(pubnub: PubNub, pnSpaceResult: PNSpaceResult) {}
        })

        codeScanner = CodeScanner(this, scanner_view)

        codeScanner.camera = CodeScanner.CAMERA_BACK
        codeScanner.formats = CodeScanner.ALL_FORMATS

        codeScanner.autoFocusMode = AutoFocusMode.SAFE
        codeScanner.scanMode = ScanMode.SINGLE
        codeScanner.isAutoFocusEnabled = true
        codeScanner.isFlashEnabled = false

        if (ContextCompat.checkSelfPermission(
                this@RestaurantScanner,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            codeScanner.decodeCallback = DecodeCallback { result ->
                handleQRCodeScan(result)
            }
            codeScanner.errorCallback = ErrorCallback {
                runOnUiThread {
                    codeScanner.startPreview()
                    if(!hasShownToast) {
                        hasShownToast = true
                        TingToast(
                            this@RestaurantScanner,
                            it.message!!,
                            TingToastType.ERROR
                        ).showToast(Toast.LENGTH_LONG)
                    }
                }
            }
        } else {
            ActivityCompat.requestPermissions(this@RestaurantScanner,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_REQUEST_PERMISSION
            )
        }

        scanner_view.setOnClickListener { codeScanner.startPreview() }
        close_scanner.setOnClickListener { onBackPressed() }
    }

    private fun handleQRCodeScan(result: com.google.zxing.Result) {

        val stringURL = "${Routes.requestRestaurantTable}?table=${result.text}"

        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .header("Authorization", session.token!!)
            .url(stringURL)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    codeScanner.startPreview()
                    TingToast(
                        this@RestaurantScanner,
                        e.message!!,
                        TingToastType.ERROR
                    ).showToast(Toast.LENGTH_LONG)
                }
            }

            @SuppressLint("MissingPermission")
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body!!.string()
                val gson = Gson()
                runOnUiThread {
                    if(utilsFunctions.checkLocationPermissions()) {
                        try {
                            val table = gson.fromJson(responseBody, RestaurantTable::class.java)
                            fusedLocationClient.lastLocation.addOnSuccessListener {
                                if(it != null){
                                    runOnUiThread {
                                        if (table.branch?.latitude == null) {
                                            try {
                                                val placement = gson.fromJson(responseBody, Placement::class.java)
                                                userPlacement.setToken(placement.token)
                                                userPlacement.set(gson.toJson(placement))
                                                startActivity(Intent(this@RestaurantScanner, CurrentRestaurant::class.java))
                                            } catch (e: Exception){
                                                codeScanner.startPreview()
                                                TingToast(
                                                    this@RestaurantScanner,
                                                    "An Error Occurred",
                                                    TingToastType.ERROR
                                                ).showToast(Toast.LENGTH_LONG)
                                            }
                                        } else {
                                            val userLocation = LatLng(it.latitude, it.longitude)
                                            val branchLocation = LatLng(table.branch.latitude, table.branch.longitude)
                                            val distance = SphericalUtil.computeDistanceBetween(userLocation, branchLocation)

                                            val statusTimer = utilsFunctions.statusWorkTime(
                                                table.branch.restaurant!!.opening,
                                                table.branch.restaurant.closing
                                            )

                                            if(statusTimer["st"] == "opened") {
                                                if(distance <= 5000) {
                                                    try {
                                                        progressOverlay.show(supportFragmentManager, progressOverlay.tag)
                                                        if(userPlacement.getTempToken().isNullOrEmpty()) {
                                                            userPlacement.setTempToken(utilsFunctions.getToken((100 until 200).random()))
                                                        }
                                                        val receiver = SocketUser(table.branch.id, 1, "${table.branch.restaurant.name}, ${table.branch.name}", table.branch.email, table.branch.restaurant.logo, table.branch.channel)
                                                        val args = mapOf<String, String?>("table" to table.id.toString(), "token" to userPlacement.getTempToken())
                                                        val data = mapOf<String, String>("table" to table.number)
                                                        val message = SocketResponseMessage(pubnubConfig.uuid, Constants.SOCKET_REQUEST_RESTO_TABLE, userAuthentication.socketUser(), receiver, null, 200, null, args, data)
                                                        pubnub.publish().channel(table.branch.channel).message(Gson().toJson(message))
                                                            .async { _, status ->
                                                                if (status.isError || status.statusCode != 200) {
                                                                    progressOverlay.dismiss()
                                                                    codeScanner.startPreview()
                                                                    TingToast(
                                                                        this@RestaurantScanner,
                                                                        "Connection Error Occurred",
                                                                        TingToastType.ERROR
                                                                    ).showToast(Toast.LENGTH_LONG)
                                                                }
                                                            }
                                                    } catch (e: java.lang.Exception){
                                                        codeScanner.startPreview()
                                                        try {
                                                            val serverResponse = gson.fromJson(responseBody, ServerResponse::class.java)
                                                            TingToast(
                                                                this@RestaurantScanner,
                                                                serverResponse.message,
                                                                TingToastType.ERROR
                                                            ).showToast(Toast.LENGTH_LONG)
                                                        } catch (e: java.lang.Exception) {
                                                            TingToast(
                                                                this@RestaurantScanner,
                                                                "An Error Has Occurred",
                                                                TingToastType.ERROR
                                                            ).showToast(Toast.LENGTH_LONG)
                                                        }
                                                    }
                                                } else {
                                                    codeScanner.startPreview()
                                                    TingToast(
                                                        this@RestaurantScanner,
                                                        "Are You sure You Are At The Restaurant?",
                                                        TingToastType.DEFAULT
                                                    ).showToast(Toast.LENGTH_LONG)
                                                }
                                            } else {
                                                codeScanner.startPreview()
                                                TingToast(
                                                    this@RestaurantScanner,
                                                    "The Restaurant Is Closed",
                                                    TingToastType.DEFAULT
                                                ).showToast(Toast.LENGTH_LONG)
                                            }
                                        }
                                    }
                                }
                            }.addOnCanceledListener {
                                runOnUiThread {
                                    codeScanner.startPreview()
                                    TingToast(
                                        this@RestaurantScanner,
                                        "Operation Canceled",
                                        TingToastType.ERROR
                                    ).showToast(Toast.LENGTH_LONG)
                                }
                            }.addOnFailureListener {
                                runOnUiThread {
                                    codeScanner.startPreview()
                                    TingToast(
                                        this@RestaurantScanner,
                                        it.message!!,
                                        TingToastType.ERROR
                                    ).showToast(Toast.LENGTH_LONG)
                                }
                            }
                        } catch (e: Exception){
                            runOnUiThread {
                                try {
                                    val placement = gson.fromJson(responseBody, Placement::class.java)
                                    userPlacement.setToken(placement.token)
                                    userPlacement.set(gson.toJson(placement))
                                    startActivity(Intent(this@RestaurantScanner, CurrentRestaurant::class.java))
                                } catch (e: Exception){
                                    codeScanner.startPreview()
                                    TingToast(
                                        this@RestaurantScanner,
                                        "An Error Occurred",
                                        TingToastType.ERROR
                                    ).showToast(Toast.LENGTH_LONG)
                                }
                            }
                        }
                    } else {
                        codeScanner.startPreview()
                        TingToast(
                            this@RestaurantScanner,
                            "Please, Allow App To Use Your Location",
                            TingToastType.ERROR
                        ).showToast(Toast.LENGTH_LONG)
                    }
                }
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == CAMERA_REQUEST_PERMISSION) {
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                codeScanner.startPreview()
                codeScanner.decodeCallback = DecodeCallback { result ->
                    handleQRCodeScan(result)
                }
                codeScanner.errorCallback = ErrorCallback {
                    runOnUiThread {
                        codeScanner.startPreview()
                        TingToast(
                            this@RestaurantScanner,
                            it.message!!,
                            TingToastType.ERROR
                        ).showToast(Toast.LENGTH_LONG)
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)
        Bridge.saveInstanceState(this, outState)
        outState.clear()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { outPersistentState?.clear() }
    }

    override fun onDestroy() {
        super.onDestroy()
        ACTIVITY_ID = 0
        Bridge.clear(this)
    }

    override fun onResume() {
        super.onResume()
        ACTIVITY_ID = 1
        try {
            codeScanner.startPreview()
        } catch (e: java.lang.Exception) {}
    }

    override fun onPause() {
        try {
            codeScanner.releaseResources()
        } catch (e: java.lang.Exception) {}
        ACTIVITY_ID = 0
        super.onPause()
    }

    override fun onRestart() {
        ACTIVITY_ID = 1
        super.onRestart()
    }

    companion object {
        public var ACTIVITY_ID = 0
        private const val CAMERA_REQUEST_PERMISSION = 2
    }
}
