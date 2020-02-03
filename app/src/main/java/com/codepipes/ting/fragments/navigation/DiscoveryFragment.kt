package com.codepipes.ting.fragments.navigation


import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import com.codepipes.ting.R
import com.codepipes.ting.RestaurantProfile
import com.codepipes.ting.adapters.cuisine.CuisinesAdapter
import com.codepipes.ting.carouselview.enums.IndicatorAnimationType
import com.codepipes.ting.carouselview.enums.OffsetType
import com.codepipes.ting.dialogs.TingToast
import com.codepipes.ting.dialogs.TingToastType
import com.codepipes.ting.models.Branch
import com.codepipes.ting.models.RestaurantCategory
import com.codepipes.ting.models.User
import com.codepipes.ting.providers.LocalData
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.utils.Routes
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.livefront.bridge.Bridge
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_discovery.*
import kotlinx.android.synthetic.main.fragment_discovery.view.*
import kotlinx.android.synthetic.main.row_discover_restaurant.view.*
import okhttp3.*
import java.io.IOException
import java.lang.Exception
import java.util.concurrent.TimeUnit


class DiscoveryFragment : Fragment() {

    private lateinit var session: User
    private lateinit var userAuthentication: UserAuthentication
    private lateinit var mLocalData: LocalData

    private lateinit var gson: Gson
    private lateinit var routes: Routes

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

        gson = Gson()
        routes = Routes()

        view.discover_restaurants_shimmer.startShimmer()
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
        } else { this.getCuisines() }

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
                activity!!.runOnUiThread {
                    TingToast(context!!, e.message!!.capitalize(), TingToastType.ERROR).showToast(
                        Toast.LENGTH_LONG)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body()!!.string()
                val restaurants = gson.fromJson<MutableList<Branch>>(dataString, object : TypeToken<MutableList<Branch>>(){}.type)

                activity!!.runOnUiThread{
                    try {
                        view.discover_restaurants_shimmer.stopShimmer()
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
                    } catch (e: Exception) {}
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

                activity!!.runOnUiThread{
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

    private fun getDiscoverTodayPromotions(view: View) {

    }

    private fun getDiscoverReviewedMenus(view: View) {

    }

    private fun getDiscoverMenus(view: View) {

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Bridge.saveInstanceState(this, outState)
        outState.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        Bridge.clear(this)
    }
}
