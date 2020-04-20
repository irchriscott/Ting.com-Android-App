package com.codepipes.ting.activities.menu

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.View
import android.widget.Toast
import com.codepipes.ting.R
import com.codepipes.ting.activities.restaurant.RestaurantProfile
import com.codepipes.ting.adapters.menu.PromotionMenusListAdapter
import com.codepipes.ting.custom.ActionSheet
import com.codepipes.ting.dialogs.messages.TingToast
import com.codepipes.ting.dialogs.messages.TingToastType
import com.codepipes.ting.fragments.restaurants.RestaurantsMapFragment
import com.codepipes.ting.interfaces.ActionSheetCallBack
import com.codepipes.ting.models.*
import com.codepipes.ting.models.MenuPromotion
import com.codepipes.ting.models.RestaurantMenu
import com.codepipes.ting.providers.LocalData
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.UtilsFunctions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.livefront.bridge.Bridge
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_menu_promotion.*
import kotlinx.android.synthetic.main.activity_menu_promotion.progress_loader
import kotlinx.android.synthetic.main.activity_menu_promotion.shimmerLoader
import okhttp3.*
import java.io.IOException
import java.text.NumberFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class MenuPromotion : AppCompatActivity() {

    private lateinit var promotion: MenuPromotion
    private lateinit var gson: Gson

    private lateinit var utilsFunctions: UtilsFunctions
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var localData: LocalData

    private lateinit var userAuthentication: UserAuthentication
    private lateinit var session: User

    private var selectedLatitude: Double = 0.0
    private var selectedLongitude: Double = 0.0

    private lateinit var promotionTimer: Timer
    private val TIMER_PERIOD = 6000.toLong()

    @SuppressLint("PrivateResource", "DefaultLocale")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_promotion)

        Bridge.restoreInstanceState(this, savedInstanceState)
        savedInstanceState?.clear()

        gson = Gson()
        utilsFunctions = UtilsFunctions(this@MenuPromotion)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this@MenuPromotion)
        userAuthentication = UserAuthentication(this@MenuPromotion)
        session = userAuthentication.get()!!

        localData = LocalData(this@MenuPromotion)
        promotionTimer = Timer()

        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.elevation = 0F
        supportActionBar!!.title = "Menu Promotion".toUpperCase()

        try {
            val upArrow = ContextCompat.getDrawable(this@MenuPromotion,
                R.drawable.abc_ic_ab_back_material
            )
            upArrow!!.setColorFilter(ContextCompat.getColor(this@MenuPromotion,
                R.color.colorPrimary
            ), PorterDuff.Mode.SRC_ATOP)
            supportActionBar!!.setHomeAsUpIndicator(upArrow)
        } catch (e: java.lang.Exception) {}

        val promoId = intent.getIntExtra("promo", 0)
        val url = intent.getStringExtra("url")

        if(localData.getPromotion(promoId) != null){
            promotion = localData.getPromotion(promoId)!!
            shimmerLoader.stopShimmer()
            shimmerLoader.visibility = View.GONE
            promotion_view.visibility = View.VISIBLE
            this.showMenuPromotion(promotion, false)
            promotionTimer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() { getMenuPromotion(url) }
            }, TIMER_PERIOD, TIMER_PERIOD)
            this.getMenuPromotion(url)
        } else {
            shimmerLoader.startShimmer()
            shimmerLoader.visibility = View.VISIBLE
            promotion_view.visibility = View.GONE
            promotionTimer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() { getMenuPromotion(intent.getStringExtra("url")) }
            }, TIMER_PERIOD, TIMER_PERIOD)
            this.getMenuPromotion(intent.getStringExtra("url"))
        }

        refresh_promotion.setColorSchemeColors(resources.getColor(R.color.colorPrimary), resources.getColor(
            R.color.colorAccentMain
        ), resources.getColor(R.color.colorPrimaryDark), resources.getColor(R.color.colorAccentMain))
        refresh_promotion.setOnRefreshListener {
            refresh_promotion.isRefreshing = true
            this.getMenuPromotion(intent.getStringExtra("url"))
        }
    }

    private fun getMenuPromotion(url: String){
        val stringURL = "${Routes.HOST_END_POINT}$url"

        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(stringURL)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { refresh_promotion.isRefreshing = false }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body!!.string()
                val gson = Gson()
                try{
                    val menuPromotion = gson.fromJson(responseBody, MenuPromotion::class.java)
                    runOnUiThread {
                        promotionTimer.cancel()
                        promotion = menuPromotion

                        progress_loader.visibility = View.GONE
                        promotion_view.visibility = View.VISIBLE
                        refresh_promotion.isRefreshing = false

                        shimmerLoader.stopShimmer()
                        shimmerLoader.visibility = View.GONE
                        promotion_view.visibility = View.VISIBLE
                        showMenuPromotion(menuPromotion, true)
                    }
                } catch (e: Exception){
                    runOnUiThread {
                        TingToast(
                            this@MenuPromotion,
                            "An Error Has Occurred",
                            TingToastType.ERROR
                        ).showToast(Toast.LENGTH_LONG)
                    }
                }
            }
        })
    }

    @SuppressLint("MissingPermission", "SetTextI18n", "DefaultLocale")
    private fun showMenuPromotion(promotion: com.codepipes.ting.models.MenuPromotion, clickable: Boolean){

        val menuList = mutableListOf<String>()
        menuList.add("Current Location")
        session.addresses?.addresses!!.forEach {
            menuList.add("${it.type} - ${it.address}")
        }

        promotion_restaurant_distance_view.setOnClickListener {
            val cx = (it.x + it.width / 2).toInt()
            val cy = (it.y + it.height).toInt()

            val mapFragment =  RestaurantsMapFragment()
            val args: Bundle = Bundle()

            args.putInt("cx", cx)
            args.putInt("cy", cy)
            args.putDouble("lat", selectedLatitude)
            args.putDouble("lng", selectedLongitude)
            args.putString("resto", Gson().toJson(promotion.branch))

            mapFragment.arguments = args
            mapFragment.show(supportFragmentManager, mapFragment.tag)
        }

        promotion_restaurant_distance_view.setOnLongClickListener {
            val actionSheet = ActionSheet(this@MenuPromotion, menuList)
                .setTitle("Restaurant Near Location")
                .setColorData(resources.getColor(R.color.colorGray))
                .setColorTitleCancel(resources.getColor(R.color.colorGoogleRedTwo))
                .setColorSelected(resources.getColor(R.color.colorPrimary))
                .setCancelTitle("Cancel")

            actionSheet.create(object : ActionSheetCallBack {

                override fun data(data: String, position: Int) {
                    if (promotion.branch != null) {
                        if (position == 0) {
                            if (utilsFunctions.checkLocationPermissions()) {
                                try {
                                    fusedLocationClient.lastLocation.addOnSuccessListener {
                                        if (it != null) {
                                            val from = LatLng(it.latitude, it.longitude)
                                            selectedLatitude = it.latitude
                                            selectedLongitude = it.longitude
                                            val to = LatLng(promotion.branch.latitude, promotion.branch.longitude)
                                            val dist = utilsFunctions.calculateDistance(from, to)
                                            promotion.branch.dist = dist
                                            promotion.branch.fromLocation = from
                                            runOnUiThread { promotion_restaurant_distance.text = "${dist.toString()} Km" }
                                        } else {
                                            val from = LatLng(
                                                session.addresses!!.addresses[0].latitude,
                                                session.addresses!!.addresses[0].longitude
                                            )
                                            selectedLatitude = session.addresses!!.addresses[0].latitude
                                            selectedLongitude = session.addresses!!.addresses[0].longitude
                                            val to = LatLng(promotion.branch.latitude, promotion.branch.longitude)
                                            val dist = utilsFunctions.calculateDistance(from, to)
                                            promotion.branch.dist = dist
                                            promotion.branch.fromLocation = from
                                            runOnUiThread { promotion_restaurant_distance.text = "${dist.toString()} Km" }
                                        }
                                    }.addOnFailureListener {
                                        val from = LatLng(
                                            session.addresses!!.addresses[0].latitude,
                                            session.addresses!!.addresses[0].longitude
                                        )
                                        selectedLatitude = session.addresses!!.addresses[0].latitude
                                        selectedLongitude = session.addresses!!.addresses[0].longitude
                                        val to = LatLng(promotion.branch.latitude, promotion.branch.longitude)
                                        val dist = utilsFunctions.calculateDistance(from, to)
                                        promotion.branch.dist = dist
                                        promotion.branch.fromLocation = from
                                        runOnUiThread { promotion_restaurant_distance.text = "${dist.toString()} Km" }
                                        TingToast(
                                            this@MenuPromotion,
                                            it.message!!,
                                            TingToastType.ERROR
                                        ).showToast(
                                            Toast.LENGTH_LONG
                                        )
                                    }
                                } catch (e: java.lang.Exception) {
                                    TingToast(
                                        this@MenuPromotion,
                                        e.message!!.capitalize(),
                                        TingToastType.ERROR
                                    ).showToast(Toast.LENGTH_LONG)
                                }
                            }
                        } else {
                            val address = session.addresses?.addresses!![position - 1]
                            val from = LatLng(address.latitude, address.longitude)
                            selectedLatitude = address.latitude
                            selectedLongitude = address.longitude
                            val to = LatLng(promotion.branch.latitude, promotion.branch.longitude)
                            val dist = utilsFunctions.calculateDistance(from, to)
                            promotion.branch.dist = dist
                            promotion.branch.fromLocation = from
                            runOnUiThread { promotion_restaurant_distance.text = "${dist.toString()} Km" }
                        }
                    }
                }
            })

            return@setOnLongClickListener true
        }

        promotion_restaurant_view.setOnClickListener {
            val intent = Intent(this@MenuPromotion, RestaurantProfile::class.java)
            intent.putExtra("resto", promotion.branch?.id)
            startActivity(intent)
        }

        Picasso.get().load("${Routes.HOST_END_POINT}${promotion.posterImage}").into(promotion_poster_image)
        promotion_title.text = promotion.occasionEvent
        promotion_menu_type_on_text.text = "Promotion On ${promotion.promotionItem.type.name}"
        promotion_time.text = promotion.period

        if(promotion.isOn && promotion.isOnToday){
            promotion_status.background = resources.getDrawable(R.drawable.background_time_green)
            promotion_status_icon.setImageDrawable(resources.getDrawable(R.drawable.ic_check_white_48dp))
            promotion_status_text.text = "Is On Today"
        } else {
            promotion_status.background = resources.getDrawable(R.drawable.background_time_red)
            promotion_status_icon.setImageDrawable(resources.getDrawable(R.drawable.ic_close_white_24dp))
            promotion_status_text.text = "Is Off Today"
        }

        if (promotion.reduction.hasReduction){
            promotion_reduction.text = "Order this menu and get ${promotion.reduction.amount} ${promotion.reduction.reductionType} reduction"
        } else {
            promotion_reduction_icon.visibility = View.GONE
            promotion_reduction.visibility = View.GONE
        }

        if(promotion.supplement.hasSupplement){
            if (!promotion.supplement.isSame){ promotion_supplement.text = "Order ${promotion.supplement.minQuantity} of this menu and get ${promotion.supplement.quantity} free ${promotion.supplement.supplement?.menu?.name}" }
            else { promotion_supplement.text = "Order ${promotion.supplement.minQuantity} of this menu and get ${promotion.supplement.quantity} more for free" }
        } else {
            promotion_supplement_icon.visibility = View.GONE
            promotion_supplement.visibility = View.GONE
        }

        when(promotion.promotionItem.type.id){
            0 -> {
                if(promotion.menus.menus != null){
                    val menus = promotion.menus.menus
                    if (menus.isNotEmpty()){
                        promotion_menus_view.visibility = View.VISIBLE
                        promotion_menus.layoutManager = LinearLayoutManager(this@MenuPromotion)
                        promotion_menus.adapter = PromotionMenusListAdapter(menus.shuffled().toMutableList(), supportFragmentManager)
                    } else { promotion_menus_view.visibility = View.GONE }
                } else { promotion_menus_view.visibility = View.GONE }
                promotion_menu_on.visibility = View.GONE
            }
            1 -> {
                if(promotion.menus.menus != null){
                    val menus = promotion.menus.menus
                    if (menus.isNotEmpty()){
                        promotion_menus_view.visibility = View.VISIBLE
                        promotion_menus.layoutManager = LinearLayoutManager(this@MenuPromotion)
                        promotion_menus.adapter = PromotionMenusListAdapter(menus.shuffled().toMutableList(), supportFragmentManager)
                    } else { promotion_menus_view.visibility = View.GONE }
                } else {promotion_menus_view.visibility = View.GONE }
                promotion_menu_on.visibility = View.GONE
            }
            2 -> {
                if(promotion.menus.menus != null){
                    val menus = promotion.menus.menus
                    if (menus.isNotEmpty()){
                        promotion_menus_view.visibility = View.VISIBLE
                        promotion_menus.layoutManager = LinearLayoutManager(this@MenuPromotion)
                        promotion_menus.adapter = PromotionMenusListAdapter(menus.shuffled().toMutableList(), supportFragmentManager)
                    } else { promotion_menus_view.visibility = View.GONE }
                } else { promotion_menus_view.visibility = View.GONE }
                promotion_menu_on.visibility = View.GONE
            }
            3 -> {
                if(promotion.menus.menus != null){
                    val menus = promotion.menus.menus
                    if (menus.isNotEmpty()){
                        promotion_menus_view.visibility = View.VISIBLE
                        promotion_menus.layoutManager = LinearLayoutManager(this@MenuPromotion)
                        promotion_menus.adapter = PromotionMenusListAdapter(menus.shuffled().toMutableList(), supportFragmentManager)
                    } else { promotion_menus_view.visibility = View.GONE }
                } else { promotion_menus_view.visibility = View.GONE }
                promotion_menu_on.visibility = View.GONE
            }
            4 -> {
                val index = (0 until promotion.promotionItem.menu?.menu?.images?.count!! - 1).random()
                val image = promotion.promotionItem.menu.menu.images.images[index]
                Picasso.get().load("${Routes.HOST_END_POINT}${image.image}").into(promotion_menu_on_image)
                promotion_menu_on_text.text = "Promotion On ${promotion.promotionItem.menu.menu.name}"

                val menus = mutableListOf<RestaurantMenu>()
                menus.add(promotion.promotionItem.menu)

                promotion_menus_view.visibility = View.VISIBLE
                promotion_menus.layoutManager = LinearLayoutManager(this@MenuPromotion)
                promotion_menus.adapter = PromotionMenusListAdapter(menus.toMutableList(), supportFragmentManager)
            }
            5 -> {
                if(promotion.menus.menus != null){
                    val menus = promotion.menus.menus
                    if (menus.isNotEmpty()){
                        promotion_menus_view.visibility = View.VISIBLE
                        promotion_menus.layoutManager = LinearLayoutManager(this@MenuPromotion)
                        promotion_menus.adapter = PromotionMenusListAdapter(menus.shuffled().toMutableList(), supportFragmentManager)
                    } else { promotion_menus_view.visibility = View.GONE }
                } else { promotion_menus_view.visibility = View.GONE }

                promotion_menu_on_text.text = "Promotion On ${promotion.promotionItem.category?.name}"
                Picasso.get().load("${Routes.HOST_END_POINT}${promotion.promotionItem.category?.image}").into(promotion_menu_on_image)
            }
            else -> {
                promotion_menus_view.visibility = View.GONE
                promotion_menu_on.visibility = View.GONE
            }
        }

        promotion_description_text.setHtml(promotion.description)
        promotion_created_at.text = utilsFunctions.timeAgo(promotion.createdAt)
        promotion_interests.text = NumberFormat.getNumberInstance().format(promotion.interests.count)

        if (promotion.branch != null && promotion.restaurant != null) {

            Picasso.get().load(promotion.restaurant.logoURL()).into(menu_restaurant_image)
            promotion_restaurant_name.text = "${promotion.restaurant.name}, ${promotion.branch.name}"

            promotion_restaurant_name.isClickable = true
            promotion_restaurant_name.setOnClickListener {
                if(clickable){
                    val intent = Intent(this@MenuPromotion, RestaurantProfile::class.java)
                    intent.putExtra("resto", promotion.branch.id)
                    intent.putExtra("tab", 0)
                    startActivity(intent)
                }
            }

            if (promotion.branch.isAvailable) {

                val status =
                    utilsFunctions.statusWorkTime(promotion.restaurant.opening, promotion.restaurant.closing)
                promotion_restaurant_time.text = status["msg"]

                when (status["clr"]) {
                    "green" -> {
                        promotion_restaurant_work_status.background =
                            resources.getDrawable(R.drawable.background_time_green)
                    }
                    "orange" -> {
                        promotion_restaurant_work_status.background =
                            resources.getDrawable(R.drawable.background_time_orange)
                    }
                    "red" -> {
                        promotion_restaurant_work_status.background =
                            resources.getDrawable(R.drawable.background_time_red)
                    }
                }

                Timer().scheduleAtFixedRate(object : TimerTask() {
                    override fun run() {
                        runOnUiThread {

                            val statusTimer = utilsFunctions.statusWorkTime(
                                promotion.restaurant.opening,
                                promotion.restaurant.closing
                            )
                            promotion_restaurant_time.text = statusTimer?.get("msg")

                            when (statusTimer?.get("clr")) {
                                "green" -> {
                                    promotion_restaurant_work_status.background =
                                        resources.getDrawable(R.drawable.background_time_green)
                                }
                                "orange" -> {
                                    promotion_restaurant_work_status.background =
                                        resources.getDrawable(R.drawable.background_time_orange)
                                }
                                "red" -> {
                                    promotion_restaurant_work_status.background =
                                        resources.getDrawable(R.drawable.background_time_red)
                                }
                            }
                        }
                    }
                }, 0, 10000)
            } else {
                promotion_restaurant_work_status.background = resources.getDrawable(R.drawable.background_time_red)
                promotion_restaurant_work_status_icon.setImageDrawable(resources.getDrawable(R.drawable.ic_close_white_24dp))
                promotion_restaurant_time.text = "Not Available"
            }

            if (utilsFunctions.checkLocationPermissions()) {
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener {
                        if (it != null) {
                            val from = LatLng(it.latitude, it.longitude)
                            selectedLatitude = it.latitude
                            selectedLongitude = it.longitude
                            val to = LatLng(promotion.branch.latitude, promotion.branch.longitude)
                            val dist = utilsFunctions.calculateDistance(from, to)
                            promotion.branch.dist = dist
                            promotion.branch.fromLocation = from
                            runOnUiThread { promotion_restaurant_distance.text = "${dist.toString()} Km" }
                        } else {
                            val from = LatLng(
                                session.addresses!!.addresses[0].latitude,
                                session.addresses!!.addresses[0].longitude
                            )
                            selectedLatitude = session.addresses!!.addresses[0].latitude
                            selectedLongitude = session.addresses!!.addresses[0].longitude
                            val to = LatLng(promotion.branch.latitude, promotion.branch.longitude)
                            val dist = utilsFunctions.calculateDistance(from, to)
                            promotion.branch.dist = dist
                            promotion.branch.fromLocation = from
                            runOnUiThread { promotion_restaurant_distance.text = "${dist.toString()} Km" }
                        }
                    }.addOnFailureListener {
                        val from = LatLng(
                            session.addresses!!.addresses[0].latitude,
                            session.addresses!!.addresses[0].longitude
                        )
                        selectedLatitude = session.addresses!!.addresses[0].latitude
                        selectedLongitude = session.addresses!!.addresses[0].longitude
                        val to = LatLng(promotion.branch.latitude, promotion.branch.longitude)
                        val dist = utilsFunctions.calculateDistance(from, to)
                        promotion.branch.dist = dist
                        promotion.branch.fromLocation = from
                        runOnUiThread { promotion_restaurant_distance.text = "${dist.toString()} Km" }
                        TingToast(
                            this@MenuPromotion,
                            it.message!!,
                            TingToastType.ERROR
                        ).showToast(Toast.LENGTH_LONG)
                    }
                } catch (e: java.lang.Exception) {
                    TingToast(
                        this@MenuPromotion,
                        e.message!!.capitalize(),
                        TingToastType.ERROR
                    ).showToast(Toast.LENGTH_LONG)
                }
            }

        } else {
            promotion_restaurant_name.isClickable = false
            promotion_restaurant_name.setOnClickListener(null)
            promotion_restaurant_name.text = "Loading..."
            promotion_restaurant_distance.text = "0.0 Km"
            promotion_restaurant_time.text = "Loading..."
        }

        if(promotion.interests.count > 0){
            if(utilsFunctions.userPromotionInterest(promotion.interests.interests, session)){
                promotion_interest_button.tag = 1
                promotion_interest_button.playAnimation()
                promotion_interest_button.isChecked = true
            } else {
                promotion_interest_button.tag = 0
                promotion_interest_button.isChecked = false
            }
        } else {
            promotion_interest_button.tag = 0
            promotion_interest_button.isChecked = false
        }

        promotion_interest_button.setOnClickListener {
            this.interestPromotion("${Routes.HOST_END_POINT}${promotion.urls.apiInterest}", session.token!!, promotion.id)
        }
    }

    private fun interestPromotion(url: String, token: String, promotion: Int){
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("promo", promotion.toString())
            .build()

        val request = Request.Builder()
            .header("Authorization", token)
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    TingToast(
                        this@MenuPromotion,
                        e.message!!,
                        TingToastType.ERROR
                    ).showToast(Toast.LENGTH_LONG)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body!!.string()
                try{
                    val serverResponse = gson.fromJson(responseBody, ServerResponse::class.java)
                    runOnUiThread {
                        if (serverResponse.status == 200){
                            if(serverResponse.message.contains("Not") || promotion_interest_button.tag == 1){
                                promotion_interest_button.tag = 0
                                promotion_interest_button.isChecked = false
                            } else {
                                promotion_interest_button.tag = 1
                                promotion_interest_button.playAnimation()
                                promotion_interest_button.isChecked = true
                            }
                            TingToast(
                                this@MenuPromotion,
                                serverResponse.message,
                                TingToastType.SUCCESS
                            ).showToast(Toast.LENGTH_LONG)
                        } else { TingToast(
                            this@MenuPromotion,
                            serverResponse.message,
                            TingToastType.ERROR
                        ).showToast(Toast.LENGTH_LONG) }
                    }
                } catch (e: Exception){
                    runOnUiThread { TingToast(
                        this@MenuPromotion,
                        "An Error Has Occurred",
                        TingToastType.ERROR
                    ).showToast(Toast.LENGTH_LONG) }
                }
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
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
        try { promotionTimer.cancel() } catch (e: java.lang.Exception) {}
        Bridge.clear(this)
    }

    override fun onPause() {
        super.onPause()
        try { promotionTimer.cancel() } catch (e: java.lang.Exception) {}
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        try { promotionTimer.cancel() } catch (e: java.lang.Exception) {}
    }

    override fun onStop() {
        super.onStop()
        try { promotionTimer.cancel() } catch (e: java.lang.Exception) {}
    }
}
