package com.codepipes.ting.fragments.cuisine


import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import com.codepipes.ting.R
import com.codepipes.ting.abstracts.EndlessScrollEventListener
import com.codepipes.ting.adapters.cuisine.CuisineRestaurantsAdapter
import com.codepipes.ting.dialogs.messages.TingToast
import com.codepipes.ting.dialogs.messages.TingToastType
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.livefront.bridge.Bridge
import kotlinx.android.synthetic.main.fragment_cuisine_restaurants.view.*
import kotlinx.android.synthetic.main.include_empty_data.view.*
import okhttp3.*
import java.io.IOException
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit

class CuisineRestaurantsFragment : Fragment() {

    private lateinit var cuisine: RestaurantCategory

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mUtilFunctions: UtilsFunctions
    private lateinit var mLocalData: LocalData

    private lateinit var session: User
    private lateinit var userAuthentication: UserAuthentication

    private lateinit var cuisineRestaurantsTimer: Timer
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
        val view = inflater.inflate(R.layout.fragment_cuisine_restaurants, container, false)
        cuisine = Gson().fromJson(arguments?.getString("cuisine"), RestaurantCategory::class.java)

        val activity = context as Activity

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
        mUtilFunctions = UtilsFunctions(context!!)
        mLocalData = LocalData(context!!)

        userAuthentication = UserAuthentication(context!!)
        session = userAuthentication.get()!!
        cuisineRestaurantsTimer = Timer()

        view.shimmer_loader.startShimmer()
        cuisineRestaurantsTimer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() { loadRestaurants(view) }
        }, TIMER_PERIOD, TIMER_PERIOD)
        this.loadRestaurants(view)

        view.refresh_cuisine_restaurants.setColorSchemeColors(context!!.resources.getColor(R.color.colorPrimary), context!!.resources.getColor(R.color.colorAccentMain), context!!.resources.getColor(R.color.colorPrimaryDark), context!!.resources.getColor(R.color.colorAccentMain))
        view.refresh_cuisine_restaurants.setOnRefreshListener {
            view.refresh_cuisine_restaurants.isRefreshing = true
            this.loadRestaurants(view)
        }

        return view
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun loadRestaurants(view: View) {
        val url = "${Routes.cuisineRestaurants}${cuisine.id}/"

        TingClient.getRequest(url, null, session.token) { _, isSuccess, result ->
            activity?.runOnUiThread {
                if(isSuccess) {
                    try {
                        val branches = Gson().fromJson<MutableList<Branch>>(result, object : TypeToken<MutableList<Branch>>(){}.type)
                        cuisineRestaurantsTimer.cancel()
                        if (branches.size > 0) {
                            view.shimmer_loader.stopShimmer()
                            view.shimmer_loader.visibility = View.GONE

                            view.cuisine_restaurants.visibility = View.VISIBLE
                            view.empty_data.visibility = View.GONE

                            view.refresh_cuisine_restaurants.isRefreshing = false

                            val linearLayoutManager = LinearLayoutManager(context)
                            var cuisineRestaurantsAdapter = CuisineRestaurantsAdapter(branches, fragmentManager!!)

                            if(mUtilFunctions.checkLocationPermissions()){
                                try {
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
                                        branches.sortBy { b -> b.dist }
                                        cuisineRestaurantsAdapter = CuisineRestaurantsAdapter(branches, fragmentManager!!)
                                        view.cuisine_restaurants.layoutManager = linearLayoutManager
                                        view.cuisine_restaurants.adapter = cuisineRestaurantsAdapter
                                    }.addOnFailureListener {
                                        val from = LatLng(session.addresses!!.addresses[0].latitude, session.addresses!!.addresses[0].longitude)
                                        branches.forEach { b ->
                                            val to = LatLng(b.latitude, b.longitude)
                                            val dist = mUtilFunctions.calculateDistance(from, to)
                                            b.dist = dist
                                            b.fromLocation = from
                                        }
                                        cuisineRestaurantsAdapter = CuisineRestaurantsAdapter(branches, fragmentManager!!)
                                        view.cuisine_restaurants.layoutManager = linearLayoutManager
                                        view.cuisine_restaurants.adapter = cuisineRestaurantsAdapter
                                        TingToast(
                                            context!!,
                                            it.message!!.capitalize(),
                                            TingToastType.ERROR
                                        ).showToast(Toast.LENGTH_LONG)
                                    }

                                    ViewCompat.setNestedScrollingEnabled(view.cuisine_restaurants, false)

                                    val endlessScrollEventListener = object : EndlessScrollEventListener(linearLayoutManager) {
                                        override fun onLoadMore(pageNum: Int, recyclerView: RecyclerView?) {
                                            val urlPage = "${Routes.cuisineRestaurants}${cuisine.id}/?page=${pageNum + 1}"
                                            TingClient.getRequest(urlPage, null, session.token) { _, isSuccess, result ->
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

                                    view.cuisine_restaurants.addOnScrollListener(endlessScrollEventListener)

                                } catch (e: Exception){ TingToast(
                                    context!!,
                                    e.message!!.capitalize(),
                                    TingToastType.ERROR
                                ).showToast(Toast.LENGTH_LONG) }
                            }
                        } else {
                            view.shimmer_loader.stopShimmer()
                            view.shimmer_loader.visibility = View.GONE

                            view.cuisine_restaurants.visibility = View.GONE
                            view.empty_data.visibility = View.VISIBLE

                            view.refresh_cuisine_restaurants.isRefreshing = false

                            view.empty_data.empty_image.setImageResource(R.drawable.ic_restaurants)
                            view.empty_data.empty_text.text = "No Restaurant To Show"
                        }
                    } catch (e: Exception) {
                        cuisineRestaurantsTimer.cancel()

                        view.shimmer_loader.stopShimmer()
                        view.shimmer_loader.visibility = View.GONE

                        view.cuisine_restaurants.visibility = View.GONE
                        view.empty_data.visibility = View.VISIBLE

                        view.refresh_cuisine_restaurants.isRefreshing = false

                        view.empty_data.empty_image.setImageResource(R.drawable.ic_restaurants)
                        view.empty_data.empty_text.text = "No Restaurant To Show"
                    }
                } else {
                    cuisineRestaurantsTimer.cancel()

                    view.shimmer_loader.stopShimmer()
                    view.shimmer_loader.visibility = View.GONE

                    view.cuisine_restaurants.visibility = View.GONE
                    view.empty_data.visibility = View.VISIBLE

                    view.refresh_cuisine_restaurants.isRefreshing = false

                    view.empty_data.empty_image.setImageResource(R.drawable.ic_restaurants)
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

        fun newInstance(cuisine: String) =
            CuisineRestaurantsFragment().apply {
                arguments = Bundle().apply {
                    putString("cuisine", cuisine)
                }
            }
    }
}
