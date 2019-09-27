package com.codepipes.ting

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Paint
import android.graphics.PorterDuff
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.Toast
import com.codepipes.ting.adapters.menu.MenuFoodsAdapter
import com.codepipes.ting.adapters.promotion.PromotionRestaurantMenuAdapter
import com.codepipes.ting.customclasses.ActionSheet
import com.codepipes.ting.dialogs.TingToast
import com.codepipes.ting.dialogs.TingToastType
import com.codepipes.ting.fragments.restaurants.RestaurantsMapFragment
import com.codepipes.ting.interfaces.ActionSheetCallBack
import com.codepipes.ting.models.*
import com.codepipes.ting.models.RestaurantMenu
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.UtilsFunctions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_restaurant_menu.*
import okhttp3.*
import okhttp3.Route
import java.io.IOException
import java.text.NumberFormat
import java.util.*
import java.util.concurrent.TimeUnit

class RestaurantMenu : AppCompatActivity() {

    private lateinit var menu: RestaurantMenu
    private lateinit var gson: Gson
    private lateinit var utilsFunctions: UtilsFunctions
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var userAuthentication: UserAuthentication
    private lateinit var session: User

    private var selectedLatitude: Double = 0.0
    private var selectedLongitude: Double = 0.0

    @SuppressLint("PrivateResource", "MissingPermission", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurant_menu)

        gson = Gson()
        utilsFunctions = UtilsFunctions(this@RestaurantMenu)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this@RestaurantMenu)
        userAuthentication = UserAuthentication(this@RestaurantMenu)
        session = userAuthentication.get()!!

        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.elevation = 0F
        supportActionBar!!.title = "Restaurant Menu".toUpperCase()

        val upArrow = ContextCompat.getDrawable(this@RestaurantMenu, R.drawable.abc_ic_ab_back_material)
        upArrow!!.setColorFilter(ContextCompat.getColor(this@RestaurantMenu, R.color.colorPrimary), PorterDuff.Mode.SRC_ATOP)
        supportActionBar!!.setHomeAsUpIndicator(upArrow)

        menu = gson.fromJson(intent.getStringExtra("menu"), com.codepipes.ting.models.RestaurantMenu::class.java)
        this.showRestaurantMenu(menu)
        this.getRestaurantMenu(menu.urls.apiGet)

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
                    if(position == 0){
                        if(utilsFunctions.checkLocationPermissions()){
                            try {
                                fusedLocationClient.lastLocation.addOnSuccessListener {
                                    if(it != null){
                                        val from = LatLng(it.latitude, it.longitude)
                                        selectedLatitude = it.latitude
                                        selectedLongitude = it.longitude
                                        val to = LatLng(menu.menu.branch?.latitude!!, menu.menu.branch?.longitude!!)
                                        val dist = utilsFunctions.calculateDistance(from, to)
                                        menu.menu.branch?.dist = dist
                                        menu.menu.branch?.fromLocation = from
                                        runOnUiThread { menu_restaurant_distance.text = "${dist.toString()} Km" }
                                    } else {
                                        val from = LatLng(session.addresses!!.addresses[0].latitude, session.addresses!!.addresses[0].longitude)
                                        selectedLatitude = session.addresses!!.addresses[0].latitude
                                        selectedLongitude = session.addresses!!.addresses[0].longitude
                                        val to = LatLng(menu.menu.branch?.latitude!!, menu.menu.branch?.longitude!!)
                                        val dist = utilsFunctions.calculateDistance(from, to)
                                        menu.menu.branch?.dist = dist
                                        menu.menu.branch?.fromLocation = from
                                        runOnUiThread { menu_restaurant_distance.text = "${dist.toString()} Km" }
                                    }
                                }.addOnFailureListener {
                                    val from = LatLng(session.addresses!!.addresses[0].latitude, session.addresses!!.addresses[0].longitude)
                                    selectedLatitude = session.addresses!!.addresses[0].latitude
                                    selectedLongitude = session.addresses!!.addresses[0].longitude
                                    val to = LatLng(menu.menu.branch?.latitude!!, menu.menu.branch?.longitude!!)
                                    val dist = utilsFunctions.calculateDistance(from, to)
                                    menu.menu.branch?.dist = dist
                                    menu.menu.branch?.fromLocation = from
                                    runOnUiThread { menu_restaurant_distance.text = "${dist.toString()} Km" }
                                    TingToast(this@RestaurantMenu, it.message!!, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                                }
                            } catch (e: java.lang.Exception){ TingToast(this@RestaurantMenu, e.message!!.capitalize(), TingToastType.ERROR).showToast(Toast.LENGTH_LONG) }
                        }
                    } else {
                        val address = session.addresses?.addresses!![position - 1]
                        val from = LatLng(address.latitude, address.longitude)
                        selectedLatitude = address.latitude
                        selectedLongitude = address.longitude
                        val to = LatLng(menu.menu.branch?.latitude!!, menu.menu.branch?.longitude!!)
                        val dist = utilsFunctions.calculateDistance(from, to)
                        menu.menu.branch?.dist = dist
                        menu.menu.branch?.fromLocation = from
                        runOnUiThread { menu_restaurant_distance.text = "${dist.toString()} Km" }
                    }
                }
            })

            return@setOnLongClickListener true
        }

        menu_restaurant_view.setOnClickListener {
            val intent = Intent(this@RestaurantMenu, RestaurantProfile::class.java)
            intent.putExtra("resto", Gson().toJson(menu.menu.branch))
            startActivity(intent)
        }

        refresh_menu.setColorSchemeColors(resources.getColor(R.color.colorPrimary), resources.getColor(R.color.colorAccentMain), resources.getColor(R.color.colorPrimaryDark), resources.getColor(R.color.colorAccentMain))
        refresh_menu.setOnRefreshListener {
            refresh_menu.isRefreshing = true
            this.getRestaurantMenu(menu.urls.apiGet)
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
                    val restaurantMenu = gson.fromJson(responseBody, RestaurantMenu::class.java)
                    runOnUiThread {
                        menu = restaurantMenu
                        progress_loader.visibility = View.GONE
                        menu_view.visibility = View.VISIBLE
                        refresh_menu.isRefreshing = false
                        showRestaurantMenu(restaurantMenu)
                    }
                } catch (e: Exception){
                    runOnUiThread {
                        TingToast(this@RestaurantMenu, "An Error Has Occurred", TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                    }
                }
            }
        })
    }

    @SuppressLint("SetTextI18n", "MissingPermission")
    private fun showRestaurantMenu(menu: com.codepipes.ting.models.RestaurantMenu){

        runOnUiThread {
            val index = (0 until menu.menu.images.count - 1).random()
            val image = menu.menu.images.images[index]
            Picasso.get().load("${Routes().HOST_END_POINT}${image.image}").into(menu_image)

            menu_name.text = menu.menu.name
            menu_rating.rating = menu.menu.reviews?.average!!.toFloat()
            menu_description.text = menu.menu.description

            if (menu.menu.showIngredients) {
                menu_ingredients.setHtml(menu.menu.ingredients)
            } else {
                menu_ingredients.text = "Not Available"
            }

            if (menu.type.id != 2) {
                menu_subcategory_name.text = menu.menu.category?.name
                Picasso.get().load("${Routes().HOST_END_POINT}${menu.menu.category?.image}")
                    .into(menu_subcategory_image)
            } else {
                menu_subcategory.visibility = View.GONE
            }

            menu_type_name.text = menu.type.name.capitalize()
            menu_new_price.text =
                "${menu.menu.currency} ${NumberFormat.getNumberInstance().format(menu.menu.price)}".toUpperCase()
            if (menu.menu.price != menu.menu.lastPrice) {
                menu_old_price.text =
                    "${menu.menu.currency} ${NumberFormat.getNumberInstance().format(menu.menu.lastPrice)}".toUpperCase()
                menu_old_price.paintFlags = menu_old_price.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                menu_old_price.visibility = View.GONE
            }


            when (menu.type.id) {
                1 -> {
                    menu_type_image.setImageDrawable(resources.getDrawable(R.drawable.ic_spoon_gray))
                    menu_category_name.text = menu.menu.foodType
                    if (menu.menu.isCountable) {
                        menu_quantity.text = "${menu.menu.quantity} pieces / packs"
                    } else {
                        menu_quantity.visibility = View.GONE
                    }
                }
                2 -> {
                    menu_type_image.setImageDrawable(resources.getDrawable(R.drawable.ic_glass_gray))
                    menu_category_name.text = menu.menu.drinkType
                    if (menu.menu.isCountable) {
                        menu_quantity.text = "${menu.menu.quantity} cups / bottles"
                    } else {
                        menu_quantity.visibility = View.GONE
                    }
                }
                3 -> {
                    menu_type_image.setImageDrawable(resources.getDrawable(R.drawable.ic_fork_knife_gray))
                    menu_category_image.setImageDrawable(resources.getDrawable(R.drawable.ic_clock_gray_24dp))
                    menu_category_name.text = menu.menu.dishTime
                    if (menu.menu.isCountable) {
                        menu_quantity.text = "${menu.menu.quantity} plates / packs"
                    } else {
                        menu_quantity.visibility = View.GONE
                    }
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
                    menu_like_button.setImageDrawable(resources.getDrawable(R.drawable.ic_like_filled_gray))
                } else {
                    menu_like_button.tag = 0
                    menu_like_button.setImageDrawable(resources.getDrawable(R.drawable.ic_like_outlined_gray))
                }
            } else {
                menu_like_button.tag = 0
                menu_like_button.setImageDrawable(resources.getDrawable(R.drawable.ic_like_outlined_gray))
            }

            menu_like_button.setOnClickListener { this.likeMenuToggle("${Routes().HOST_END_POINT}${menu.urls.apiLike}", session.token!!, menu.id) }
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
                                menu_like_button.setImageDrawable(resources.getDrawable(R.drawable.ic_like_outlined_gray))
                            } else {
                                menu_like_button.tag = 1
                                menu_like_button.setImageDrawable(resources.getDrawable(R.drawable.ic_like_filled_gray))
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
}
