package com.codepipes.ting

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.Toast
import com.codepipes.ting.dialogs.InfoDialog
import com.codepipes.ting.dialogs.SuccessOverlay
import com.codepipes.ting.dialogs.TingToast
import com.codepipes.ting.dialogs.TingToastType
import com.codepipes.ting.interfaces.SuccessDialogCloseListener
import com.codepipes.ting.models.User
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.providers.UserPlacement
import com.codepipes.ting.utils.UtilData
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
import java.lang.Exception

class CurrentRestaurant : AppCompatActivity() {

    private lateinit var userAuthentication: UserAuthentication
    private lateinit var userPlacement: UserPlacement
    private lateinit var session: User

    private lateinit var pubnub: PubNub

    @SuppressLint("PrivateResource")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_current_restaurant)

        Bridge.restoreInstanceState(this, savedInstanceState)
        savedInstanceState?.clear()

        RestaurantScanner.ACTIVITY_ID = 2

        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.elevation = 0F
        supportActionBar!!.title = ""

        try {
            val upArrow = ContextCompat.getDrawable(this@CurrentRestaurant, R.drawable.abc_ic_ab_back_material)
            upArrow!!.setColorFilter(ContextCompat.getColor(this@CurrentRestaurant, R.color.colorPrimary), PorterDuff.Mode.SRC_ATOP)
            supportActionBar!!.setHomeAsUpIndicator(upArrow)
        } catch (e: Exception) {}

        userPlacement = UserPlacement(this@CurrentRestaurant)
        userAuthentication = UserAuthentication(this@CurrentRestaurant)
        session = userAuthentication.get()!!

        val pubnubConfig = PNConfiguration()
        pubnubConfig.subscribeKey = UtilData.PUBNUB_SUBSCRIBE_KEY
        pubnubConfig.publishKey = UtilData.PUBNUB_PUBLISH_KEY
        pubnubConfig.isSecure = true

        pubnub = PubNub(pubnubConfig)
        pubnub.subscribe().channels(listOf(session.channel)).withPresence().execute()

        val snackView = findViewById<View>(android.R.id.content)

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
                        if(RestaurantScanner.ACTIVITY_ID == 2) {
                            val response = pnMessageResult.message.asJsonObject
                            when (response.get("type").asString) {
                                UtilData.SOCKET_RESPONSE_ERROR -> {
                                    TingToast(this@CurrentRestaurant, if(!response.get("message").isJsonNull){ response.get("message").asString } else { "An Error Occurred" }, TingToastType.ERROR).showToast(
                                        Toast.LENGTH_LONG)
                                }
                                UtilData.SOCKET_RESPONSE_TABLE_WAITER -> {
                                    val snackbar = Snackbar.make(snackView, "You've been assigned a Waiter", Snackbar.LENGTH_LONG)
                                    snackbar.setAction("OK") { snackbar.dismiss() }
                                    snackbar.show()

                                    val waiter = response.get("data").asJsonObject.get("waiter").asJsonObject

                                    val infoDialog = InfoDialog()
                                    val bundle = Bundle()
                                    bundle.putString("title", waiter.get("name").asString)
                                    bundle.putString("image", waiter.get("image").asString)
                                    bundle.putString("message", "This is the waiter who will be serving you today. Enjoy !")
                                    Log.i("IMAGE", waiter.get("image").asString)
                                    infoDialog.arguments = bundle
                                    infoDialog.show(this@CurrentRestaurant.fragmentManager, infoDialog.tag)
                                }
                                UtilData.SOCKET_RESPONSE_PLACEMENT_DONE -> {
                                    val successOverlay = SuccessOverlay()
                                    val bundle = Bundle()
                                    bundle.putString("message", "Placement Terminated")
                                    bundle.putString("type", "info")
                                    successOverlay.arguments = bundle
                                    successOverlay.show(fragmentManager, successOverlay.tag)
                                    successOverlay.dismissListener(object : SuccessDialogCloseListener {
                                        override fun handleDialogClose(dialog: DialogInterface?) {
                                            userPlacement.placeOut()
                                            successOverlay.dismiss()
                                            startActivity(Intent(this@CurrentRestaurant, TingDotCom::class.java))
                                        }
                                    })
                                }
                                else -> { }
                            }
                        }
                    } catch (e: Exception) {
                        TingToast(this@CurrentRestaurant, "An Error Occurred", TingToastType.ERROR).showToast(
                            Toast.LENGTH_LONG)
                    }
                }
            }

            override fun space(pubnub: PubNub, pnSpaceResult: PNSpaceResult) {}
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        startActivity(Intent(this@CurrentRestaurant, TingDotCom::class.java))
        return true
    }

    override fun onBackPressed() {
        startActivity(Intent(this@CurrentRestaurant, TingDotCom::class.java))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Bridge.saveInstanceState(this, outState)
        outState.clear()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)
        Bridge.saveInstanceState(this, outState)
        outState.clear()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { outPersistentState?.clear() }
    }

    override fun onDestroy() {
        super.onDestroy()
        RestaurantScanner.ACTIVITY_ID = 0
        Bridge.clear(this)
    }

    override fun onPause() {
        RestaurantScanner.ACTIVITY_ID = 0
        super.onPause()
    }

    override fun onResume() {
        RestaurantScanner.ACTIVITY_ID = 2
        super.onResume()
    }

    override fun onRestart() {
        RestaurantScanner.ACTIVITY_ID = 2
        super.onRestart()
    }
}
