package com.codepipes.ting.fragments.navigation


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codepipes.ting.R
import com.codepipes.ting.fragments.restaurants.RestaurantsMapFragment
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Toast
import androidx.core.widget.NestedScrollView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.codepipes.ting.activities.placement.CurrentRestaurant
import com.codepipes.ting.adapters.cuisine.CuisinesAdapter
import com.codepipes.ting.adapters.restaurant.GlobalRestaurantAdapter
import com.codepipes.ting.custom.ActionSheet
import com.codepipes.ting.dialogs.messages.ConfirmDialog
import com.codepipes.ting.dialogs.messages.ProgressOverlay
import com.codepipes.ting.dialogs.messages.TingToast
import com.codepipes.ting.dialogs.messages.TingToastType
import com.codepipes.ting.fragments.restaurants.RestaurantFiltersFragment
import com.codepipes.ting.interfaces.ActionSheetCallBack
import com.codepipes.ting.interfaces.ConfirmDialogListener
import com.codepipes.ting.interfaces.FilterRestaurantsClickListener
import com.codepipes.ting.models.Branch
import com.codepipes.ting.models.RestaurantCategory
import com.codepipes.ting.models.User
import com.codepipes.ting.providers.LocalData
import com.codepipes.ting.providers.TingClient
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.UtilsFunctions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.livefront.bridge.Bridge
import kotlinx.android.synthetic.main.fragment_restaurants.*
import kotlinx.android.synthetic.main.fragment_restaurants.view.*
import kotlinx.android.synthetic.main.include_empty_data.view.*
import okhttp3.*
import java.io.IOException
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class RestaurantsFragment : Fragment() {

    private lateinit var activity: Activity
    private val gson: Gson = Gson()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mUtilFunctions: UtilsFunctions
    private lateinit var mLocalData: LocalData

    private lateinit var session: User
    private lateinit var userAuthentication: UserAuthentication

    private lateinit var restaurants: MutableList<Branch>

    private var selectedLatitude: Double = 0.0
    private var selectedLongitude: Double = 0.0

    private var country: String = ""
    private var town: String = ""

    private lateinit var cuisinesTimer: Timer
    private lateinit var restaurantsTimer: Timer
    private lateinit var filteredRestaurantTimer: Timer
    private lateinit var statusWorkTimer: Timer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Bridge.restoreInstanceState(this, savedInstanceState)
        savedInstanceState?.clear()
    }

    @SuppressLint("SetTextI18n", "MissingPermission", "DefaultLocale")
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_restaurants, container, false)

        restaurants = ArrayList()

        activity = context!! as Activity
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
        mUtilFunctions = UtilsFunctions(context!!)
        mLocalData = LocalData(context!!)

        userAuthentication = UserAuthentication(context!!)
        session = userAuthentication.get()!!

        country = mLocalData.getUserCountry() ?: session.country
        town = mLocalData.getUserTown() ?: session.town

        this.getFilters()
        this.getRestaurants(view)

        cuisinesTimer = Timer()
        restaurantsTimer = Timer()
        filteredRestaurantTimer = Timer()
        statusWorkTimer = Timer()

        view.open_restaurant_map.setOnClickListener {
            val cx = (open_restaurant_map.x + open_restaurant_map.width / 2).toInt()
            val cy = (open_restaurant_map.y + open_restaurant_map.height + 56).toInt()

            val mapFragment =  RestaurantsMapFragment()
            val args: Bundle = Bundle()

            args.putInt("cx", cx)
            args.putInt("cy", cy)
            args.putDouble("lat", selectedLatitude)
            args.putDouble("lng", selectedLongitude)

            mapFragment.arguments = args
            mapFragment.setRestaurants(gson.toJson(restaurants))
            mapFragment.show(fragmentManager!!, mapFragment.tag)
        }

        view.open_restaurant_map.isClickable = false

        view.cuisines_shimmer.startShimmer()
        view.shimmer_loader.startShimmer()

        val cuisines = mLocalData.getCuisines()

        if(!cuisines.isNullOrEmpty()) {
            view.cuisines_recycler_view.visibility = View.VISIBLE
            view.cuisines_shimmer.visibility = View.GONE
            val layoutManager = LinearLayoutManager(context)
            layoutManager.orientation = LinearLayoutManager.HORIZONTAL
            view.cuisines_recycler_view.layoutManager = layoutManager
            view.cuisines_recycler_view.adapter =
                CuisinesAdapter(cuisines.shuffled().toMutableList())
        } else {
            cuisinesTimer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() { getCuisines() }
            }, TIMER_PERIOD, TIMER_PERIOD)
            this.getCuisines()
        }

        val menuList = mutableListOf<String>()
        menuList.add("Current Location")
        session.addresses?.addresses!!.forEach { menuList.add("${it.type} - ${it.address}") }

        view.open_restaurant_map.setOnLongClickListener {
            val actionSheet = ActionSheet(context!!, menuList)
                .setTitle("Restaurant Near Location")
                .setColorData(activity.resources.getColor(R.color.colorGray))
                .setColorTitleCancel(activity.resources.getColor(R.color.colorGoogleRedTwo))
                .setColorSelected(activity.resources.getColor(R.color.colorPrimary))
                .setCancelTitle("Cancel")

            actionSheet.create(object : ActionSheetCallBack {

                override fun data(data: String, position: Int) {
                    if(!restaurants.isNullOrEmpty()){
                        if(position == 0){ showRestaurantWithCurrentAddress(1, view) }
                        else {
                            val address = session.addresses?.addresses!![position - 1]
                            val from = LatLng(address.latitude, address.longitude)
                            selectedLatitude = address.latitude
                            selectedLongitude = address.longitude
                            restaurants.forEach { b ->
                                val to = LatLng(b.latitude, b.longitude)
                                val dist = mUtilFunctions.calculateDistance(from, to)
                                b.dist = dist
                                b.fromLocation = from
                            }
                            restaurants.sortBy { b -> b.dist }
                            restaurants_recycler_view.layoutManager = LinearLayoutManager(context)
                            restaurants_recycler_view.adapter = GlobalRestaurantAdapter(restaurants, fragmentManager!!, statusWorkTimer)
                        }
                    }
                }
            })

            return@setOnLongClickListener true
        }

        view.refresh_restaurants.setColorSchemeColors(context!!.resources.getColor(R.color.colorPrimary), context!!.resources.getColor(R.color.colorAccentMain), context!!.resources.getColor(R.color.colorPrimaryDark), context!!.resources.getColor(R.color.colorAccentMain))
        view.refresh_restaurants.setOnRefreshListener {
            statusWorkTimer = Timer()
            refresh_restaurants.isRefreshing = true
            this.getRestaurants(view)
        }

        restaurantsTimer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() { getRestaurants(view) }
        }, TIMER_PERIOD, TIMER_PERIOD)

        val query = view.search_restaurant_input.text.toString()

        view.filter_distance.setOnClickListener { TingToast(
            context!!,
            "Not Available For Now",
            TingToastType.DEFAULT
        ).showToast(Toast.LENGTH_LONG) }
        view.filter_availability.setOnClickListener { showFilters(AVAILABILITY_KEY, query, view) }
        view.filter_cuisines.setOnClickListener { showFilters(CUISINES_KEY, query, view) }
        view.filter_services.setOnClickListener { showFilters(SERVICES_KEY, query, view) }
        view.filter_specials.setOnClickListener { showFilters(SPECIALS_KEY, query, view) }
        view.filter_types.setOnClickListener { showFilters(TYPES_KEY, query, view) }
        view.filter_ratings.setOnClickListener { showFilters(RATINGS_KEY, query, view) }

        val mProgressOverlay: ProgressOverlay = ProgressOverlay()

        view.filter_restaurant_button.setOnClickListener {
            searchFilterRestaurants(gson.toJson(mLocalData.getParametersFilters()), query, view)
            filteredRestaurantTimer = Timer()
            filteredRestaurantTimer.scheduleAtFixedRate(object: TimerTask() {
                override fun run() { searchFilterRestaurants(gson.toJson(mLocalData.getParametersFilters()), query, view) }
            }, TIMER_PERIOD, TIMER_PERIOD)
            mProgressOverlay.show(fragmentManager!!, mProgressOverlay.tag)
        }

        view.filter_restaurant_button.setOnLongClickListener {

            val confirmDialog = ConfirmDialog()
            val bundle = Bundle()
            bundle.putString(CurrentRestaurant.CONFIRM_TITLE_KEY, "Reset All Filters")
            bundle.putString(CurrentRestaurant.CONFIRM_MESSAGE_KEY, "Do you really want to reset all filters ?")
            confirmDialog.arguments = bundle
            confirmDialog.show(fragmentManager!!, confirmDialog.tag)
            confirmDialog.onDialogListener(object : ConfirmDialogListener {
                override fun onAccept() { activity.runOnUiThread {
                    mLocalData.saveParametersFilters(null)
                    confirmDialog.dismiss()
                    TingToast(context!!, "Filters Reset Successfully", TingToastType.SUCCESS).showToast(Toast.LENGTH_LONG)}
                }
                override fun onCancel() { confirmDialog.dismiss() }
            })

            return@setOnLongClickListener true
        }

        view.search_restaurant_input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH) {
                if(view.search_restaurant_input.text.isNotBlank() && view.search_restaurant_input.text.isNotEmpty() && !view.search_restaurant_input.text.isNullOrBlank()) {
                    val inputManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputManager.hideSoftInputFromWindow(view.search_restaurant_input.windowToken, 0)
                    return@setOnEditorActionListener true
                }
            }
            return@setOnEditorActionListener false
        }

        return view
    }

    @SuppressLint("DefaultLocale")
    private fun showRestaurantWithCurrentAddress(typeLoad: Int, view: View) {

        view.restaurants_recycler_view.visibility = View.VISIBLE
        view.progress_loader.visibility = View.GONE
        view.empty_data.visibility = View.GONE

        if (shimmer_loader != null) { shimmer_loader.visibility = View.GONE }

        val linearLayoutManager = LinearLayoutManager(context)
        var globalRestaurantAdapter = GlobalRestaurantAdapter(restaurants, fragmentManager!!, statusWorkTimer)

        if(mUtilFunctions.checkLocationPermissions()){
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener {
                    if(it != null){
                        val from = LatLng(it.latitude, it.longitude)
                        selectedLatitude = it.latitude
                        selectedLongitude = it.longitude
                        restaurants.forEach { b ->
                            val to = LatLng(b.latitude, b.longitude)
                            val dist = mUtilFunctions.calculateDistance(from, to)
                            b.dist = dist
                            b.fromLocation = from
                        }
                    } else {
                        val from = LatLng(session.addresses!!.addresses[0].latitude, session.addresses!!.addresses[0].longitude)
                        selectedLatitude = session.addresses!!.addresses[0].latitude
                        selectedLongitude = session.addresses!!.addresses[0].longitude
                        restaurants.forEach { b ->
                            val to = LatLng(b.latitude, b.longitude)
                            val dist = mUtilFunctions.calculateDistance(from, to)
                            b.dist = dist
                            b.fromLocation = from
                        }
                    }
                    restaurants.sortBy { b -> b.dist }
                    globalRestaurantAdapter = GlobalRestaurantAdapter(restaurants, fragmentManager!!, statusWorkTimer)
                    view.restaurants_recycler_view.layoutManager = linearLayoutManager
                    view.restaurants_recycler_view.adapter = globalRestaurantAdapter
                }.addOnFailureListener {
                    val from = LatLng(session.addresses!!.addresses[0].latitude, session.addresses!!.addresses[0].longitude)
                    selectedLatitude = session.addresses!!.addresses[0].latitude
                    selectedLongitude = session.addresses!!.addresses[0].longitude
                    restaurants.forEach { b ->
                        val to = LatLng(b.latitude, b.longitude)
                        val dist = mUtilFunctions.calculateDistance(from, to)
                        b.dist = dist
                        b.fromLocation = from
                    }
                    restaurants.sortBy { b -> b.dist }
                    globalRestaurantAdapter = GlobalRestaurantAdapter(restaurants, fragmentManager!!, statusWorkTimer)
                    view.restaurants_recycler_view.layoutManager = linearLayoutManager
                    view.restaurants_recycler_view.adapter = globalRestaurantAdapter
                    TingToast(
                        context!!,
                        it.message!!,
                        TingToastType.ERROR
                    ).showToast(Toast.LENGTH_LONG)
                }

                var pageNum = 1
                ViewCompat.setNestedScrollingEnabled(view.restaurants_recycler_view, false)

                scroll_view.setOnScrollChangeListener { nestedScrollView: NestedScrollView?, _: Int, scrollY: Int, _: Int, oldScrollY: Int ->

                    if(nestedScrollView?.getChildAt(nestedScrollView.childCount - 1) != null) {
                        if ((scrollY >= (nestedScrollView.getChildAt(nestedScrollView.childCount - 1)!!.measuredHeight - nestedScrollView.measuredHeight)) && scrollY > oldScrollY) {

                            val visibleItemCount = linearLayoutManager.childCount
                            val totalItemCount = linearLayoutManager.itemCount
                            val pastVisibleItems = linearLayoutManager.findLastVisibleItemPosition()

                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {

                                pageNum++

                                if (typeLoad == 1) {
                                    TingClient.getRequest("${Routes.restaurantsGlobal}?page=$pageNum", LocationInterceptor(), session.token) { _, isSuccess, result ->
                                        activity.runOnUiThread {
                                            if(isSuccess){
                                                try {
                                                    val restosResultPage =
                                                        Gson().fromJson<MutableList<Branch>>(
                                                            result,
                                                            object :
                                                                TypeToken<MutableList<Branch>>() {}.type
                                                        )
                                                    try {
                                                        fusedLocationClient.lastLocation.addOnSuccessListener {
                                                            if(it != null){
                                                                val from = LatLng(it.latitude, it.longitude)
                                                                restosResultPage.forEach { b ->
                                                                    val to = LatLng(b.latitude, b.longitude)
                                                                    val dist = mUtilFunctions.calculateDistance(from, to)
                                                                    b.dist = dist
                                                                    b.fromLocation = from
                                                                }
                                                            } else {
                                                                val from = LatLng(session.addresses!!.addresses[0].latitude, session.addresses!!.addresses[0].longitude)
                                                                restosResultPage.forEach { b ->
                                                                    val to = LatLng(b.latitude, b.longitude)
                                                                    val dist = mUtilFunctions.calculateDistance(from, to)
                                                                    b.dist = dist
                                                                    b.fromLocation = from
                                                                }
                                                            }
                                                            restosResultPage.sortBy { b -> b.dist }
                                                            restaurants.addAll(restosResultPage)
                                                            globalRestaurantAdapter.addItems(restosResultPage)
                                                        }.addOnFailureListener {
                                                            val from = LatLng(session.addresses!!.addresses[0].latitude, session.addresses!!.addresses[0].longitude)
                                                            restosResultPage.forEach { b ->
                                                                val to = LatLng(b.latitude, b.longitude)
                                                                val dist = mUtilFunctions.calculateDistance(from, to)
                                                                b.dist = dist
                                                                b.fromLocation = from
                                                            }
                                                            restosResultPage.sortBy { b -> b.dist }
                                                            restaurants.addAll(restosResultPage)
                                                            globalRestaurantAdapter.addItems(restosResultPage)
                                                        }
                                                    } catch (e: Exception) {}

                                                } catch (e: Exception) { }
                                            }
                                        }
                                    }
                                } else {
                                    val formData = HashMap<String, String>()
                                    formData["country"] = country
                                    formData["town"] = town
                                    formData["query"] = search_restaurant_input.text.toString()
                                    formData["filters"] = gson.toJson(mLocalData.getParametersFilters())
                                    formData["page"] = pageNum.toString()

                                    TingClient.postRequest(Routes.restaurantsSearchFiltered, formData, null, session.token) { _, isSuccess, result ->
                                        activity.runOnUiThread {
                                            if(isSuccess){
                                                try {
                                                    val restosResultPage =
                                                        Gson().fromJson<MutableList<Branch>>(
                                                            result,
                                                            object :
                                                                TypeToken<MutableList<Branch>>() {}.type
                                                        )
                                                    try {
                                                        fusedLocationClient.lastLocation.addOnSuccessListener {
                                                            if(it != null){
                                                                val from = LatLng(it.latitude, it.longitude)
                                                                restosResultPage.forEach { b ->
                                                                    val to = LatLng(b.latitude, b.longitude)
                                                                    val dist = mUtilFunctions.calculateDistance(from, to)
                                                                    b.dist = dist
                                                                    b.fromLocation = from
                                                                }
                                                            } else {
                                                                val from = LatLng(session.addresses!!.addresses[0].latitude, session.addresses!!.addresses[0].longitude)
                                                                restosResultPage.forEach { b ->
                                                                    val to = LatLng(b.latitude, b.longitude)
                                                                    val dist = mUtilFunctions.calculateDistance(from, to)
                                                                    b.dist = dist
                                                                    b.fromLocation = from
                                                                }
                                                            }
                                                            restosResultPage.sortBy { b -> b.dist }
                                                            restaurants.addAll(restosResultPage)
                                                            globalRestaurantAdapter.addItems(restosResultPage)
                                                        }.addOnFailureListener {
                                                            val from = LatLng(session.addresses!!.addresses[0].latitude, session.addresses!!.addresses[0].longitude)
                                                            restosResultPage.forEach { b ->
                                                                val to = LatLng(b.latitude, b.longitude)
                                                                val dist = mUtilFunctions.calculateDistance(from, to)
                                                                b.dist = dist
                                                                b.fromLocation = from
                                                            }
                                                            restosResultPage.sortBy { b -> b.dist }
                                                            restaurants.addAll(restosResultPage)
                                                            globalRestaurantAdapter.addItems(restosResultPage)
                                                        }
                                                    } catch (e: Exception) {}

                                                } catch (e: Exception) { }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            } catch (e: Exception){ TingToast(
                context!!,
                e.message!!.capitalize(),
                TingToastType.ERROR
            ).showToast(Toast.LENGTH_LONG) }
        }
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

    @SuppressLint("SetTextI18n", "MissingPermission", "DefaultLocale")
    private fun getRestaurants(view: View){

        statusWorkTimer = Timer()

        val url = Routes.restaurantsGlobal
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60 * 5, TimeUnit.SECONDS)
            .addInterceptor(LocationInterceptor())
            .build()

        val request = Request.Builder()
            .header("Authorization", session.token!!)
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                activity.runOnUiThread {
                    refresh_restaurants.isRefreshing = false
                    restaurants_recycler_view.visibility = View.GONE
                    progress_loader.visibility = View.GONE
                    empty_data.visibility = View.VISIBLE

                    if (shimmer_loader != null) { shimmer_loader.visibility = View.GONE }
                    empty_data.empty_image.setImageResource(R.drawable.ic_restaurants)
                    empty_data.empty_text.text = "No Restaurant To Show"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body!!.string()
                try {
                    restaurants = gson.fromJson<MutableList<Branch>>(dataString, object : TypeToken<MutableList<Branch>>(){}.type)
                    activity.runOnUiThread {
                        restaurantsTimer.cancel()
                        refresh_restaurants.isRefreshing = false
                        mLocalData.saveRestaurants(dataString)

                        if(!restaurants.isNullOrEmpty()){ showRestaurantWithCurrentAddress(1, view) }
                        else {
                            restaurants_recycler_view.visibility = View.GONE
                            progress_loader.visibility = View.GONE
                            empty_data.visibility = View.VISIBLE

                            if (shimmer_loader != null) { shimmer_loader.visibility = View.GONE }
                            empty_data.empty_image.setImageResource(R.drawable.ic_restaurants)
                            empty_data.empty_text.text = "No Restaurant To Show"
                            TingToast(
                                context!!,
                                "No Restaurant To Show",
                                TingToastType.DEFAULT
                            ).showToast(Toast.LENGTH_LONG)
                        }
                    }
                } catch (e: Exception) {}
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
                activity.runOnUiThread {}
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body!!.string()
                val cuisines = gson.fromJson<MutableList<RestaurantCategory>>(dataString, object : TypeToken<MutableList<RestaurantCategory>>(){}.type)

                activity.runOnUiThread{
                    cuisinesTimer.cancel()
                    mLocalData.saveCuisines(dataString)

                    cuisines_shimmer.stopShimmer()
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

    private fun getFilters() {
        val url = Routes.restaurantFilters
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60 * 5, TimeUnit.SECONDS).build()

        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                activity.runOnUiThread {}
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body!!.string()
                activity.runOnUiThread{ mLocalData.saveFilters(dataString) }
            }
        })
    }

    private fun showFilters(filter: String, query: String, view: View) {
        val mProgressOverlay: ProgressOverlay = ProgressOverlay()
        val restaurantsFiltersFragment = RestaurantFiltersFragment()
        val bundle = Bundle()
        bundle.putString(FILTER_KEY, filter)
        restaurantsFiltersFragment.arguments = bundle
        restaurantsFiltersFragment.show(fragmentManager!!, restaurantsFiltersFragment.tag)
        restaurantsFiltersFragment.onFilterRestaurantsListener(object : FilterRestaurantsClickListener {
            override fun onFilterRestaurantsClickListener(type: String, filters: List<Int>) {
                activity.runOnUiThread {
                    restaurantsFiltersFragment.dismiss()
                    val paramsFilters = mLocalData.getParametersFilters()
                    when(type) {
                        RestaurantsFragment.AVAILABILITY_KEY -> { paramsFilters.availability = filters }
                        RestaurantsFragment.CUISINES_KEY -> { paramsFilters.cuisines = filters }
                        RestaurantsFragment.SPECIALS_KEY -> { paramsFilters.specials = filters }
                        RestaurantsFragment.SERVICES_KEY -> { paramsFilters.services = filters }
                        RestaurantsFragment.TYPES_KEY -> { paramsFilters.types = filters }
                        RestaurantsFragment.RATINGS_KEY -> { paramsFilters.ratings = filters }
                    }
                    filteredRestaurantTimer = Timer()
                    mLocalData.saveParametersFilters(gson.toJson(paramsFilters))
                    searchFilterRestaurants(gson.toJson(paramsFilters), query, view)
                    filteredRestaurantTimer = Timer()
                    filteredRestaurantTimer.scheduleAtFixedRate(object: TimerTask() {
                        override fun run() { searchFilterRestaurants(gson.toJson(mLocalData.getParametersFilters()), query, view) }
                    }, TIMER_PERIOD, TIMER_PERIOD)
                    mProgressOverlay.show(fragmentManager!!, mProgressOverlay.tag)
                }
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun searchFilterRestaurants(filters: String, query: String, view: View) {
        val url = Routes.restaurantsSearchFiltered
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60 * 5, TimeUnit.SECONDS)
            .build()

        val form = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("country", country)
            .addFormDataPart("town", town)
            .addFormDataPart("query", query)
            .addFormDataPart("filters", filters)
            .build()

        val request = Request.Builder()
            .header("Authorization", session.token!!)
            .url(url)
            .post(form)
            .build()

        val mProgressOverlay: ProgressOverlay = ProgressOverlay()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                activity.runOnUiThread {
                    refresh_restaurants.isRefreshing = false
                    restaurants_recycler_view.visibility = View.GONE
                    progress_loader.visibility = View.GONE
                    empty_data.visibility = View.VISIBLE
                    mProgressOverlay.dismiss()

                    if (shimmer_loader != null) { shimmer_loader.visibility = View.GONE }
                    empty_data.empty_image.setImageResource(R.drawable.ic_restaurants)
                    empty_data.empty_text.text = "No Restaurant To Show"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body!!.string()

                activity.runOnUiThread {
                    refresh_restaurants.isRefreshing = false
                    mProgressOverlay.dismiss()

                    try {
                        filteredRestaurantTimer.cancel()
                        restaurants = gson.fromJson<MutableList<Branch>>(dataString, object : TypeToken<MutableList<Branch>>(){}.type)
                        if(!restaurants.isNullOrEmpty()){ showRestaurantWithCurrentAddress(2, view) }
                        else {
                            restaurants_recycler_view.visibility = View.GONE
                            progress_loader.visibility = View.GONE
                            empty_data.visibility = View.VISIBLE

                            if (shimmer_loader != null) { shimmer_loader.visibility = View.GONE }
                            empty_data.empty_image.setImageResource(R.drawable.ic_restaurants)
                            empty_data.empty_text.text = "No Restaurant To Show"
                            TingToast(
                                context!!,
                                "No Restaurant To Show",
                                TingToastType.DEFAULT
                            ).showToast(Toast.LENGTH_LONG)
                        }
                    } catch (e: Exception) { TingToast(
                        context!!,
                        e.localizedMessage,
                        TingToastType.ERROR
                    ).showToast(Toast.LENGTH_LONG) }
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
            cuisinesTimer.cancel()
            restaurantsTimer.cancel()
            filteredRestaurantTimer.cancel()
        } catch (e: Exception) {}
        try { statusWorkTimer.cancel() } catch (e: Exception) {}
        Bridge.clear(this)
    }

    override fun onPause() {
        super.onPause()
        try {
            cuisinesTimer.cancel()
            restaurantsTimer.cancel()
            filteredRestaurantTimer.cancel()
        } catch (e: Exception) {}
        try { statusWorkTimer.cancel() } catch (e: Exception) {}
    }

    override fun onDetach() {
        super.onDetach()
        try {
            cuisinesTimer.cancel()
            restaurantsTimer.cancel()
            filteredRestaurantTimer.cancel()
        } catch (e: Exception) {}
        try { statusWorkTimer.cancel() } catch (e: Exception) {}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            cuisinesTimer.cancel()
            restaurantsTimer.cancel()
            filteredRestaurantTimer.cancel()
        } catch (e: Exception) {}
        try { statusWorkTimer.cancel() } catch (e: Exception) {}
        Bridge.clear(this)
        Bridge.clearAll(context!!)
    }

    companion object {
        public const val FILTER_KEY           = "filter"
        public const val AVAILABILITY_KEY     = "availability"
        public const val CUISINES_KEY         = "cuisines"
        public const val SERVICES_KEY         = "services"
        public const val SPECIALS_KEY         = "specials"
        public const val TYPES_KEY            = "types"
        public const val RATINGS_KEY          = "ratings"
        private const val TIMER_PERIOD  = 10000.toLong()
    }
}