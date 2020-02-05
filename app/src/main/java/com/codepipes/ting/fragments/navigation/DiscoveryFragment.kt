package com.codepipes.ting.fragments.navigation


import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import com.codepipes.ting.R
import com.codepipes.ting.RestaurantProfile
import com.codepipes.ting.TodayPromotions
import com.codepipes.ting.adapters.cuisine.CuisineMenusAdapter
import com.codepipes.ting.adapters.cuisine.CuisineRestaurantsAdapter
import com.codepipes.ting.adapters.cuisine.CuisinesAdapter
import com.codepipes.ting.carouselview.enums.IndicatorAnimationType
import com.codepipes.ting.carouselview.enums.OffsetType
import com.codepipes.ting.dialogs.TingToast
import com.codepipes.ting.dialogs.TingToastType
import com.codepipes.ting.models.*
import com.codepipes.ting.providers.LocalData
import com.codepipes.ting.providers.UserAuthentication
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
import java.util.*
import java.util.concurrent.TimeUnit


class DiscoveryFragment : Fragment() {

    private lateinit var session: User
    private lateinit var userAuthentication: UserAuthentication
    private lateinit var mLocalData: LocalData

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mUtilFunctions: UtilsFunctions

    private lateinit var gson: Gson
    private lateinit var routes: Routes

    private lateinit var restaurantsTimer: Timer
    private lateinit var cuisinesTimer: Timer
    private lateinit var promotionsTimer: Timer
    private lateinit var discoverMenusTimer: Timer
    private lateinit var topRestaurantTimer: Timer
    private lateinit var topMenusTimer: Timer

    private val TIMER_PERIOD = 6000.toLong()

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

        gson = Gson()
        routes = Routes()

        restaurantsTimer = Timer()
        cuisinesTimer = Timer()
        promotionsTimer = Timer()
        discoverMenusTimer = Timer()
        topRestaurantTimer = Timer()
        topMenusTimer = Timer()

        view.discover_restaurants_shimmer.startShimmer()
        restaurantsTimer.scheduleAtFixedRate(object : TimerTask() { override fun run() { getDiscoverRestaurants(view) } }, TIMER_PERIOD, TIMER_PERIOD)
        this.getDiscoverRestaurants(view)

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
            cuisinesTimer.scheduleAtFixedRate(object : TimerTask() { override fun run() { getCuisines() } }, TIMER_PERIOD, TIMER_PERIOD)
            this.getCuisines()
        }

        view.promotions_shimmer.startShimmer()
        promotionsTimer.scheduleAtFixedRate(object : TimerTask() { override fun run() { getDiscoverTodayPromotions(view) } }, TIMER_PERIOD, TIMER_PERIOD)
        this.getDiscoverTodayPromotions(view)

        view.discover_menus_shimmer.startShimmer()
        discoverMenusTimer.scheduleAtFixedRate(object : TimerTask() { override fun run () { getDiscoverMenus(view) } }, TIMER_PERIOD, TIMER_PERIOD)
        this.getDiscoverMenus(view)

        view.top_restaurants_shimmer.startShimmer()
        topRestaurantTimer.scheduleAtFixedRate(object : TimerTask() { override fun run () { getDiscoverTopRestaurants(view) } }, TIMER_PERIOD, TIMER_PERIOD)
        this.getDiscoverTopRestaurants(view)

        view.top_menus_shimmer.startShimmer()
        topMenusTimer.scheduleAtFixedRate(object : TimerTask() { override fun run () { getDiscoverTopMenus(view) } }, TIMER_PERIOD, TIMER_PERIOD)
        this.getDiscoverTopMenus(view)

        view.refresh_discovery.setColorSchemeColors(resources.getColor(R.color.colorPrimary), resources.getColor(R.color.colorAccentMain), resources.getColor(R.color.colorPrimaryDark), resources.getColor(R.color.colorAccentMain))
        view.refresh_discovery.setOnRefreshListener {

            view.refresh_discovery.isRefreshing = true

            view.discover_recommended_restaurants_view.visibility = View.VISIBLE
            this.getDiscoverRestaurants(view)

            view.discover_promotions_view.visibility = View.VISIBLE
            this.getDiscoverTodayPromotions(view)

            view.discover_menus_view.visibility = View.VISIBLE
            this.getDiscoverMenus(view)

            view.top_restaurants_view.visibility = View.VISIBLE
            this.getDiscoverTopRestaurants(view)

            view.top_menus_view.visibility = View.VISIBLE
            this.getDiscoverTopMenus(view)
        }

        return view
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun getDiscoverRestaurants(view: View) {
        val url = routes.discoverRestaurants
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60 * 5, TimeUnit.SECONDS).build()

        val request = Request.Builder().header("Authorization", session.token!!).url(url).get().build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                activity!!.runOnUiThread {}
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body()!!.string()
                activity!!.runOnUiThread{
                    try {
                        val restaurants = gson.fromJson<MutableList<Branch>>(dataString, object : TypeToken<MutableList<Branch>>(){}.type)
                        restaurantsTimer.cancel()
                        view.refresh_discovery.isRefreshing = false

                        view.discover_restaurants_shimmer.visibility = View.GONE
                        view.discover_restaurants.visibility = View.VISIBLE

                        view.discover_restaurants.apply {
                            size = restaurants.size
                            resource = R.layout.row_discover_restaurant
                            indicatorAnimationType = IndicatorAnimationType.THIN_WORM
                            carouselOffset = OffsetType.START
                            setCarouselViewListener { view, position ->
                                val branch = restaurants[position]
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
                    } catch (e: Exception) { restaurantsTimer.cancel() }
                }
            }
        })
    }

    private fun getCuisines() {
        val url = routes.cuisinesGlobal
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
                val dataString = response.body()!!.string()
                val cuisines = gson.fromJson<MutableList<RestaurantCategory>>(dataString, object : TypeToken<MutableList<RestaurantCategory>>(){}.type)

                activity!!.runOnUiThread {
                    cuisinesTimer.cancel()
                    mLocalData.saveCuisines(dataString)

                    cuisines_shimmer.visibility = View.GONE
                    cuisines_recycler_view.visibility = View.VISIBLE

                    val layoutManager = LinearLayoutManager(context)
                    layoutManager.orientation = LinearLayoutManager.HORIZONTAL
                    cuisines_recycler_view.layoutManager = layoutManager
                    cuisines_recycler_view.adapter =
                        CuisinesAdapter(cuisines.shuffled().toMutableList())
                }
            }
        })
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun getDiscoverTodayPromotions(view: View) {
        val url = routes.discoverTodayPromosRand
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60 * 5, TimeUnit.SECONDS).build()

        val request = Request.Builder().header("Authorization", session.token!!).url(url).get().build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                activity!!.runOnUiThread {}
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body()!!.string()
                val promotions = gson.fromJson<MutableList<MenuPromotion>>(dataString, object : TypeToken<MutableList<MenuPromotion>>(){}.type)
                activity!!.runOnUiThread{
                    try {
                        promotionsTimer.cancel()
                        view.refresh_discovery.isRefreshing = false

                        if(promotions.size > 0) {
                            view.promotions_shimmer.visibility = View.GONE
                            view.discover_promotions.visibility = View.VISIBLE

                            val viewListener = ViewListener { inflateDiscoveredPromotion(promotions[it]) }
                            view.discover_promotions.setViewListener(viewListener)
                            view.discover_promotions.pageCount = promotions.size

                            view.discover_more_promotions.visibility = View.VISIBLE
                            view.discover_more_promotions.setOnClickListener {
                                activity!!.startActivity(Intent(context, TodayPromotions::class.java))
                            }
                        } else { view.discover_promotions_view.visibility = View.GONE }
                    } catch (e: Exception) {
                        promotionsTimer.cancel()
                        view.discover_promotions_view.visibility = View.GONE
                    }
                }
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun inflateDiscoveredPromotion(promotion: MenuPromotion) : View {
        val view = layoutInflater.inflate(R.layout.row_discover_promotion, null)
        Picasso.get().load("${Routes().HOST_END_POINT}${promotion.posterImage}").into(view.promotion_poster)
        view.promotion_title.text = promotion.occasionEvent
        when {
            promotion.promotionItem.type.id == 5 -> view.promotion_type.text = "Promotion On ${promotion.promotionItem.category?.name}"
            promotion.promotionItem.type.id == 4 -> view.promotion_type.text = "Promotion On ${promotion.promotionItem.menu?.menu?.name}"
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
            val intent = Intent(activity, com.codepipes.ting.MenuPromotion::class.java)
            intent.putExtra("promo", promotion.id)
            intent.putExtra("url", promotion.urls.apiGet)
            activity!!.startActivity(intent)
        }

        return view
    }

    @SuppressLint("DefaultLocale")
    private fun getDiscoverMenus(view: View) {
        val url = routes.discoverMenus
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60 * 5, TimeUnit.SECONDS).build()

        val request = Request.Builder().header("Authorization", session.token!!).url(url).get().build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                activity!!.runOnUiThread {}
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body()!!.string()
                activity!!.runOnUiThread{
                    try {
                        val menus = gson.fromJson<MutableList<RestaurantMenu>>(dataString, object : TypeToken<MutableList<RestaurantMenu>>(){}.type)
                        discoverMenusTimer.cancel()
                        view.refresh_discovery.isRefreshing = false

                        view.discover_menus_shimmer.visibility = View.GONE
                        view.discover_menus.visibility = View.VISIBLE

                        view.discover_restaurants.carouselViewListener = null

                        view.discover_menus.apply {
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
                                Picasso.get().load("${Routes().HOST_END_POINT}${image.image}").into(view.menu_image)

                                view.setOnClickListener {
                                    val intent = Intent(context, com.codepipes.ting.RestaurantMenu::class.java)
                                    intent.putExtra("menu", menu.id)
                                    intent.putExtra("url", menu.urls.apiGet)
                                    activity?.startActivity(intent)
                                }
                            }
                            hideIndicator(true)
                            enableSnapping(true)
                            scaleOnScroll = false
                            show()
                        }
                    } catch (e: Exception) { discoverMenusTimer.cancel() }
                }
            }
        })
    }

    @SuppressLint("DefaultLocale")
    private fun getDiscoverTopRestaurants(view: View) {
        val url = routes.discoverTopRestaurants
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60 * 5, TimeUnit.SECONDS).build()

        val request = Request.Builder().header("Authorization", session.token!!).url(url).get().build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                activity!!.runOnUiThread {}
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body()!!.string()
                activity!!.runOnUiThread {
                    if(mUtilFunctions.checkLocationPermissions()){
                        try {
                            topRestaurantTimer.cancel()
                            view.refresh_discovery.isRefreshing = false
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
                                view.top_restaurants_shimmer.visibility = View.GONE
                                view.top_restaurants.visibility = View.VISIBLE
                                view.top_restaurants.layoutManager = LinearLayoutManager(context)
                                view.top_restaurants.adapter = CuisineRestaurantsAdapter(branches, fragmentManager!!)
                            }.addOnFailureListener {
                                val from = LatLng(session.addresses!!.addresses[0].latitude, session.addresses!!.addresses[0].longitude)
                                branches.forEach { b ->
                                    val to = LatLng(b.latitude, b.longitude)
                                    val dist = mUtilFunctions.calculateDistance(from, to)
                                    b.dist = dist
                                    b.fromLocation = from
                                }
                                view.top_restaurants_shimmer.visibility = View.GONE
                                view.top_restaurants.visibility = View.VISIBLE
                                view.top_restaurants.layoutManager = LinearLayoutManager(context)
                                view.top_restaurants.adapter = CuisineRestaurantsAdapter(branches, fragmentManager!!)
                                TingToast(context!!, it.message!!.capitalize(), TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                            }
                        } catch (e: Exception){
                            topRestaurantTimer.cancel()
                            view.top_restaurants_view.visibility = View.GONE
                            TingToast(context!!, e.message!!.capitalize(), TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                        }
                    }
                }
            }
        })
    }

    private fun getDiscoverTopMenus(view: View) {
        val url = routes.discoverTopMenus
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60 * 5, TimeUnit.SECONDS).build()

        val request = Request.Builder().header("Authorization", session.token!!).url(url).get().build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                activity!!.runOnUiThread {}
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body()!!.string()
                activity!!.runOnUiThread {
                    topMenusTimer.cancel()
                    view.refresh_discovery.isRefreshing = false
                    try {
                        val menus = gson.fromJson<MutableList<RestaurantMenu>>(dataString, object : TypeToken<MutableList<RestaurantMenu>>(){}.type)
                        view.top_menus_shimmer.visibility = View.GONE
                        view.top_menus.visibility = View.VISIBLE
                        view.top_menus.layoutManager = LinearLayoutManager(context)
                        view.top_menus.adapter = CuisineMenusAdapter(menus)
                    } catch (e: Exception) { view.top_menus_view.visibility = View.GONE }
                }
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Bridge.saveInstanceState(this, outState)
        outState.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            restaurantsTimer.cancel()
            cuisinesTimer.cancel()
            promotionsTimer.cancel()
            discoverMenusTimer.cancel()
            topMenusTimer.cancel()
            topRestaurantTimer.cancel()
        } catch (e: Exception) {}
        Bridge.clear(this)
    }

    override fun onPause() {
        super.onPause()
        try {
            restaurantsTimer.cancel()
            cuisinesTimer.cancel()
            promotionsTimer.cancel()
            discoverMenusTimer.cancel()
            topMenusTimer.cancel()
            topRestaurantTimer.cancel()
        } catch (e: Exception) {}
    }
}
