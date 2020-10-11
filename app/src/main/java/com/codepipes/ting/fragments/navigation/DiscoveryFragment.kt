package com.codepipes.ting.fragments.navigation


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.codepipes.ting.*
import com.codepipes.ting.activities.discovery.TodayPromotions
import com.codepipes.ting.activities.placement.CurrentRestaurant
import com.codepipes.ting.activities.placement.RestaurantScanner
import com.codepipes.ting.activities.restaurant.RestaurantProfile

import com.codepipes.ting.adapters.cuisine.CuisineMenusAdapter
import com.codepipes.ting.adapters.cuisine.CuisineRestaurantsAdapter
import com.codepipes.ting.adapters.cuisine.CuisinesAdapter
import com.codepipes.ting.carouselview.enums.IndicatorAnimationType
import com.codepipes.ting.carouselview.enums.OffsetType
import com.codepipes.ting.dialogs.messages.TingToast
import com.codepipes.ting.dialogs.messages.TingToastType
import com.codepipes.ting.models.*
import com.codepipes.ting.models.MenuPromotion
import com.codepipes.ting.models.RestaurantMenu
import com.codepipes.ting.providers.LocalData
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.providers.UserPlacement
import com.codepipes.ting.sliderview.interfaces.ViewListener
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.UtilsFunctions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.livefront.bridge.Bridge
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_discovery.*
import kotlinx.android.synthetic.main.fragment_discovery.view.*
import kotlinx.android.synthetic.main.row_discover_menu.view.*
import kotlinx.android.synthetic.main.row_discover_promotion.view.*
import kotlinx.android.synthetic.main.row_discover_restaurant.view.*
import kotlinx.android.synthetic.main.row_discover_restaurant.view.restaurant_address
import okhttp3.*
import java.io.IOException
import java.lang.Exception
import java.text.NumberFormat
import java.util.*
import java.util.concurrent.TimeUnit


class DiscoveryFragment : Fragment() {

    private lateinit var session: User
    private lateinit var userAuthentication: UserAuthentication
    private lateinit var mLocalData: LocalData

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mUtilFunctions: UtilsFunctions

    private lateinit var gson: Gson

    private lateinit var restaurantsTimer: Timer
    private lateinit var cuisinesTimer: Timer
    private lateinit var promotionsTimer: Timer
    private lateinit var discoverMenusTimer: Timer
    private lateinit var topRestaurantTimer: Timer
    private lateinit var topMenusTimer: Timer

    private lateinit var restaurantsTimerTask: RestaurantsTimerTask
    private lateinit var cuisinesTimerTask: CuisinesTimerTask
    private lateinit var promotionsTimerTask: PromotionsTimerTask
    private lateinit var discoverMenusTimerTask: DiscoverMenusTimerTask
    private lateinit var topRestaurantsTimerTask: TopRestaurantsTimerTask
    private lateinit var topMenusTimerTask: TopMenusTimerTask

    private var country: String = ""
    private var town: String = ""

    private lateinit var statusWorkTimer: Timer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Bridge.restoreInstanceState(this, savedInstanceState)
        savedInstanceState?.clear()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_discovery, container, false)

        userAuthentication = UserAuthentication(context!!)
        session = userAuthentication.get()!!
        mLocalData = LocalData(context!!)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity!!)
        mUtilFunctions = UtilsFunctions(context!!)

        statusWorkTimer = Timer()

        country = session.country
        town = session.town

        if(ContextCompat.checkSelfPermission(context!!, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener {
                    if(it != null){
                        try {
                            val geocoder = Geocoder(activity, Locale.getDefault())
                            activity!!.runOnUiThread {
                                try {
                                    val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                                    country = addresses[0].countryName
                                    town = addresses[0].locality
                                } catch (e: Exception) {}
                            }
                        } catch (e: Exception) {}
                    }
                }
            } catch (e: Exception){ }
        } else {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                UtilsFunctions.REQUEST_FINE_LOCATION
            )
        }

        gson = Gson()

        restaurantsTimer = Timer()
        cuisinesTimer = Timer()
        promotionsTimer = Timer()
        discoverMenusTimer = Timer()
        topRestaurantTimer = Timer()
        topMenusTimer = Timer()

        restaurantsTimerTask = RestaurantsTimerTask()
        cuisinesTimerTask = CuisinesTimerTask()
        promotionsTimerTask = PromotionsTimerTask()
        discoverMenusTimerTask = DiscoverMenusTimerTask()
        topRestaurantsTimerTask = TopRestaurantsTimerTask()
        topMenusTimerTask = TopMenusTimerTask()

        view.discover_restaurants_shimmer.startShimmer()
        restaurantsTimer.scheduleAtFixedRate(restaurantsTimerTask, TIMER_PERIOD, TIMER_PERIOD)
        this.getDiscoverRestaurants()

        view.cuisines_shimmer.startShimmer()
        val cuisines = mLocalData.getCuisines()

        if(!cuisines.isNullOrEmpty()) {
            view.cuisines_recycler_view.visibility = View.VISIBLE
            view.cuisines_shimmer.stopShimmer()
            view.cuisines_shimmer.visibility = View.GONE
            val layoutManager = LinearLayoutManager(context)
            layoutManager.orientation = LinearLayoutManager.HORIZONTAL
            view.cuisines_recycler_view.layoutManager = layoutManager
            view.cuisines_recycler_view.adapter =
                CuisinesAdapter(cuisines.shuffled().toMutableList())
        } else {
            cuisinesTimer.scheduleAtFixedRate(cuisinesTimerTask, TIMER_PERIOD, TIMER_PERIOD)
            this.getCuisines()
        }

        view.promotions_shimmer.startShimmer()
        promotionsTimer.scheduleAtFixedRate(promotionsTimerTask, TIMER_PERIOD, TIMER_PERIOD)
        this.getDiscoverTodayPromotions()

        view.discover_menus_shimmer.startShimmer()
        discoverMenusTimer.scheduleAtFixedRate(discoverMenusTimerTask, TIMER_PERIOD, TIMER_PERIOD)
        this.getDiscoverMenus()

        view.top_restaurants_shimmer.startShimmer()
        topRestaurantTimer.scheduleAtFixedRate(topRestaurantsTimerTask, TIMER_PERIOD, TIMER_PERIOD)
        this.getDiscoverTopRestaurants()

        view.top_menus_shimmer.startShimmer()
        topMenusTimer.scheduleAtFixedRate(topMenusTimerTask, TIMER_PERIOD, TIMER_PERIOD)
        this.getDiscoverTopMenus()

        view.refresh_discovery.setColorSchemeColors(resources.getColor(R.color.colorPrimary), resources.getColor(R.color.colorAccentMain), resources.getColor(R.color.colorPrimaryDark), resources.getColor(R.color.colorAccentMain))
        view.refresh_discovery.setOnRefreshListener {

            statusWorkTimer = Timer()
            view.refresh_discovery.isRefreshing = true

            view.discover_recommended_restaurants_view.visibility = View.VISIBLE
            this.getDiscoverRestaurants()

            view.discover_promotions_view.visibility = View.VISIBLE
            this.getDiscoverTodayPromotions()

            view.discover_menus_view.visibility = View.VISIBLE
            this.getDiscoverMenus()

            view.top_restaurants_view.visibility = View.VISIBLE
            this.getDiscoverTopRestaurants()

            view.top_menus_view.visibility = View.VISIBLE
            this.getDiscoverTopMenus()
        }

        view.discover_recommended_restaurants_view.setOnLongClickListener {
            try {
                restaurantsTimer.cancel()
                restaurantsTimerTask.cancel()
            } catch (e: Exception){ }
            getDiscoverRestaurants()
            return@setOnLongClickListener true
        }

        view.discover_promotions_view.setOnLongClickListener {
            try {
                promotionsTimer.cancel()
                promotionsTimerTask.cancel()
            } catch (e: Exception){ }
            getDiscoverTodayPromotions()
            return@setOnLongClickListener true
        }

        view.discover_menus_view.setOnLongClickListener {
            try {
                discoverMenusTimer.cancel()
                discoverMenusTimerTask.cancel()
            } catch (e: Exception) { }
            getDiscoverMenus()
            return@setOnLongClickListener true
        }

        view.top_restaurants_view.setOnLongClickListener {
            try {
                topRestaurantTimer.cancel()
                topRestaurantsTimerTask.cancel()
            } catch (e: Exception) { }
            getDiscoverTopRestaurants()
            return@setOnLongClickListener true
        }

        view.top_menus_view.setOnLongClickListener {
            try {
                topMenusTimer.cancel()
                topMenusTimerTask.cancel()
            } catch (e: Exception) {}
            getDiscoverTopMenus()
            return@setOnLongClickListener true
        }

        val userPlacement = UserPlacement(context!!)

        view.scan_table.setOnClickListener {
            if(userPlacement.isPlacedIn()){
                activity?.startActivity(Intent(activity, CurrentRestaurant::class.java))
            } else { activity?.startActivity(Intent(activity, RestaurantScanner::class.java)) }
        }

        return view
    }

    inner class LocationInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val url = chain.request().url.newBuilder()
                .addQueryParameter("country", country)
                .addQueryParameter("town", town)
                .build()
            val request = chain.request().newBuilder()
                .header("Authorization", session.token!!)
                .url(url)
                .build()
            return chain.proceed(request)
        }
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun getDiscoverRestaurants() {
        val url = Routes.discoverRestaurants
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60 * 5, TimeUnit.SECONDS)
            .addInterceptor(LocationInterceptor())
            .build()

        val request = Request.Builder().header("Authorization", session.token!!).url(url).get().build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                activity!!.runOnUiThread {}
            }

            @SuppressLint("MissingPermission")
            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body!!.string()
                activity!!.runOnUiThread{
                    try {
                        val branches = gson.fromJson<MutableList<Branch>>(dataString, object : TypeToken<MutableList<Branch>>(){}.type)
                        restaurantsTimer.cancel()
                        restaurantsTimerTask.cancel()
                        refresh_discovery.isRefreshing = false

                        discover_restaurants_shimmer.visibility = View.GONE
                        discover_restaurants.visibility = View.VISIBLE

                        fusedLocationClient.lastLocation.addOnSuccessListener {
                            if(it != null){
                                val from = LatLng(it.latitude, it.longitude)
                                branches.forEach { b ->
                                    val to = LatLng(b.latitude, b.longitude)
                                    val dist = mUtilFunctions.calculateDistance(from, to)
                                    b.dist = dist
                                    b.fromLocation = from
                                }
                            } else {
                                val from = LatLng(session.addresses!!.addresses[0].latitude, session.addresses!!.addresses[0].longitude)
                                branches.forEach { b ->
                                    val to = LatLng(b.latitude, b.longitude)
                                    val dist = mUtilFunctions.calculateDistance(from, to)
                                    b.dist = dist
                                    b.fromLocation = from
                                }
                            }
                            try {
                                discover_restaurants_shimmer.visibility = View.GONE
                                discover_restaurants.visibility = View.VISIBLE
                                discover_restaurants.apply {
                                    size = branches.size
                                    resource = R.layout.row_discover_restaurant
                                    indicatorAnimationType = IndicatorAnimationType.THIN_WORM
                                    carouselOffset = OffsetType.START
                                    setCarouselViewListener { view, position ->
                                        val branch = branches[position]
                                        view.restaurant_name.text = "${branch.restaurant?.name}, ${branch.name}"
                                        view.restaurant_address.text = branch.address
                                        view.restaurant_rating.rating = branch.reviews!!.average
                                        Picasso.get().load(branch.restaurant?.logoURL()).into(view.restaurant_image)
                                        view.setOnClickListener {
                                            val intent = Intent(context, RestaurantProfile::class.java)
                                            intent.putExtra("resto", branch.id)
                                            activity?.startActivity(intent)
                                        }
                                    }
                                    hideIndicator(true)
                                    show()
                                }
                            } catch (e: Exception) {}

                        }.addOnFailureListener {
                            val from = LatLng(session.addresses!!.addresses[0].latitude, session.addresses!!.addresses[0].longitude)
                            branches.forEach { b ->
                                val to = LatLng(b.latitude, b.longitude)
                                val dist = mUtilFunctions.calculateDistance(from, to)
                                b.dist = dist
                                b.fromLocation = from
                            }
                            discover_restaurants_shimmer.visibility = View.GONE
                            discover_restaurants.visibility = View.VISIBLE
                            discover_restaurants.apply {
                                size = branches.size
                                resource = R.layout.row_discover_restaurant
                                indicatorAnimationType = IndicatorAnimationType.THIN_WORM
                                carouselOffset = OffsetType.START
                                setCarouselViewListener { view, position ->
                                    val branch = branches[position]
                                    view.restaurant_name.text = "${branch.restaurant?.name}, ${branch.name}"
                                    view.restaurant_address.text = branch.address
                                    view.restaurant_rating.rating = branch.reviews!!.average
                                    Picasso.get().load(branch.restaurant?.logoURL()).into(view.restaurant_image)
                                    view.setOnClickListener {
                                        val intent = Intent(context, RestaurantProfile::class.java)
                                        intent.putExtra("resto", branch.id)
                                        activity?.startActivity(intent)
                                    }
                                }
                                hideIndicator(true)
                                try { show() } catch (e: Exception){}
                            }
                            TingToast(
                                context!!,
                                it.message!!.capitalize(),
                                TingToastType.ERROR
                            ).showToast(Toast.LENGTH_LONG)
                        }
                    } catch (e: Exception) {
                        restaurantsTimer.cancel()
                        restaurantsTimerTask.cancel()
                    }
                }
            }
        })
    }

    private fun getCuisines() {
        val url = Routes.cuisinesGlobal
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60 * 5, TimeUnit.SECONDS).build()

        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                activity!!.runOnUiThread {}
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body!!.string()
                try {
                    val cuisines = gson.fromJson<MutableList<RestaurantCategory>>(dataString, object : TypeToken<MutableList<RestaurantCategory>>(){}.type)

                    activity!!.runOnUiThread {
                        cuisinesTimer.cancel()
                        cuisinesTimerTask.cancel()
                        mLocalData.saveCuisines(dataString)

                        cuisines_shimmer.visibility = View.GONE
                        cuisines_recycler_view.visibility = View.VISIBLE

                        val layoutManager = LinearLayoutManager(context)
                        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
                        cuisines_recycler_view.layoutManager = layoutManager
                        cuisines_recycler_view.adapter =
                            CuisinesAdapter(cuisines.shuffled().toMutableList())
                    }
                } catch (e: Exception) {}
            }
        })
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun getDiscoverTodayPromotions() {
        val url = Routes.discoverTodayPromosRand
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60 * 5, TimeUnit.SECONDS)
            .addInterceptor(LocationInterceptor())
            .build()

        val request = Request.Builder().header("Authorization", session.token!!).url(url).get().build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                activity!!.runOnUiThread {}
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body!!.string()
                activity!!.runOnUiThread{
                    try {
                        val promotions = gson.fromJson<MutableList<MenuPromotion>>(dataString, object : TypeToken<MutableList<MenuPromotion>>(){}.type)
                        promotionsTimer.cancel()
                        promotionsTimerTask.cancel()
                        refresh_discovery.isRefreshing = false

                        if(promotions.size > 0) {
                            promotions_shimmer.visibility = View.GONE
                            discover_promotions.visibility = View.VISIBLE

                            val viewListener = ViewListener { inflateDiscoveredPromotion(promotions[it]) }
                            discover_promotions.setViewListener(viewListener)
                            discover_promotions.pageCount = promotions.size

                            discover_more_promotions.visibility = View.VISIBLE
                            discover_more_promotions.setOnClickListener {
                                activity!!.startActivity(Intent(context, TodayPromotions::class.java))
                            }
                        } else { discover_promotions_view.visibility = View.GONE }
                    } catch (e: Exception) {
                        promotionsTimer.cancel()
                        promotionsTimerTask.cancel()
                        discover_promotions_view.visibility = View.GONE
                    }
                }
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun inflateDiscoveredPromotion(promotion: MenuPromotion) : View {
        val view = layoutInflater.inflate(R.layout.row_discover_promotion, null)
        Picasso.get().load("${Routes.HOST_END_POINT}${promotion.posterImage}").into(view.promotion_poster)
        view.promotion_title.text = promotion.occasionEvent
        when (promotion.promotionItem.type.id) {
            5 -> view.promotion_type.text = "Promotion On ${promotion.promotionItem.category?.name}"
            4 -> view.promotion_type.text = "Promotion On ${promotion.promotionItem.menu?.menu?.name}"
            else -> view.promotion_type.text = promotion.promotionItem.type.name
        }
        if (promotion.reduction.hasReduction){
            view.promotion_reduction.text = "Order this menu and get ${promotion.reduction.amount} ${promotion.reduction.reductionType} reduction"
        } else {
            view.promotion_reduction_icon.visibility = View.GONE
            view.promotion_reduction.visibility = View.GONE
        }

        if(promotion.supplement.hasSupplement){
            if (!promotion.supplement.isSame){ view.promotion_supplement.text = "Order ${promotion.supplement.minQuantity} of this menu and get ${promotion.supplement.quantity} free ${promotion.supplement.supplement?.menu?.name}" }
            else { view.promotion_supplement.text = "Order ${promotion.supplement.minQuantity} of this menu and get ${promotion.supplement.quantity} more for free" }
        } else {
            view.promotion_supplement_icon.visibility = View.GONE
            view.promotion_supplement.visibility = View.GONE
        }

        view.setOnClickListener {
            val intent = Intent(activity, com.codepipes.ting.activities.menu.MenuPromotion::class.java)
            intent.putExtra("promo", promotion.id)
            intent.putExtra("url", promotion.urls.apiGet)
            activity!!.startActivity(intent)
        }

        return view
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun getDiscoverMenus() {
        val url = Routes.discoverMenus
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60 * 5, TimeUnit.SECONDS)
            .addInterceptor(LocationInterceptor())
            .build()

        val request = Request.Builder().header("Authorization", session.token!!).url(url).get().build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                activity!!.runOnUiThread {}
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body!!.string()
                activity!!.runOnUiThread{
                    try {
                        val menus = gson.fromJson<MutableList<RestaurantMenu>>(dataString, object : TypeToken<MutableList<RestaurantMenu>>(){}.type)
                        discoverMenusTimer.cancel()
                        discoverMenusTimerTask.cancel()
                        refresh_discovery.isRefreshing = false

                        discover_menus_shimmer.visibility = View.GONE
                        discover_menus.visibility = View.VISIBLE

                        discover_restaurants.carouselViewListener = null

                        discover_menus.apply {
                            size = menus.size
                            resource = R.layout.row_discover_menu
                            indicatorAnimationType = IndicatorAnimationType.THIN_WORM
                            carouselOffset = OffsetType.START
                            setCarouselViewListener { view, position ->
                                val menu = menus[position]
                                view.menu_name.text = menu.menu.name
                                view.menu_restaurant_address.text = menu.restaurant.name
                                view.menu_rating.rating = menu.menu.reviews!!.average

                                val index = (0 until menu.menu.images.count - 1).random()
                                val image = menu.menu.images.images[index]
                                Picasso.get().load("${Routes.HOST_END_POINT}${image.image}").into(view.menu_image)

                                view.menu_price.text = "${menu.menu.currency} ${NumberFormat.getNumberInstance().format(menu.menu.price)}".toUpperCase()
                                view.setOnClickListener {
                                    val intent = Intent(context, com.codepipes.ting.activities.menu.RestaurantMenu::class.java)
                                    intent.putExtra("menu", menu.id)
                                    intent.putExtra("url", menu.urls.apiGet)
                                    activity?.startActivity(intent)
                                }
                            }
                            hideIndicator(true)
                            enableSnapping(true)
                            scaleOnScroll = false
                            try { show() } catch (e: Exception){}
                        }
                    } catch (e: Exception) {
                        discoverMenusTimer.cancel()
                        discoverMenusTimerTask.cancel()
                    }
                }
            }
        })
    }

    @SuppressLint("DefaultLocale")
    private fun getDiscoverTopRestaurants() {
        val url = Routes.discoverTopRestaurants
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60 * 5, TimeUnit.SECONDS)
            .addInterceptor(LocationInterceptor())
            .build()

        val request = Request.Builder().header("Authorization", session.token!!).url(url).get().build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                activity!!.runOnUiThread {}
            }

            @SuppressLint("MissingPermission")
            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body!!.string()
                activity!!.runOnUiThread {
                    if(mUtilFunctions.checkLocationPermissions()){
                        try {
                            topRestaurantTimer.cancel()
                            topRestaurantsTimerTask.cancel()
                            refresh_discovery.isRefreshing = false
                            val branches = gson.fromJson<MutableList<Branch>>(dataString, object : TypeToken<MutableList<Branch>>(){}.type)
                            fusedLocationClient.lastLocation.addOnSuccessListener {
                                if(it != null){
                                    val from = LatLng(it.latitude, it.longitude)
                                    branches.forEach { b ->
                                        val to = LatLng(b.latitude, b.longitude)
                                        val dist = mUtilFunctions.calculateDistance(from, to)
                                        b.dist = dist
                                        b.fromLocation = from
                                    }
                                } else {
                                    val from = LatLng(session.addresses!!.addresses[0].latitude, session.addresses!!.addresses[0].longitude)
                                    branches.forEach { b ->
                                        val to = LatLng(b.latitude, b.longitude)
                                        val dist = mUtilFunctions.calculateDistance(from, to)
                                        b.dist = dist
                                        b.fromLocation = from
                                    }
                                }
                                top_restaurants_shimmer.visibility = View.GONE
                                top_restaurants.visibility = View.VISIBLE
                                top_restaurants.layoutManager = LinearLayoutManager(context)
                                top_restaurants.adapter = CuisineRestaurantsAdapter(branches, fragmentManager!!, statusWorkTimer)
                            }.addOnFailureListener {
                                val from = LatLng(session.addresses!!.addresses[0].latitude, session.addresses!!.addresses[0].longitude)
                                branches.forEach { b ->
                                    val to = LatLng(b.latitude, b.longitude)
                                    val dist = mUtilFunctions.calculateDistance(from, to)
                                    b.dist = dist
                                    b.fromLocation = from
                                }
                                top_restaurants_shimmer.visibility = View.GONE
                                top_restaurants.visibility = View.VISIBLE
                                top_restaurants.layoutManager = LinearLayoutManager(context)
                                top_restaurants.adapter = CuisineRestaurantsAdapter(branches, fragmentManager!!, statusWorkTimer)
                                TingToast(
                                    context!!,
                                    it.message!!.capitalize(),
                                    TingToastType.ERROR
                                ).showToast(Toast.LENGTH_LONG)
                            }
                            ViewCompat.setNestedScrollingEnabled(top_restaurants, false)
                        } catch (e: Exception){
                            topRestaurantTimer.cancel()
                            topRestaurantsTimerTask.cancel()
                            top_restaurants_view.visibility = View.GONE
                            TingToast(
                                context!!,
                                e.message!!.capitalize(),
                                TingToastType.ERROR
                            ).showToast(Toast.LENGTH_LONG)
                        }
                    }
                }
            }
        })
    }

    private fun getDiscoverTopMenus() {
        val url = Routes.discoverTopMenus
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60 * 5, TimeUnit.SECONDS)
            .addInterceptor(LocationInterceptor())
            .build()

        val request = Request.Builder().header("Authorization", session.token!!).url(url).get().build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                activity!!.runOnUiThread {}
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body!!.string()
                activity!!.runOnUiThread {
                    topMenusTimer.cancel()
                    topMenusTimerTask.cancel()
                    refresh_discovery.isRefreshing = false
                    try {
                        val menus = gson.fromJson<MutableList<RestaurantMenu>>(dataString, object : TypeToken<MutableList<RestaurantMenu>>(){}.type)
                        top_menus_shimmer.visibility = View.GONE
                        top_menus.visibility = View.VISIBLE
                        top_menus.layoutManager = LinearLayoutManager(context)
                        top_menus.adapter = CuisineMenusAdapter(menus.toMutableSet())
                        ViewCompat.setNestedScrollingEnabled(top_menus, false)
                    } catch (e: Exception) { top_menus_view.visibility = View.GONE }
                }
            }
        })
    }

    private inner  class RestaurantsTimerTask() : TimerTask() {
        override fun run() { getDiscoverRestaurants() }
    }

    private inner  class CuisinesTimerTask() : TimerTask() {
        override fun run() { getCuisines() }
    }

    private inner  class PromotionsTimerTask() : TimerTask() {
        override fun run() { getDiscoverTodayPromotions() }
    }

    private inner  class DiscoverMenusTimerTask() : TimerTask() {
        override fun run() { getDiscoverMenus() }
    }

    private inner  class TopRestaurantsTimerTask() : TimerTask() {
        override fun run() { getDiscoverTopRestaurants() }
    }

    private inner  class TopMenusTimerTask() : TimerTask() {
        override fun run() { getDiscoverTopMenus() }
    }

    private fun cancelTimers() {
        try {
            restaurantsTimer.cancel()
            cuisinesTimer.cancel()
            promotionsTimer.cancel()
            discoverMenusTimer.cancel()
            topMenusTimer.cancel()
            topRestaurantTimer.cancel()

            restaurantsTimerTask.cancel()
            cuisinesTimerTask.cancel()
            promotionsTimerTask.cancel()
            discoverMenusTimerTask.cancel()
            topMenusTimerTask.cancel()
            topRestaurantsTimerTask.cancel()
        } catch (e: Exception) {}
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            UtilsFunctions.REQUEST_FINE_LOCATION -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getDiscoverTopRestaurants()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Bridge.saveInstanceState(this, outState)
        outState.clear()
    }

    override fun onDetach() {
        super.onDetach()
        cancelTimers()
        try { statusWorkTimer.cancel() } catch (e: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelTimers()
        try { statusWorkTimer.cancel() } catch (e: Exception) {}
        Bridge.clear(this)
    }

    override fun onPause() {
        super.onPause()
        cancelTimers()
        try { statusWorkTimer.cancel() } catch (e: Exception) {}
        Bridge.clear(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cancelTimers()
        try { statusWorkTimer.cancel() } catch (e: Exception) {}
        Bridge.clear(this)
        Bridge.clearAll(context!!)
    }

    companion object {
        private const val TIMER_PERIOD = 10000.toLong()
    }
}
