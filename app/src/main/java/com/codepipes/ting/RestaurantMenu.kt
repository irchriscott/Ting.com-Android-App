package com.codepipes.ting

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.codepipes.ting.adapters.menu.MenuFoodsAdapter
import com.codepipes.ting.adapters.menu.MenuReviewsAdapter
import com.codepipes.ting.adapters.promotion.PromotionRestaurantMenuAdapter
import com.codepipes.ting.customclasses.ActionSheet
import com.codepipes.ting.customclasses.XAxisValueFormatter
import com.codepipes.ting.dialogs.TingToast
import com.codepipes.ting.dialogs.TingToastType
import com.codepipes.ting.fragments.menu.MenuReviewsBottomSheetFragment
import com.codepipes.ting.fragments.restaurants.RestaurantsMapFragment
import com.codepipes.ting.imageviewer.StfalconImageViewer
import com.codepipes.ting.interfaces.ActionSheetCallBack
import com.codepipes.ting.models.*
import com.codepipes.ting.models.MenuPromotion
import com.codepipes.ting.models.RestaurantMenu
import com.codepipes.ting.providers.LocalData
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.ratings.BarLabels
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.UtilsFunctions
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.livefront.bridge.Bridge
import com.squareup.picasso.Picasso
import com.stepstone.apprating.AppRatingDialog
import com.stepstone.apprating.listener.RatingDialogListener
import kotlinx.android.synthetic.main.activity_restaurant_menu.*
import kotlinx.android.synthetic.main.include_empty_data.view.*
import okhttp3.*
import java.io.IOException
import java.text.NumberFormat
import java.util.*
import java.util.concurrent.TimeUnit

class RestaurantMenu : AppCompatActivity(), RatingDialogListener {

    private lateinit var menu: RestaurantMenu
    private lateinit var gson: Gson

    private lateinit var utilsFunctions: UtilsFunctions
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var localData: LocalData

    private lateinit var userAuthentication: UserAuthentication
    private lateinit var session: User

    private var selectedLatitude: Double = 0.0
    private var selectedLongitude: Double = 0.0

    @SuppressLint("PrivateResource", "MissingPermission", "SetTextI18n", "DefaultLocale")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurant_menu)

        Bridge.restoreInstanceState(this, savedInstanceState)
        savedInstanceState?.clear()

        gson = Gson()
        utilsFunctions = UtilsFunctions(this@RestaurantMenu)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this@RestaurantMenu)
        userAuthentication = UserAuthentication(this@RestaurantMenu)
        session = userAuthentication.get()!!

        localData = LocalData(this@RestaurantMenu)

        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.elevation = 0F
        supportActionBar!!.title = "Restaurant Menu".toUpperCase()

        try {
            val upArrow = ContextCompat.getDrawable(this@RestaurantMenu, R.drawable.abc_ic_ab_back_material)
            upArrow!!.setColorFilter(ContextCompat.getColor(this@RestaurantMenu, R.color.colorPrimary), PorterDuff.Mode.SRC_ATOP)
            supportActionBar!!.setHomeAsUpIndicator(upArrow)
        } catch (e: java.lang.Exception) {}

        val menuId = intent.getIntExtra("menu", 0)
        val localMenu = localData.getMenu(menuId)

        if(localMenu != null) {
            menu = localMenu
            shimmerLoader.stopShimmer()
            shimmerLoader.visibility = View.GONE
            menu_view.visibility = View.VISIBLE
            this.showRestaurantMenu(menu, false)
            this.getRestaurantMenu(intent.getStringExtra("url"))
        } else {
            shimmerLoader.startShimmer()
            shimmerLoader.visibility = View.VISIBLE
            menu_view.visibility = View.GONE
            this.getRestaurantMenu(intent.getStringExtra("url"))
        }

        refresh_menu.setColorSchemeColors(resources.getColor(R.color.colorPrimary), resources.getColor(R.color.colorAccentMain), resources.getColor(R.color.colorPrimaryDark), resources.getColor(R.color.colorAccentMain))
        refresh_menu.setOnRefreshListener {
            refresh_menu.isRefreshing = true
            this.getRestaurantMenu(intent.getStringExtra("url"))
        }
    }

    private fun getRestaurantMenu(url: String){

        val stringURL = "${Routes().HOST_END_POINT}$url"

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
                runOnUiThread {
                    refresh_menu.isRefreshing = false
                    TingToast(this@RestaurantMenu, e.message!!, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body()!!.string()
                val gson = Gson()
                try{
                    val restaurantMenu = gson.fromJson(responseBody, com.codepipes.ting.models.RestaurantMenu::class.java)
                    runOnUiThread {
                        menu = restaurantMenu
                        progress_loader.visibility = View.GONE
                        menu_view.visibility = View.VISIBLE
                        refresh_menu.isRefreshing = false
                        shimmerLoader.stopShimmer()
                        shimmerLoader.visibility = View.GONE
                        menu_view.visibility = View.VISIBLE
                        showRestaurantMenu(restaurantMenu, true)
                    }
                } catch (e: Exception){
                    runOnUiThread {
                        TingToast(this@RestaurantMenu, "An Error Has Occurred", TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                    }
                }
            }
        })
    }

    @SuppressLint("SetTextI18n", "MissingPermission", "DefaultLocale")
    private fun showRestaurantMenu(menu: com.codepipes.ting.models.RestaurantMenu, clickable: Boolean){

        val menuList = mutableListOf<String>()
        menuList.add("Current Location")
        session.addresses?.addresses!!.forEach {
            menuList.add("${it.type} - ${it.address}")
        }

        menu_restaurant_distance_view.setOnClickListener {
            val cx = (it.x + it.width / 2).toInt()
            val cy = (it.y + it.height).toInt()

            val mapFragment =  RestaurantsMapFragment()
            val args: Bundle = Bundle()

            args.putInt("cx", cx)
            args.putInt("cy", cy)
            args.putDouble("lat", selectedLatitude)
            args.putDouble("lng", selectedLongitude)
            args.putString("resto", Gson().toJson(menu.menu.branch))

            mapFragment.arguments = args
            mapFragment.show(supportFragmentManager, mapFragment.tag)
        }

        menu_restaurant_distance_view.setOnLongClickListener {
            val actionSheet = ActionSheet(this@RestaurantMenu, menuList)
                .setTitle("Restaurant Near Location")
                .setColorData(resources.getColor(R.color.colorGray))
                .setColorTitleCancel(resources.getColor(R.color.colorGoogleRedTwo))
                .setColorSelected(resources.getColor(R.color.colorPrimary))
                .setCancelTitle("Cancel")

            actionSheet.create(object : ActionSheetCallBack {

                override fun data(data: String, position: Int) {
                    if (menu.menu.branch != null) {
                        if (position == 0) {
                            if (utilsFunctions.checkLocationPermissions()) {
                                try {
                                    fusedLocationClient.lastLocation.addOnSuccessListener {
                                        if (it != null) {
                                            val from = LatLng(it.latitude, it.longitude)
                                            selectedLatitude = it.latitude
                                            selectedLongitude = it.longitude
                                            val to = LatLng(menu.menu.branch.latitude, menu.menu.branch.longitude)
                                            val dist = utilsFunctions.calculateDistance(from, to)
                                            menu.menu.branch.dist = dist
                                            menu.menu.branch.fromLocation = from
                                            runOnUiThread { menu_restaurant_distance.text = "${dist.toString()} Km" }
                                        } else {
                                            val from = LatLng(
                                                session.addresses!!.addresses[0].latitude,
                                                session.addresses!!.addresses[0].longitude
                                            )
                                            selectedLatitude = session.addresses!!.addresses[0].latitude
                                            selectedLongitude = session.addresses!!.addresses[0].longitude
                                            val to = LatLng(menu.menu.branch.latitude, menu.menu.branch.longitude)
                                            val dist = utilsFunctions.calculateDistance(from, to)
                                            menu.menu.branch.dist = dist
                                            menu.menu.branch.fromLocation = from
                                            runOnUiThread { menu_restaurant_distance.text = "${dist.toString()} Km" }
                                        }
                                    }.addOnFailureListener {
                                        val from = LatLng(
                                            session.addresses!!.addresses[0].latitude,
                                            session.addresses!!.addresses[0].longitude
                                        )
                                        selectedLatitude = session.addresses!!.addresses[0].latitude
                                        selectedLongitude = session.addresses!!.addresses[0].longitude
                                        val to = LatLng(menu.menu.branch.latitude, menu.menu.branch.longitude)
                                        val dist = utilsFunctions.calculateDistance(from, to)
                                        menu.menu.branch.dist = dist
                                        menu.menu.branch.fromLocation = from
                                        runOnUiThread { menu_restaurant_distance.text = "${dist.toString()} Km" }
                                        TingToast(this@RestaurantMenu, it.message!!, TingToastType.ERROR).showToast(
                                            Toast.LENGTH_LONG
                                        )
                                    }
                                } catch (e: java.lang.Exception) {
                                    TingToast(
                                        this@RestaurantMenu,
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
                            val to = LatLng(menu.menu.branch.latitude, menu.menu.branch.longitude)
                            val dist = utilsFunctions.calculateDistance(from, to)
                            menu.menu.branch.dist = dist
                            menu.menu.branch.fromLocation = from
                            runOnUiThread { menu_restaurant_distance.text = "${dist.toString()} Km" }
                        }
                    }
                }
            })

            return@setOnLongClickListener true
        }

        menu_restaurant_view.setOnClickListener {
            val intent = Intent(this@RestaurantMenu, RestaurantProfile::class.java)
            intent.putExtra("resto", menu.menu.branch?.id)
            startActivity(intent)
        }

        val index = (0 until menu.menu.images.count - 1).random()
        val image = menu.menu.images.images[index]
        Picasso.get().load("${Routes().HOST_END_POINT}${image.image}").into(menu_image)

        menu_image.setOnClickListener {
            StfalconImageViewer.Builder<MenuImage>(this@RestaurantMenu, menu.menu.images.images) { view, image ->
                Picasso.get().load("${Routes().HOST_END_POINT}${image.image}").into(view)
            }.show(true)
        }

        menu_name.text = menu.menu.name
        menu_rating.rating = menu.menu.reviews?.average!!.toFloat()
        menu_description.text = menu.menu.description

        if (menu.menu.showIngredients) {
            menu_ingredients.setHtml(menu.menu.ingredients)
        } else { menu_ingredients.text = "Not Available" }

        if (menu.type.id != 2) {
            menu_subcategory_name.text = menu.menu.category?.name
            menu_cuisine_name.text = menu.menu.cuisine?.name
            Picasso.get().load("${Routes().HOST_END_POINT}${menu.menu.category?.image}")
                .into(menu_subcategory_image)
            Picasso.get().load("${Routes().HOST_END_POINT}${menu.menu.cuisine?.image}")
                .into(menu_cuisine_image)
        } else {
            menu_subcategory.visibility = View.GONE
            menu_cuisine_view.visibility = View.GONE
        }

        menu_type_name.text = menu.type.name.capitalize()
        menu_new_price.text =
            "${menu.menu.currency} ${NumberFormat.getNumberInstance().format(menu.menu.price)}".toUpperCase()
        if (menu.menu.price != menu.menu.lastPrice) {
            menu_old_price.text =
                "${menu.menu.currency} ${NumberFormat.getNumberInstance().format(menu.menu.lastPrice)}".toUpperCase()
            menu_old_price.paintFlags = menu_old_price.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else { menu_old_price.visibility = View.GONE }


        when (menu.type.id) {
            1 -> {
                menu_type_image.setImageDrawable(resources.getDrawable(R.drawable.ic_spoon_gray))
                menu_category_name.text = menu.menu.foodType
                if (menu.menu.isCountable) {
                    menu_quantity.text = "${menu.menu.quantity} pieces / packs"
                } else { menu_quantity.visibility = View.GONE }
            }
            2 -> {
                menu_type_image.setImageDrawable(resources.getDrawable(R.drawable.ic_glass_gray))
                menu_category_name.text = menu.menu.drinkType
                if (menu.menu.isCountable) {
                    menu_quantity.text = "${menu.menu.quantity} cups / bottles"
                } else { menu_quantity.visibility = View.GONE }
            }
            3 -> {
                menu_type_image.setImageDrawable(resources.getDrawable(R.drawable.ic_fork_knife_gray))
                menu_category_image.setImageDrawable(resources.getDrawable(R.drawable.ic_clock_gray_24dp))
                menu_category_name.text = menu.menu.dishTime
                if (menu.menu.isCountable) {
                    menu_quantity.text = "${menu.menu.quantity} plates / packs"
                } else { menu_quantity.visibility = View.GONE }
            }
        }

        menu_likes.text = NumberFormat.getNumberInstance().format(menu.menu.likes?.count)
        menu_reviews.text = NumberFormat.getNumberInstance().format(menu.menu.reviews.count)

        if (menu.menu.isAvailable) {
            menu_availability_text.text = "Available"
            menu_availability_view.background = resources.getDrawable(R.drawable.background_time_green)
            menu_availability_icon.setImageDrawable(resources.getDrawable(R.drawable.ic_check_white_48dp))
        } else {
            menu_availability_text.text = "Not Available"
            menu_availability_view.background = resources.getDrawable(R.drawable.background_time_red)
            menu_availability_icon.setImageDrawable(resources.getDrawable(R.drawable.ic_close_white_24dp))
        }

        if (menu.menu.branch != null && menu.menu.restaurant != null) {

            Picasso.get().load(menu.menu.restaurant.logoURL()).into(menu_restaurant_image)
            menu_restaurant_name.text = "${menu.menu.restaurant.name}, ${menu.menu.branch.name}"

            menu_restaurant_name.isClickable = true
            menu_restaurant_name.setOnClickListener {
                if(clickable){
                    val intent = Intent(this@RestaurantMenu, RestaurantProfile::class.java)
                    intent.putExtra("resto", menu.menu.branch.id)
                    intent.putExtra("tab", 0)
                    startActivity(intent)
                }
            }

            if (menu.menu.branch.isAvailable) {

                val status =
                    utilsFunctions.statusWorkTime(menu.menu.restaurant.opening, menu.menu.restaurant.closing)
                menu_restaurant_time.text = status?.get("msg")

                when (status?.get("clr")) {
                    "green" -> {
                        menu_restaurant_work_status.background =
                            resources.getDrawable(R.drawable.background_time_green)
                    }
                    "orange" -> {
                        menu_restaurant_work_status.background =
                            resources.getDrawable(R.drawable.background_time_orange)
                    }
                    "red" -> {
                        menu_restaurant_work_status.background =
                            resources.getDrawable(R.drawable.background_time_red)
                    }
                }

                Timer().scheduleAtFixedRate(object : TimerTask() {
                    override fun run() {
                        runOnUiThread {

                            val statusTimer = utilsFunctions.statusWorkTime(
                                menu.menu.restaurant.opening,
                                menu.menu.restaurant.closing
                            )
                            menu_restaurant_time.text = statusTimer?.get("msg")

                            when (statusTimer?.get("clr")) {
                                "green" -> {
                                    menu_restaurant_work_status.background =
                                        resources.getDrawable(R.drawable.background_time_green)
                                }
                                "orange" -> {
                                    menu_restaurant_work_status.background =
                                        resources.getDrawable(R.drawable.background_time_orange)
                                }
                                "red" -> {
                                    menu_restaurant_work_status.background =
                                        resources.getDrawable(R.drawable.background_time_red)
                                }
                            }
                        }
                    }
                }, 0, 10000)
            } else {
                menu_restaurant_work_status.background = resources.getDrawable(R.drawable.background_time_red)
                menu_restaurant_work_status_icon.setImageDrawable(resources.getDrawable(R.drawable.ic_close_white_24dp))
                menu_restaurant_time.text = "Not Available"
            }

            if (utilsFunctions.checkLocationPermissions()) {
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener {
                        if (it != null) {
                            val from = LatLng(it.latitude, it.longitude)
                            selectedLatitude = it.latitude
                            selectedLongitude = it.longitude
                            val to = LatLng(menu.menu.branch.latitude, menu.menu.branch.longitude)
                            val dist = utilsFunctions.calculateDistance(from, to)
                            menu.menu.branch.dist = dist
                            menu.menu.branch.fromLocation = from
                            runOnUiThread { menu_restaurant_distance.text = "${dist.toString()} Km" }
                        } else {
                            val from = LatLng(
                                session.addresses!!.addresses[0].latitude,
                                session.addresses!!.addresses[0].longitude
                            )
                            selectedLatitude = session.addresses!!.addresses[0].latitude
                            selectedLongitude = session.addresses!!.addresses[0].longitude
                            val to = LatLng(menu.menu.branch.latitude, menu.menu.branch.longitude)
                            val dist = utilsFunctions.calculateDistance(from, to)
                            menu.menu.branch.dist = dist
                            menu.menu.branch.fromLocation = from
                            runOnUiThread { menu_restaurant_distance.text = "${dist.toString()} Km" }
                        }
                    }.addOnFailureListener {
                        val from = LatLng(
                            session.addresses!!.addresses[0].latitude,
                            session.addresses!!.addresses[0].longitude
                        )
                        selectedLatitude = session.addresses!!.addresses[0].latitude
                        selectedLongitude = session.addresses!!.addresses[0].longitude
                        val to = LatLng(menu.menu.branch.latitude, menu.menu.branch.longitude)
                        val dist = utilsFunctions.calculateDistance(from, to)
                        menu.menu.branch.dist = dist
                        menu.menu.branch.fromLocation = from
                        runOnUiThread { menu_restaurant_distance.text = "${dist.toString()} Km" }
                        TingToast(
                            this@RestaurantMenu,
                            it.message!!,
                            TingToastType.ERROR
                        ).showToast(Toast.LENGTH_LONG)
                    }
                } catch (e: java.lang.Exception) {
                    TingToast(
                        this@RestaurantMenu,
                        e.message!!.capitalize(),
                        TingToastType.ERROR
                    ).showToast(Toast.LENGTH_LONG)
                }
            }
        } else {
            menu_restaurant_name.isClickable = false
            menu_restaurant_name.setOnClickListener(null)
            menu_restaurant_name.text = "Loading..."
            menu_restaurant_distance.text = "0.0 Km"
            menu_restaurant_time.text = "Loading..."
        }

        if (menu.type.id == 3 ){
            if(menu.menu.foods?.count!! > 0) {
                val dishFoods = menu.menu.foods.foods as MutableList<DishFood>
                if(menu.menu.hasDrink!!){
                    val drinkFood = DishFood(id = menu.menu.drink?.id!! + 10, food = menu.menu.drink, quantity = 1, isCountable = false, createdAt = "Today", updatedAt = "Today")
                    dishFoods.add(drinkFood)
                }
                menu_dish_foods_recycler_view.layoutManager = LinearLayoutManager(this@RestaurantMenu)
                menu_dish_foods_recycler_view.adapter = MenuFoodsAdapter(dishFoods, supportFragmentManager)
            } else { menu_dish_foods_view.visibility = View.GONE }
        } else { menu_dish_foods_view.visibility = View.GONE }

        if (menu.menu.promotions?.count!! > 0) {
            val promotions = menu.menu.promotions.promotions!!.filter { return@filter it.isOn && it.isOnToday }
            if (promotions.isNotEmpty()){
                menu_promotions_recycler_view.layoutManager = LinearLayoutManager(this@RestaurantMenu)
                menu_promotions_recycler_view.adapter = PromotionRestaurantMenuAdapter(promotions as MutableList<MenuPromotion>)
            } else { menu_promotions_view.visibility = View.GONE }
        } else { menu_promotions_view.visibility = View.GONE }

        if(menu.menu.likes?.count!! > 0){
            if(utilsFunctions.likesMenu(menu.menu.likes.likes!!, session)){
                menu_like_button.tag = 1
                menu_like_button.playAnimation()
                menu_like_button.isChecked = true
            } else {
                menu_like_button.tag = 0
                menu_like_button.isChecked = false
            }
        } else {
            menu_like_button.tag = 0
            menu_like_button.isChecked = false
        }

        menu_like_button.setOnClickListener { this.likeMenuToggle("${Routes().HOST_END_POINT}${menu.urls.apiLike}", session.token!!, menu.id) }

        menu_reviews_average.text = menu.menu.reviews.average.toString()

        val ratingChart = findViewById<HorizontalBarChart>(R.id.menu_rating_percents)
        ratingChart.setDrawBarShadow(false)
        val description = Description()
        description.text = ""
        ratingChart.description = description
        ratingChart.legend.isEnabled = false
        ratingChart.setPinchZoom(false)
        ratingChart.setDrawValueAboveBar(false)
        ratingChart.axisLeft.textColor = resources.getColor(R.color.colorGray)
        ratingChart.xAxis.textColor = resources.getColor(R.color.colorGray)

        val xAxis = ratingChart.xAxis
        xAxis.setDrawGridLines(false)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.isEnabled = true
        xAxis.setDrawAxisLine(false)


        val yLeft = ratingChart.axisLeft

        yLeft.axisMaximum = 100f
        yLeft.axisMinimum = 0f
        yLeft.isEnabled = false

        xAxis.labelCount = menu.menu.reviews.percents.size

        val values = arrayOf("1 ★", "2 ★", "3 ★", "4 ★", "5 ★")
        xAxis.valueFormatter = XAxisValueFormatter(values)

        val yRight = ratingChart.axisRight
        yRight.setDrawAxisLine(true)
        yRight.setDrawGridLines(false)
        yRight.isEnabled = false

        val entries = ArrayList<BarEntry>()
        entries.add(BarEntry(0f, menu.menu.reviews.percents[0].toFloat()))
        entries.add(BarEntry(1f, menu.menu.reviews.percents[1].toFloat()))
        entries.add(BarEntry(2f, menu.menu.reviews.percents[2].toFloat()))
        entries.add(BarEntry(3f, menu.menu.reviews.percents[3].toFloat()))
        entries.add(BarEntry(4f, menu.menu.reviews.percents[4].toFloat()))

        val barDataSet = BarDataSet(entries, "Bar Data Set")
        barDataSet.setColors(
            ContextCompat.getColor(ratingChart.context, R.color.colorGray),
            ContextCompat.getColor(ratingChart.context, R.color.colorGray),
            ContextCompat.getColor(ratingChart.context, R.color.colorGray),
            ContextCompat.getColor(ratingChart.context, R.color.colorGray),
            ContextCompat.getColor(ratingChart.context, R.color.colorGray)
        )

        barDataSet.setDrawValues(false)

        ratingChart.animateY(1000)
        ratingChart.setDrawBarShadow(true)
        barDataSet.barShadowColor = Color.argb(40, 150, 150, 150)

        val data = BarData(barDataSet)
        data.barWidth = 0.9f
        ratingChart.data = data
        ratingChart.invalidate()

        if(menu.menu.reviews.count > 0){
            empty_data.visibility = View.GONE
            menu_reviews_recycler_view.visibility = View.VISIBLE

            val max = if(menu.menu.reviews.count > 6){ 5 } else { menu.menu.reviews.count }

            val reviews = menu.menu.reviews.reviews!!.subList(0, max)
            menu_reviews_recycler_view.layoutManager = LinearLayoutManager(this@RestaurantMenu)
            menu_reviews_recycler_view.adapter = MenuReviewsAdapter(reviews as MutableList<MenuReview>)

            if (menu.menu.reviews.count > 6){
                menu_reviews_show_more.visibility = View.VISIBLE
                menu_reviews_show_more.setOnClickListener {
                    val reviewsFragment = MenuReviewsBottomSheetFragment()
                    val bundle = Bundle()
                    bundle.putString("menu", gson.toJson(menu))
                    reviewsFragment.arguments = bundle
                    reviewsFragment.show(supportFragmentManager, reviewsFragment.tag)
                }
            } else { menu_reviews_show_more.visibility = View.GONE }
        } else {
            empty_data.visibility = View.VISIBLE
            empty_data.empty_text.text = "No Reviews For This Menu"
            empty_data.empty_image.setImageDrawable(resources.getDrawable(R.drawable.ic_comments_gray))
            menu_reviews_recycler_view.visibility = View.GONE
            menu_reviews_show_more.visibility = View.GONE
        }
    }

    private fun likeMenuToggle(url: String, token: String, menu: Int){

        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("menu", menu.toString())
            .build()

        val request = Request.Builder()
            .header("Authorization", token)
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    TingToast(this@RestaurantMenu, e.message!!, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body()!!.string()
                try{
                    val serverResponse = gson.fromJson(responseBody, ServerResponse::class.java)
                    runOnUiThread {
                        if (serverResponse.status == 200){
                            if(serverResponse.message.contains("Disliked") || menu_like_button.tag == 1){
                                menu_like_button.tag = 0
                                menu_like_button.isChecked = false
                            } else {
                                menu_like_button.tag = 1
                                menu_like_button.playAnimation()
                                menu_like_button.isChecked = true
                            }
                            TingToast(this@RestaurantMenu, serverResponse.message, TingToastType.SUCCESS).showToast(Toast.LENGTH_LONG)
                        } else { TingToast(this@RestaurantMenu, serverResponse.message, TingToastType.ERROR).showToast(Toast.LENGTH_LONG) }
                    }
                } catch (e: Exception){
                    runOnUiThread { TingToast(this@RestaurantMenu, "An Error Has Occurred", TingToastType.ERROR).showToast(Toast.LENGTH_LONG) }
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.restaurant_menu_review, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.menu_rate -> {

                val menuReview = utilsFunctions.userMenuReview(menu.menu.reviews?.reviews!!, session)

                val ratingDialog = AppRatingDialog.Builder()
                    .setPositiveButtonText("Submit")
                    .setNegativeButtonText("Cancel")
                    .setNeutralButtonText("Later")
                    .setNoteDescriptions(listOf("Very Bad", "Not good", "Quite Ok", "Very Good", "Excellent !!!"))
                    .setDefaultRating(menuReview?.review ?: 1)
                    .setTitle("Rate this Menu")
                    .setDescription("Please select a rate and write a review")
                    .setCommentInputEnabled(true)
                    .setStarColor(R.color.colorYellowRate)
                    .setNoteDescriptionTextColor(R.color.colorGray)
                    .setTitleTextColor(R.color.colorGray)
                    .setDescriptionTextColor(R.color.colorLightGray)
                    .setHint("Please write your review here ...")
                    .setHintTextColor(R.color.colorGray)
                    .setCommentTextColor(R.color.colorGray)
                    .setCommentBackgroundColor(R.color.colorPrimaryDarkTransparentDark)
                    .setWindowAnimation(R.style.RateDialogFadeAnimation)
                    .setCancelable(false)
                    .setCanceledOnTouchOutside(false)

                if(menuReview?.comment != null){ ratingDialog.setDefaultComment(menuReview.comment) }

                ratingDialog.create(this).show()

                return true
            }
        }
        return false
    }

    override fun onPositiveButtonClicked(rate: Int, comment: String) {

        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("review", rate.toString())
            .addFormDataPart("comment", comment)
            .build()

        val request = Request.Builder()
            .header("Authorization", session.token!!)
            .url("${Routes().HOST_END_POINT}${menu.urls.apiAddReview}")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    TingToast(this@RestaurantMenu, e.message!!, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body()!!.string()
                try{
                    val serverResponse = gson.fromJson(responseBody, ServerResponse::class.java)
                    runOnUiThread {
                        getRestaurantMenu(menu.urls.apiGet)
                        if (serverResponse.status == 200){
                            TingToast(this@RestaurantMenu, serverResponse.message, TingToastType.SUCCESS).showToast(Toast.LENGTH_LONG)
                        } else { TingToast(this@RestaurantMenu, serverResponse.message, TingToastType.ERROR).showToast(Toast.LENGTH_LONG) }
                    }
                } catch (e: Exception){
                    runOnUiThread { TingToast(this@RestaurantMenu, "An Error Has Occurred", TingToastType.ERROR).showToast(Toast.LENGTH_LONG) }
                }
            }
        })
    }

    override fun onNegativeButtonClicked() {}

    override fun onNeutralButtonClicked() {}

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
        Bridge.clear(this)
    }
}
