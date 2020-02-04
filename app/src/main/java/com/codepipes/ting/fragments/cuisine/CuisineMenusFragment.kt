package com.codepipes.ting.fragments.cuisine


import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.codepipes.ting.R
import com.codepipes.ting.adapters.cuisine.CuisineMenusAdapter
import com.codepipes.ting.adapters.menu.RestaurantMenuAdapter
import com.codepipes.ting.dialogs.TingToast
import com.codepipes.ting.dialogs.TingToastType
import com.codepipes.ting.models.Branch
import com.codepipes.ting.models.RestaurantCategory
import com.codepipes.ting.models.RestaurantMenu
import com.codepipes.ting.models.User
import com.codepipes.ting.providers.LocalData
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.UtilsFunctions
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.livefront.bridge.Bridge
import kotlinx.android.synthetic.main.fragment_cuisine_menus.view.*
import kotlinx.android.synthetic.main.include_empty_data.view.*
import okhttp3.*
import java.io.IOException
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit

class CuisineMenusFragment : Fragment() {

    private lateinit var cuisine: RestaurantCategory

    private lateinit var mUtilFunctions: UtilsFunctions
    private lateinit var mLocalData: LocalData

    private lateinit var session: User
    private lateinit var userAuthentication: UserAuthentication

    private lateinit var cuisineMenusTimer: Timer
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
        val view =  inflater.inflate(R.layout.fragment_cuisine_menus, container, false)
        cuisine = Gson().fromJson(arguments?.getString("cuisine"), RestaurantCategory::class.java)

        val activity = context as Activity

        mUtilFunctions = UtilsFunctions(context!!)
        mLocalData = LocalData(context!!)
        cuisineMenusTimer = Timer()

        userAuthentication = UserAuthentication(context!!)
        session = userAuthentication.get()!!

        view.shimmer_loader.startShimmer()
        cuisineMenusTimer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() { loadMenus(view) }
        }, TIMER_PERIOD, TIMER_PERIOD)
        this.loadMenus(view)

        view.refresh_cuisine_menus.setColorSchemeColors(context!!.resources.getColor(R.color.colorPrimary), context!!.resources.getColor(R.color.colorAccentMain), context!!.resources.getColor(R.color.colorPrimaryDark), context!!.resources.getColor(R.color.colorAccentMain))
        view.refresh_cuisine_menus.setOnRefreshListener {
            view.refresh_cuisine_menus.isRefreshing = true
            this.loadMenus(view)
        }

        return view
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun loadMenus(view: View) {
        val url = "${Routes().cuisineMenus}${cuisine.id}/"
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60 * 5, TimeUnit.SECONDS).build()

        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    view.shimmer_loader.stopShimmer()
                    view.shimmer_loader.visibility = View.GONE

                    view.cuisine_menus.visibility = View.GONE
                    view.empty_data.visibility = View.VISIBLE

                    view.refresh_cuisine_menus.isRefreshing = false

                    view.empty_data.empty_image.setImageResource(R.drawable.ic_spoon_gray)
                    view.empty_data.empty_text.text = "No Menu To Show"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body()!!.string()
                try {
                    activity?.runOnUiThread {
                        val menus = Gson().fromJson<MutableList<RestaurantMenu>>(dataString, object : TypeToken<MutableList<RestaurantMenu>>(){}.type)
                        cuisineMenusTimer.cancel()
                        if (menus.size > 0) {

                            view.shimmer_loader.stopShimmer()
                            view.shimmer_loader.visibility = View.GONE

                            view.cuisine_menus.visibility = View.VISIBLE
                            view.empty_data.visibility = View.GONE

                            view.refresh_cuisine_menus.isRefreshing = false

                            view.cuisine_menus.layoutManager = LinearLayoutManager(context)
                            view.cuisine_menus.adapter = CuisineMenusAdapter(menus)
                        } else {
                            view.shimmer_loader.stopShimmer()
                            view.shimmer_loader.visibility = View.GONE

                            view.cuisine_menus.visibility = View.GONE
                            view.empty_data.visibility = View.VISIBLE

                            view.refresh_cuisine_menus.isRefreshing = false

                            view.empty_data.empty_image.setImageResource(R.drawable.ic_restaurants)
                            view.empty_data.empty_text.text = "No Menu To Show"
                        }
                    }
                } catch (e: Exception) {
                    activity?.runOnUiThread {
                        cuisineMenusTimer.cancel()

                        view.shimmer_loader.stopShimmer()
                        view.shimmer_loader.visibility = View.GONE

                        view.cuisine_menus.visibility = View.GONE
                        view.empty_data.visibility = View.VISIBLE

                        view.refresh_cuisine_menus.isRefreshing = false

                        view.empty_data.empty_image.setImageResource(R.drawable.ic_restaurants)
                        view.empty_data.empty_text.text = "No Menu To Show"
                    }
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
        try { cuisineMenusTimer.cancel() } catch (e: Exception) {}
        Bridge.clear(this)
    }

    override fun onPause() {
        super.onPause()
        try { cuisineMenusTimer.cancel() } catch (e: Exception) {}
    }

    companion object {

        fun newInstance(cuisine: String) =
            CuisineMenusFragment().apply {
                arguments = Bundle().apply {
                    putString("cuisine", cuisine)
                }
            }
    }
}
