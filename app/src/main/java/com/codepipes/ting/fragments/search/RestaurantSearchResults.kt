package com.codepipes.ting.fragments.search


import android.Manifest
import android.annotation.SuppressLint
import android.location.Geocoder
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.codepipes.ting.R
import com.codepipes.ting.abstracts.EndlessScrollEventListener
import com.codepipes.ting.adapters.cuisine.CuisineRestaurantsAdapter
import com.codepipes.ting.dialogs.messages.TingToast
import com.codepipes.ting.dialogs.messages.TingToastType
import com.codepipes.ting.models.Branch
import com.codepipes.ting.models.User
import com.codepipes.ting.providers.LocalData
import com.codepipes.ting.providers.TingClient
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.UtilsFunctions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.livefront.bridge.Bridge
import kotlinx.android.synthetic.main.fragment_restaurant_search_results.view.*
import kotlinx.android.synthetic.main.include_empty_data.view.*
import okhttp3.Interceptor
import java.lang.Exception
import java.util.*


class RestaurantSearchResults : Fragment() {

    private lateinit var mUtilFunctions: UtilsFunctions
    private lateinit var mLocalData: LocalData

    private lateinit var session: User
    private lateinit var userAuthentication: UserAuthentication

    private lateinit var cuisineRestaurantsTimer: Timer

    private var query: String = ""
    private var country: String = ""
    private var town: String = ""

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        Bridge.restoreInstanceState(this, savedInstanceState)
        savedInstanceState?.clear()

        val view = inflater.inflate(R.layout.fragment_restaurant_search_results, container, false)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context!!)
        mUtilFunctions = UtilsFunctions(context!!)
        mLocalData = LocalData(context!!)

        userAuthentication = UserAuthentication(context!!)
        session = userAuthentication.get()!!
        cuisineRestaurantsTimer = Timer()

        query = arguments?.getString("query") ?: ""
        country = mLocalData.getUserCountry() ?: session.country
        town = mLocalData.getUserTown() ?: session.town

        view.shimmer_loader.startShimmer()
        cuisineRestaurantsTimer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() { loadRestaurants(view) }
        }, TIMER_PERIOD, TIMER_PERIOD)
        this.loadRestaurants(view)

        view.refresh_search_results_restaurants.setColorSchemeColors(context!!.resources.getColor(R.color.colorPrimary), context!!.resources.getColor(R.color.colorAccentMain), context!!.resources.getColor(R.color.colorPrimaryDark), context!!.resources.getColor(R.color.colorAccentMain))
        view.refresh_search_results_restaurants.setOnRefreshListener {
            view.refresh_search_results_restaurants.isRefreshing = true
            this.loadRestaurants(view)
        }

        return view
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun loadRestaurants(view: View) {

        val interceptor = Interceptor {
            val url = it.request().url.newBuilder()
                .addQueryParameter("query", query)
                .addQueryParameter("country", country)
                .addQueryParameter("town", town)
                .build()
            val request = it.request().newBuilder()
                .header("Authorization", session.token!!)
                .url(url)
                .build()
            it.proceed(request)
        }

        TingClient.getRequest(Routes.restaurantSearchResults, interceptor, session.token) { _, isSuccess, result ->
            activity?.runOnUiThread {
                if(isSuccess) {
                    try {
                        val branches = Gson().fromJson<MutableList<Branch>>(result, object : TypeToken<MutableList<Branch>>(){}.type)
                        cuisineRestaurantsTimer.cancel()
                        if (branches.size > 0) {
                            view.shimmer_loader.stopShimmer()
                            view.shimmer_loader.visibility = View.GONE

                            view.search_result_restaurants.visibility = View.VISIBLE
                            view.empty_data.visibility = View.GONE

                            view.refresh_search_results_restaurants.isRefreshing = false

                            val linearLayoutManager = LinearLayoutManager(context)
                            var cuisineRestaurantsAdapter = CuisineRestaurantsAdapter(branches, fragmentManager!!)

                            if(mUtilFunctions.checkLocationPermissions()){
                                try {
                                    fusedLocationProviderClient.lastLocation.addOnSuccessListener {
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
                                        branches.sortBy { b -> b.dist }
                                        cuisineRestaurantsAdapter = CuisineRestaurantsAdapter(branches, fragmentManager!!)
                                        view.search_result_restaurants.layoutManager = linearLayoutManager
                                        view.search_result_restaurants.adapter = cuisineRestaurantsAdapter
                                    }.addOnFailureListener {
                                        val from = LatLng(session.addresses!!.addresses[0].latitude, session.addresses!!.addresses[0].longitude)
                                        branches.forEach { b ->
                                            val to = LatLng(b.latitude, b.longitude)
                                            val dist = mUtilFunctions.calculateDistance(from, to)
                                            b.dist = dist
                                            b.fromLocation = from
                                        }
                                        cuisineRestaurantsAdapter = CuisineRestaurantsAdapter(branches, fragmentManager!!)
                                        view.search_result_restaurants.layoutManager = linearLayoutManager
                                        view.search_result_restaurants.adapter = cuisineRestaurantsAdapter
                                        TingToast(
                                            context!!,
                                            it.message!!.capitalize(),
                                            TingToastType.ERROR
                                        ).showToast(Toast.LENGTH_LONG)
                                    }

                                    ViewCompat.setNestedScrollingEnabled(view.search_result_restaurants, false)

                                    val endlessScrollEventListener = object : EndlessScrollEventListener(linearLayoutManager) {
                                        override fun onLoadMore(pageNum: Int, recyclerView: RecyclerView?) {
                                            val interceptorScrollEvent = Interceptor {
                                                val url = it.request().url.newBuilder()
                                                    .addQueryParameter("query", query)
                                                    .addQueryParameter("country", country)
                                                    .addQueryParameter("town", town)
                                                    .addQueryParameter("page", "${pageNum + 1}")
                                                    .build()
                                                val request = it.request().newBuilder()
                                                    .header("Authorization", session.token!!)
                                                    .url(url)
                                                    .build()
                                                it.proceed(request)
                                            }
                                            TingClient.getRequest(Routes.restaurantSearchResults, interceptorScrollEvent, session.token) { _, isSuccess, result ->
                                                activity?.runOnUiThread {
                                                    if (isSuccess) {
                                                        try {
                                                            val restosResultPage =
                                                                Gson().fromJson<MutableList<Branch>>(
                                                                    result,
                                                                    object :
                                                                        TypeToken<MutableList<Branch>>() {}.type
                                                                )
                                                            try {
                                                                fusedLocationProviderClient.lastLocation.addOnSuccessListener {
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
                                                                    cuisineRestaurantsAdapter.addItems(restosResultPage)
                                                                }.addOnFailureListener {
                                                                    val from = LatLng(session.addresses!!.addresses[0].latitude, session.addresses!!.addresses[0].longitude)
                                                                    restosResultPage.forEach { b ->
                                                                        val to = LatLng(b.latitude, b.longitude)
                                                                        val dist = mUtilFunctions.calculateDistance(from, to)
                                                                        b.dist = dist
                                                                        b.fromLocation = from
                                                                    }
                                                                    restosResultPage.sortBy { b -> b.dist }
                                                                    cuisineRestaurantsAdapter.addItems(restosResultPage)
                                                                }
                                                            } catch (e: Exception) {}

                                                        } catch (e: Exception) { }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    view.search_result_restaurants.addOnScrollListener(endlessScrollEventListener)

                                } catch (e: Exception){ TingToast(
                                    context!!,
                                    e.message!!.capitalize(),
                                    TingToastType.ERROR
                                ).showToast(Toast.LENGTH_LONG) }
                            }
                        } else {
                            view.shimmer_loader.stopShimmer()
                            view.shimmer_loader.visibility = View.GONE

                            view.search_result_restaurants.visibility = View.GONE
                            view.empty_data.visibility = View.VISIBLE

                            view.refresh_search_results_restaurants.isRefreshing = false

                            view.empty_data.empty_image.setImageResource(R.drawable.ic_search)
                            view.empty_data.empty_text.text = "No Restaurant To Show"
                        }
                    } catch (e: Exception) {
                        cuisineRestaurantsTimer.cancel()

                        view.shimmer_loader.stopShimmer()
                        view.shimmer_loader.visibility = View.GONE

                        view.search_result_restaurants.visibility = View.GONE
                        view.empty_data.visibility = View.VISIBLE

                        view.refresh_search_results_restaurants.isRefreshing = false

                        view.empty_data.empty_image.setImageResource(R.drawable.ic_search)
                        view.empty_data.empty_text.text = "No Restaurant To Show"
                    }
                } else {
                    cuisineRestaurantsTimer.cancel()

                    view.shimmer_loader.stopShimmer()
                    view.shimmer_loader.visibility = View.GONE

                    view.search_result_restaurants.visibility = View.GONE
                    view.empty_data.visibility = View.VISIBLE

                    view.refresh_search_results_restaurants.isRefreshing = false

                    view.empty_data.empty_image.setImageResource(R.drawable.ic_search)
                    view.empty_data.empty_text.text = "No Restaurant To Show"
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Bridge.saveInstanceState(this, outState)
        outState.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        try { cuisineRestaurantsTimer.cancel() } catch (e: Exception) {}
        Bridge.clear(this)
    }

    override fun onPause() {
        super.onPause()
        try { cuisineRestaurantsTimer.cancel() } catch (e: Exception) {}
    }

    override fun onDetach() {
        super.onDetach()
        try { cuisineRestaurantsTimer.cancel() } catch (e: Exception) {}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try { cuisineRestaurantsTimer.cancel() } catch (e: Exception) {}
    }

    companion object {

        private const val REQUEST_FINE_LOCATION = 2
        private const val TIMER_PERIOD = 6000.toLong()

        fun newInstance(query: String) =
            RestaurantSearchResults().apply {
                arguments = Bundle().apply {
                    putString("query", query)
                }
            }
    }
}
