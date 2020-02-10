package com.codepipes.ting.fragments.navigation


import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codepipes.ting.R
import com.codepipes.ting.fragments.restaurants.RestaurantsMapFragment
import android.annotation.SuppressLint
import android.app.Activity
import android.location.Geocoder
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.Toast
import com.codepipes.ting.adapters.cuisine.CuisinesAdapter
import com.codepipes.ting.adapters.restaurant.GlobalRestaurantAdapter
import com.codepipes.ting.customclasses.ActionSheet
import com.codepipes.ting.dialogs.ProgressOverlay
import com.codepipes.ting.dialogs.TingToast
import com.codepipes.ting.dialogs.TingToastType
import com.codepipes.ting.fragments.restaurants.RestaurantFiltersFragment
import com.codepipes.ting.interfaces.ActionSheetCallBack
import com.codepipes.ting.interfaces.FilterRestaurantsClickListener
import com.codepipes.ting.models.Branch
import com.codepipes.ting.models.RestaurantCategory
import com.codepipes.ting.models.User
import com.codepipes.ting.providers.APILoadGlobalRestaurants
import com.codepipes.ting.providers.LocalData
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.UtilsFunctions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
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


class RestaurantsFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var mOpenRestaurantMapButton: FloatingActionButton
    private lateinit var mRestaurantsRecyclerView: RecyclerView
    private lateinit var mProgressLoader: View
    private lateinit var mEmptyDataView: View
    private lateinit var mRefreshRestaurant: SwipeRefreshLayout

    private lateinit var activity: Activity

    private val routes: Routes = Routes()
    private val gson: Gson = Gson()

    private var isAsync: Boolean = false
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

    private val TIMER_PERIOD = 10000.toLong()

    private val mProgressOverlay: ProgressOverlay = ProgressOverlay()

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

        country = session.country
        town = session.town

        if(mUtilFunctions.checkLocationPermissions()){
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener {
                    if(it != null){
                        val geocoder = Geocoder(activity, Locale.getDefault())
                        activity.runOnUiThread {
                            try {
                                val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                                country = addresses[0].countryName
                                town = addresses[0].locality
                            } catch (e: Exception) {}
                        }
                    }
                }
            } catch (e: Exception){ }
        }

        this.getFilters()
        this.getRestaurants()

        cuisinesTimer = Timer()
        restaurantsTimer = Timer()
        filteredRestaurantTimer = Timer()

        mOpenRestaurantMapButton = view.findViewById(R.id.open_restaurant_map) as FloatingActionButton
        mOpenRestaurantMapButton.setOnClickListener {
            val cx = (mOpenRestaurantMapButton.x + mOpenRestaurantMapButton.width / 2).toInt()
            val cy = (mOpenRestaurantMapButton.y + mOpenRestaurantMapButton.height + 56).toInt()

            val mapFragment =  RestaurantsMapFragment()
            val args: Bundle = Bundle()

            args.putInt("cx", cx)
            args.putInt("cy", cy)
            args.putDouble("lat", selectedLatitude)
            args.putDouble("lng", selectedLongitude)

            if(!restaurants.isNullOrEmpty()){
                args.putString("restos", gson.toJson(restaurants))
            }

            mapFragment.arguments = args
            mapFragment.show(fragmentManager!!, mapFragment.tag)
        }

        mOpenRestaurantMapButton.isClickable = false

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

        mOpenRestaurantMapButton.setOnLongClickListener {
            val actionSheet = ActionSheet(context!!, menuList)
                .setTitle("Restaurant Near Location")
                .setColorData(activity.resources.getColor(R.color.colorGray))
                .setColorTitleCancel(activity.resources.getColor(R.color.colorGoogleRedTwo))
                .setColorSelected(activity.resources.getColor(R.color.colorPrimary))
                .setCancelTitle("Cancel")

            actionSheet.create(object : ActionSheetCallBack {

                override fun data(data: String, position: Int) {
                    if(!restaurants.isNullOrEmpty()){
                        if(position == 0){ showRestaurantWithCurrentAddress() }
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
                            mRestaurantsRecyclerView.layoutManager = LinearLayoutManager(context)
                            mRestaurantsRecyclerView.adapter = GlobalRestaurantAdapter(restaurants, fragmentManager!!)
                        }
                    }
                }
            })

            return@setOnLongClickListener true
        }

        mRestaurantsRecyclerView = view.findViewById<RecyclerView>(R.id.restaurants_recycler_view) as RecyclerView
        mProgressLoader = view.findViewById<View>(R.id.progress_loader) as View
        mEmptyDataView = view.findViewById<View>(R.id.empty_data) as View
        mRefreshRestaurant = view.findViewById<SwipeRefreshLayout>(R.id.refresh_restaurants) as SwipeRefreshLayout

        mRefreshRestaurant.setColorSchemeColors(context!!.resources.getColor(R.color.colorPrimary), context!!.resources.getColor(R.color.colorAccentMain), context!!.resources.getColor(R.color.colorPrimaryDark), context!!.resources.getColor(R.color.colorAccentMain))
        mRefreshRestaurant.setOnRefreshListener(this)

        if(isAsync){
            val asyncTask = APILoadGlobalRestaurants(context!!).execute()
            val restaurants = asyncTask.get()

            if(!restaurants.isNullOrEmpty()){
                mOpenRestaurantMapButton.isClickable = false
                mRestaurantsRecyclerView.visibility = View.VISIBLE
                mProgressLoader.visibility = View.GONE
                mEmptyDataView.visibility = View.GONE

                mRestaurantsRecyclerView.layoutManager = LinearLayoutManager(context)
                mRestaurantsRecyclerView.adapter = GlobalRestaurantAdapter(restaurants, fragmentManager!!)
            } else {
                mOpenRestaurantMapButton.isClickable = false
                mRestaurantsRecyclerView.visibility = View.GONE
                mProgressLoader.visibility = View.GONE
                mEmptyDataView.visibility = View.VISIBLE

                mEmptyDataView.empty_image.setImageResource(R.drawable.ic_restaurants)
                mEmptyDataView.empty_text.text = "No Restaurant To Show"
                TingToast(context!!, "No Restaurant To Show", TingToastType.DEFAULT).showToast(Toast.LENGTH_LONG)
            }
        }

        restaurantsTimer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() { getRestaurants() }
        }, TIMER_PERIOD, TIMER_PERIOD)

        val query = view.search_restaurant_input.text.toString()

        view.filter_distance.setOnClickListener { TingToast(context!!, "Not Available For Now", TingToastType.DEFAULT).showToast(Toast.LENGTH_LONG) }
        view.filter_availability.setOnClickListener { showFilters(AVAILABILITY_KEY, query) }
        view.filter_cuisines.setOnClickListener { showFilters(CUISINES_KEY, query) }
        view.filter_services.setOnClickListener { showFilters(SERVICES_KEY, query) }
        view.filter_specials.setOnClickListener { showFilters(SPECIALS_KEY, query) }
        view.filter_types.setOnClickListener { showFilters(TYPES_KEY, query) }
        view.filter_ratings.setOnClickListener { showFilters(RATINGS_KEY, query) }

        view.filter_restaurant_button.setOnClickListener {
            searchFilterRestaurants(gson.toJson(mLocalData.getParametersFilters()), query)
            filteredRestaurantTimer = Timer()
            filteredRestaurantTimer.scheduleAtFixedRate(object: TimerTask() {
                override fun run() { searchFilterRestaurants(gson.toJson(mLocalData.getParametersFilters()), query) }
            }, TIMER_PERIOD, TIMER_PERIOD)
            mProgressOverlay.show(activity.fragmentManager, mProgressOverlay.tag)
        }

        view.filter_restaurant_button.setOnLongClickListener {
            val actionSheet = ActionSheet(context!!, mutableListOf("Reset All Filters"))
                .setTitle("Reset Filters")
                .setColorData(activity.resources.getColor(R.color.colorGray))
                .setColorTitleCancel(activity.resources.getColor(R.color.colorGoogleRedTwo))
                .setColorSelected(activity.resources.getColor(R.color.colorPrimary))
                .setCancelTitle("Cancel")

            actionSheet.create(object : ActionSheetCallBack {
                override fun data(data: String, position: Int) {
                    activity.runOnUiThread { mLocalData.saveParametersFilters(null) }
                }
            })

            return@setOnLongClickListener true
        }

        return view
    }

    @SuppressLint("DefaultLocale")
    private fun showRestaurantWithCurrentAddress() {

        mRestaurantsRecyclerView.visibility = View.VISIBLE
        mProgressLoader.visibility = View.GONE
        mEmptyDataView.visibility = View.GONE

        if (shimmer_loader != null) { shimmer_loader.visibility = View.GONE }

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
                    mRestaurantsRecyclerView.layoutManager = LinearLayoutManager(context)
                    mRestaurantsRecyclerView.adapter = GlobalRestaurantAdapter(restaurants, fragmentManager!!)
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
                    mRestaurantsRecyclerView.layoutManager = LinearLayoutManager(context)
                    mRestaurantsRecyclerView.adapter = GlobalRestaurantAdapter(restaurants,fragmentManager!!)
                    TingToast(context!!, it.message!!, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                }
            } catch (e: Exception){ TingToast(context!!, e.message!!.capitalize(), TingToastType.ERROR).showToast(Toast.LENGTH_LONG) }
        }
    }

    inner class LocationInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val url = chain.request().url().newBuilder()
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
    private fun getRestaurants(){
        val url = routes.restaurantsGlobal
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
                    mRefreshRestaurant.isRefreshing = false
                    mRestaurantsRecyclerView.visibility = View.GONE
                    mProgressLoader.visibility = View.GONE
                    mEmptyDataView.visibility = View.VISIBLE

                    if (shimmer_loader != null) { shimmer_loader.visibility = View.GONE }
                    mEmptyDataView.empty_image.setImageResource(R.drawable.ic_restaurants)
                    mEmptyDataView.empty_text.text = "No Restaurant To Show"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body()!!.string()
                restaurants = gson.fromJson<MutableList<Branch>>(dataString, object : TypeToken<MutableList<Branch>>(){}.type)

                activity.runOnUiThread {
                    restaurantsTimer.cancel()
                    mRefreshRestaurant.isRefreshing = false
                    mLocalData.saveRestaurants(dataString)

                    if(!restaurants.isNullOrEmpty()){ showRestaurantWithCurrentAddress() }
                    else {
                        mRestaurantsRecyclerView.visibility = View.GONE
                        mProgressLoader.visibility = View.GONE
                        mEmptyDataView.visibility = View.VISIBLE

                        if (shimmer_loader != null) { shimmer_loader.visibility = View.GONE }
                        mEmptyDataView.empty_image.setImageResource(R.drawable.ic_restaurants)
                        mEmptyDataView.empty_text.text = "No Restaurant To Show"
                        TingToast(context!!, "No Restaurant To Show", TingToastType.DEFAULT).showToast(Toast.LENGTH_LONG)
                    }
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
                activity.runOnUiThread {}
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body()!!.string()
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
        val url = routes.restaurantFilters
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
                val dataString = response.body()!!.string()
                activity.runOnUiThread{ mLocalData.saveFilters(dataString) }
            }
        })
    }

    override fun onRefresh() {
        mRefreshRestaurant.isRefreshing = true
        this.getRestaurants()
    }

    private fun showFilters(filter: String, query: String) {
        val restaurantsFiltersFragment = RestaurantFiltersFragment()
        val bundle = Bundle()
        bundle.putString(FILTER_KEY, filter)
        restaurantsFiltersFragment.arguments = bundle
        restaurantsFiltersFragment.show(fragmentManager, restaurantsFiltersFragment.tag)
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
                    searchFilterRestaurants(gson.toJson(paramsFilters), query)
                    filteredRestaurantTimer = Timer()
                    filteredRestaurantTimer.scheduleAtFixedRate(object: TimerTask() {
                        override fun run() { searchFilterRestaurants(gson.toJson(mLocalData.getParametersFilters()), query) }
                    }, TIMER_PERIOD, TIMER_PERIOD)
                    mProgressOverlay.show(activity.fragmentManager, mProgressOverlay.tag)
                }
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun searchFilterRestaurants(filters: String, query: String) {
        val url = routes.restaurantsSearchFiltered
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

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                activity.runOnUiThread {
                    mRefreshRestaurant.isRefreshing = false
                    mRestaurantsRecyclerView.visibility = View.GONE
                    mProgressLoader.visibility = View.GONE
                    mEmptyDataView.visibility = View.VISIBLE
                    mProgressOverlay.dismiss()

                    if (shimmer_loader != null) { shimmer_loader.visibility = View.GONE }
                    mEmptyDataView.empty_image.setImageResource(R.drawable.ic_restaurants)
                    mEmptyDataView.empty_text.text = "No Restaurant To Show"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body()!!.string()

                activity.runOnUiThread {
                    mRefreshRestaurant.isRefreshing = false
                    mProgressOverlay.dismiss()

                    try {
                        filteredRestaurantTimer.cancel()
                        restaurants = gson.fromJson<MutableList<Branch>>(dataString, object : TypeToken<MutableList<Branch>>(){}.type)
                        if(!restaurants.isNullOrEmpty()){ showRestaurantWithCurrentAddress() }
                        else {
                            mRestaurantsRecyclerView.visibility = View.GONE
                            mProgressLoader.visibility = View.GONE
                            mEmptyDataView.visibility = View.VISIBLE

                            if (shimmer_loader != null) { shimmer_loader.visibility = View.GONE }
                            mEmptyDataView.empty_image.setImageResource(R.drawable.ic_restaurants)
                            mEmptyDataView.empty_text.text = "No Restaurant To Show"
                            TingToast(context!!, "No Restaurant To Show", TingToastType.DEFAULT).showToast(Toast.LENGTH_LONG)
                        }
                    } catch (e: Exception) { TingToast(context!!, e.localizedMessage,TingToastType.ERROR).showToast(Toast.LENGTH_LONG) }
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
        } catch (e: Exception) {}
        Bridge.clear(this)
    }

    override fun onPause() {
        super.onPause()
        try {
            cuisinesTimer.cancel()
            restaurantsTimer.cancel()
        } catch (e: Exception) {}
    }

    override fun onDetach() {
        super.onDetach()
        try {
            cuisinesTimer.cancel()
            restaurantsTimer.cancel()
        } catch (e: Exception) {}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            cuisinesTimer.cancel()
            restaurantsTimer.cancel()
        } catch (e: Exception) {}
    }

    companion object {
        public val FILTER_KEY           = "filter"
        public val AVAILABILITY_KEY     = "availability"
        public val CUISINES_KEY         = "cuisines"
        public val SERVICES_KEY         = "services"
        public val SPECIALS_KEY         = "specials"
        public val TYPES_KEY            = "types"
        public val RATINGS_KEY          = "ratings"
    }
}