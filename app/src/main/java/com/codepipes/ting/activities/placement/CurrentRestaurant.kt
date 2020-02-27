package com.codepipes.ting.activities.placement

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
import android.support.v7.view.menu.MenuBuilder
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.codepipes.ting.R
import com.codepipes.ting.activities.base.TingDotCom
import com.codepipes.ting.activities.restaurant.RestaurantAbout
import com.codepipes.ting.customclasses.ActionSheet
import com.codepipes.ting.dialogs.messages.InfoDialog
import com.codepipes.ting.dialogs.messages.SuccessOverlay
import com.codepipes.ting.dialogs.messages.TingToast
import com.codepipes.ting.dialogs.messages.TingToastType
import com.codepipes.ting.dialogs.placement.PlacementOrdersDialog
import com.codepipes.ting.dialogs.placement.PlacementPeopleDialog
import com.codepipes.ting.dialogs.placement.RestaurantMenusOrderDialog
import com.codepipes.ting.interfaces.ActionSheetCallBack
import com.codepipes.ting.interfaces.RestaurantMenusOrderCloseListener
import com.codepipes.ting.interfaces.SubmitPeoplePlacementListener
import com.codepipes.ting.interfaces.SuccessDialogCloseListener
import com.codepipes.ting.models.*
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.providers.UserPlacement
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.UtilData
import com.google.gson.Gson
import com.livefront.bridge.Bridge
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.PNCallback
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.models.consumer.PNPublishResult
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import com.pubnub.api.models.consumer.pubsub.PNSignalResult
import com.pubnub.api.models.consumer.pubsub.message_actions.PNMessageActionResult
import com.pubnub.api.models.consumer.pubsub.objects.PNMembershipResult
import com.pubnub.api.models.consumer.pubsub.objects.PNSpaceResult
import com.pubnub.api.models.consumer.pubsub.objects.PNUserResult
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_current_restaurant.*
import okhttp3.*
import java.io.IOException
import java.lang.Exception
import java.util.concurrent.TimeUnit

class CurrentRestaurant : AppCompatActivity() {

    private lateinit var userAuthentication: UserAuthentication
    private lateinit var userPlacement: UserPlacement
    private lateinit var session: User

    private lateinit var pubnub: PubNub
    private lateinit var pubnubConfig: PNConfiguration

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
            val upArrow = ContextCompat.getDrawable(this@CurrentRestaurant,
                R.drawable.abc_ic_ab_back_material
            )
            upArrow!!.setColorFilter(ContextCompat.getColor(this@CurrentRestaurant,
                R.color.colorPrimary
            ), PorterDuff.Mode.SRC_ATOP)
            supportActionBar!!.setHomeAsUpIndicator(upArrow)
        } catch (e: Exception) {}

        userPlacement = UserPlacement(this@CurrentRestaurant)
        userAuthentication = UserAuthentication(this@CurrentRestaurant)
        session = userAuthentication.get()!!

        pubnubConfig = PNConfiguration()
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
                                    TingToast(
                                        this@CurrentRestaurant,
                                        if (!response.get("message").isJsonNull) {
                                            response.get("message").asString
                                        } else {
                                            "An Error Occurred"
                                        },
                                        TingToastType.ERROR
                                    ).showToast(
                                        Toast.LENGTH_LONG)
                                }
                                UtilData.SOCKET_RESPONSE_TABLE_WAITER -> {
                                    val waiter = response.get("data").asJsonObject.get("waiter").asJsonObject
                                    val infoDialog =
                                        InfoDialog()

                                    val bundle = Bundle()
                                    bundle.putString("title", waiter.get("name").asString)
                                    bundle.putString("image", waiter.get("image").asString)
                                    bundle.putString("message", "This is the waiter who will be serving you today. Enjoy !")

                                    infoDialog.arguments = bundle
                                    infoDialog.show(this@CurrentRestaurant.fragmentManager, infoDialog.tag)

                                    getPlacement(userPlacement.getToken()!!)
                                }
                                UtilData.SOCKET_RESPONSE_PLACEMENT_DONE -> {
                                    val successOverlay =
                                        SuccessOverlay()
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
                        TingToast(
                            this@CurrentRestaurant,
                            "An Error Occurred",
                            TingToastType.ERROR
                        ).showToast(
                            Toast.LENGTH_LONG)
                    }
                }
            }

            override fun space(pubnub: PubNub, pnSpaceResult: PNSpaceResult) {}
        })

        shimmer_loader.startShimmer()

        if (userPlacement.isPlacedIn()) {
            val placement = userPlacement.get()
            if (placement != null) {
                loadPlacement(placement)
                getPlacement(placement.token)
            } else { getPlacement(userPlacement.getTempToken()!!) }
        } else {
            TingToast(
                this@CurrentRestaurant,
                "You're Not Placed In",
                TingToastType.ERROR
            ).showToast(Toast.LENGTH_LONG)
            startActivity(Intent(this@CurrentRestaurant, RestaurantScanner::class.java))
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadPlacement(placement: Placement) {
        runOnUiThread {
            shimmer_loader.visibility = View.GONE
            place_header_view.visibility = View.VISIBLE

            Picasso.get().load(placement.table.branch?.restaurant?.logoURL()).into(place_restaurant_image)
            place_restaurant_name.text = "${placement.table.branch?.restaurant?.name}, ${placement.table.branch?.name}"
            place_table_number.text = "Table No ${placement.table.number}"
            place_bill_number.text = if(placement.bill != null) { "Bill No ${placement.bill.number}" } else { "Bill No -" }
            if(placement.waiter != null) {
                place_waiter_name.text = placement.waiter.name
                Picasso.get().load("${Routes().HOST_END_POINT}${placement.waiter.image}").into(place_waiter_image)
                place_waiter_view.setOnClickListener {
                    val infoDialog = InfoDialog()
                    val bundle = Bundle()
                    bundle.putString("title", placement.waiter.name)
                    bundle.putString("image", placement.waiter.image)
                    bundle.putString("message", "This is the waiter who will be serving you today. Enjoy !")
                    infoDialog.arguments = bundle
                    infoDialog.show(this@CurrentRestaurant.fragmentManager, infoDialog.tag)
                }
            } else {
                place_waiter_name.text = "Request Waiter"
                place_waiter_view.setOnClickListener {
                    val actionSheet = ActionSheet(this@CurrentRestaurant, mutableListOf("Request Waiter For Your Table"))
                        .setTitle("Request Waiter")
                        .setColorData(resources!!.getColor(R.color.colorGray))
                        .setColorTitleCancel(resources!!.getColor(R.color.colorGoogleRedTwo))
                        .setColorSelected(resources!!.getColor(R.color.colorPrimary))
                        .setCancelTitle("Cancel")

                    actionSheet.create(object : ActionSheetCallBack {
                        override fun data(data: String, position: Int) {
                            runOnUiThread {
                                val receiver = SocketUser(placement.table.branch?.id, 1, "${placement.table.branch?.restaurant?.name}, ${placement.table.branch?.name}", placement.table.branch?.email, placement.table.branch?.restaurant?.logo, placement.table.branch?.channel)
                                val args = mapOf<String, String?>("table" to placement.table.id.toString(), "token" to userPlacement.getTempToken())
                                val data = mapOf<String, String>("table" to placement.table.number)
                                val message = SocketResponseMessage(pubnubConfig.uuid, UtilData.SOCKET_REQUEST_ASSIGN_WAITER, userAuthentication.socketUser(), receiver, 200, null, args, data)
                                pubnub.publish().channel(placement.table.branch?.channel).message(Gson().toJson(message))
                                    .async(object : PNCallback<PNPublishResult>() {
                                        override fun onResponse(result: PNPublishResult?, status: PNStatus) {
                                            if (status.isError || status.statusCode != 200) {
                                                TingToast(
                                                    this@CurrentRestaurant,
                                                    "Connection Error Occurred",
                                                    TingToastType.ERROR
                                                ).showToast(Toast.LENGTH_LONG)
                                            }
                                        }
                                    })
                            }
                        }
                    })
                }
            }
            place_people.text = "${placement.people}"
            place_people_view.setOnClickListener {
                val placementPeopleDialog = PlacementPeopleDialog()
                val bundle =  Bundle()
                bundle.putString("people", placement.people.toString())
                placementPeopleDialog.arguments = bundle
                placementPeopleDialog.show(fragmentManager, placementPeopleDialog.tag)
                placementPeopleDialog.onSubmitPeople(object : SubmitPeoplePlacementListener {

                    override fun onSubmit(people: String) {

                        placementPeopleDialog.dismiss()
                        val url = Routes().updatePlacementPeople

                        val client = OkHttpClient.Builder()
                            .readTimeout(60, TimeUnit.SECONDS)
                            .writeTimeout(60, TimeUnit.SECONDS)
                            .callTimeout(60 * 5, TimeUnit.SECONDS)
                            .build()

                        val form = MultipartBody.Builder().setType(MultipartBody.FORM)
                            .addFormDataPart("token", placement.token)
                            .addFormDataPart("people", people)
                            .build()

                        val request = Request.Builder()
                            .header("Authorization", session.token!!)
                            .url(url)
                            .post(form)
                            .build()

                        client.newCall(request).enqueue(object : Callback {

                            override fun onFailure(call: Call, e: IOException) {
                                runOnUiThread {}
                            }

                            override fun onResponse(call: Call, response: Response) {
                                val dataString = response.body()!!.string()
                                runOnUiThread {
                                    try {
                                        val placementData = Gson().fromJson(dataString, Placement::class.java)
                                        userPlacement.setToken(placementData.token)
                                        userPlacement.set(dataString)
                                        loadPlacement(placementData)
                                        val snackbar = Snackbar.make(findViewById<View>(android.R.id.content), "People Updated !!!", Snackbar.LENGTH_LONG).setAction("OK", null)
                                        snackbar.view.setBackgroundColor(resources.getColor(R.color.colorPrimary))
                                        snackbar.show()
                                    } catch (e: Exception) {
                                        try {
                                            val serverResponse = Gson().fromJson(dataString, ServerResponse::class.java)
                                            val snackbar = Snackbar.make(findViewById<View>(android.R.id.content), serverResponse.message, Snackbar.LENGTH_LONG).setAction("OK", null)
                                            snackbar.view.setBackgroundColor(resources.getColor(R.color.colorGoogleRedOne))
                                            snackbar.show()
                                        } catch (e: Exception) {
                                            val snackbar = Snackbar.make(findViewById<View>(android.R.id.content), e.message!!, Snackbar.LENGTH_LONG).setAction("OK", null)
                                            snackbar.view.setBackgroundColor(resources.getColor(R.color.colorGoogleRedOne))
                                            snackbar.show()
                                        }
                                    }
                                }
                            }
                        })
                    }
                })
            }

            place_menu_foods.setOnClickListener { showMenusDialog(1, placement.table.branch?.id ?: 0) }
            place_menu_drinks.setOnClickListener { showMenusDialog(2, placement.table.branch?.id ?: 0) }
            place_menu_dishes.setOnClickListener { showMenusDialog(3, placement.table.branch?.id ?: 0) }
            place_menu_orders.setOnClickListener {
                val ordersDialog = PlacementOrdersDialog()
                ordersDialog.show(supportFragmentManager, ordersDialog.tag)
                ordersDialog.onDialogClose(object : RestaurantMenusOrderCloseListener {
                    override fun onClose() {
                        getPlacement(userPlacement.getToken()!!)
                    }
                })
            }
        }
    }

    private fun showMenusDialog(type: Int, branch: Int) {
        val menusDialog = RestaurantMenusOrderDialog()
        val bundle = Bundle()
        bundle.putInt(MENU_TYPE_KEY, type)
        bundle.putInt(RESTO_BRANCH_KEY, branch)
        menusDialog.arguments = bundle
        menusDialog.show(supportFragmentManager, menusDialog.tag)
        menusDialog.onDialogClose(object : RestaurantMenusOrderCloseListener {
            override fun onClose() {
                getPlacement(userPlacement.getToken()!!)
            }
        })
    }

    private fun getPlacement(token: String) {

        class TokenInterceptor : Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                val url = chain.request().url().newBuilder()
                    .addQueryParameter("token", token)
                    .build()
                val request = chain.request().newBuilder()
                    .header("Authorization", session.token!!)
                    .url(url)
                    .build()
                return chain.proceed(request)
            }
        }

        val url = Routes().getCurrentPlacement

        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60 * 5, TimeUnit.SECONDS)
            .addInterceptor(TokenInterceptor())
            .build()

        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {}
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body()!!.string()
                runOnUiThread {
                    try {
                        val placement = Gson().fromJson(dataString, Placement::class.java)
                        userPlacement.setToken(placement.token)
                        userPlacement.set(dataString)
                        loadPlacement(placement)
                    } catch (e: Exception) {
                        try {
                            val serverResponse = Gson().fromJson(dataString, ServerResponse::class.java)
                            val successOverlay =
                                SuccessOverlay()
                            val bundle = Bundle()
                            bundle.putString("message", serverResponse.message)
                            bundle.putString("type", serverResponse.type)
                            successOverlay.arguments = bundle
                            successOverlay.show(fragmentManager, successOverlay.tag)
                            successOverlay.dismissListener(object : SuccessDialogCloseListener {
                                override fun handleDialogClose(dialog: DialogInterface?) {
                                    userPlacement.placeOut()
                                    successOverlay.dismiss()
                                    startActivity(Intent(this@CurrentRestaurant, TingDotCom::class.java))
                                }
                            })
                        } catch (e: Exception) {}
                    }
                }
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        startActivity(Intent(this@CurrentRestaurant, TingDotCom::class.java))
        return true
    }

    override fun onBackPressed() {
        startActivity(Intent(this@CurrentRestaurant, TingDotCom::class.java))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.placement_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.placement_restaurant_info -> {
                val intent = Intent(this@CurrentRestaurant, RestaurantAbout::class.java)
                intent.putExtra("resto", userPlacement.get()?.id)
                intent.putExtra("apiGet", userPlacement.get()?.table?.branch?.urls?.apiGet)
                startActivity(intent)
                return true
            }
        }
        return false
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

    companion object {
        public val MENU_TYPE_KEY        = "menu_type"
        public val RESTO_BRANCH_KEY     = "resto_branch"
        public val MENU_FROM_ORDER_KEY  = "from_order"
    }
}
