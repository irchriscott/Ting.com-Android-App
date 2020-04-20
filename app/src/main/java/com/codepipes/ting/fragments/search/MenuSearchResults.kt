package com.codepipes.ting.fragments.search


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
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
import com.codepipes.ting.adapters.cuisine.CuisineMenusAdapter
import com.codepipes.ting.dialogs.messages.TingToast
import com.codepipes.ting.dialogs.messages.TingToastType
import com.codepipes.ting.models.RestaurantMenu
import com.codepipes.ting.models.User
import com.codepipes.ting.providers.LocalData
import com.codepipes.ting.providers.TingClient
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.UtilsFunctions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.livefront.bridge.Bridge
import kotlinx.android.synthetic.main.fragment_menu_search_results.view.*
import kotlinx.android.synthetic.main.include_empty_data.view.*
import okhttp3.Interceptor
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList


class MenuSearchResults : Fragment() {

    private lateinit var mUtilFunctions: UtilsFunctions
    private lateinit var mLocalData: LocalData

    private lateinit var session: User
    private lateinit var userAuthentication: UserAuthentication

    private lateinit var menusTimer: Timer

    private val menus: MutableList<RestaurantMenu> = ArrayList()

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

        val view = inflater.inflate(R.layout.fragment_menu_search_results, container, false)

        mUtilFunctions = UtilsFunctions(context!!)
        mLocalData = LocalData(context!!)
        menusTimer = Timer()

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context!!)

        userAuthentication = UserAuthentication(context!!)
        session = userAuthentication.get()!!

        query = arguments?.getString("query") ?: ""
        country = mLocalData.getUserCountry() ?: session.country
        town = mLocalData.getUserTown() ?: session.town

        view.shimmer_loader.startShimmer()
        menusTimer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() { loadMenus(view) }
        }, TIMER_PERIOD, TIMER_PERIOD)
        this.loadMenus(view)

        view.refresh_search_results_menus.setColorSchemeColors(context!!.resources.getColor(R.color.colorPrimary), context!!.resources.getColor(R.color.colorAccentMain), context!!.resources.getColor(R.color.colorPrimaryDark), context!!.resources.getColor(R.color.colorAccentMain))
        view.refresh_search_results_menus.setOnRefreshListener {
            view.refresh_search_results_menus.isRefreshing = true
            this.loadMenus(view)
        }

        return view
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun loadMenus(view: View) {
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
        TingClient.getRequest(Routes.menusSearchResults, interceptor, session.token) { _, isSuccess, result ->
            activity?.runOnUiThread {
                if(isSuccess) {
                    try {
                        val menusResult = Gson().fromJson<MutableList<RestaurantMenu>>(result, object : TypeToken<MutableList<RestaurantMenu>>(){}.type)
                        menusTimer.cancel()

                        menus.addAll(menusResult)
                        menus.toSet().toMutableList()

                        if (menus.distinctBy { it.id }.isNotEmpty()) {
                            view.shimmer_loader.stopShimmer()
                            view.shimmer_loader.visibility = View.GONE

                            view.search_results_menus.visibility = View.VISIBLE
                            view.empty_data.visibility = View.GONE

                            view.refresh_search_results_menus.isRefreshing = false

                            val linearLayoutManager = LinearLayoutManager(context)
                            val cuisinesMenusAdapter = CuisineMenusAdapter(menus.distinctBy { it.id }.toMutableSet())
                            view.search_results_menus.layoutManager = linearLayoutManager
                            view.search_results_menus.adapter = cuisinesMenusAdapter

                            ViewCompat.setNestedScrollingEnabled(view.search_results_menus, false)

                            val endlessScrollEventListener = object: EndlessScrollEventListener(linearLayoutManager) {
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
                                    TingClient.getRequest(Routes.menusSearchResults, interceptorScrollEvent, session.token) { _, isSuccess, result ->
                                        activity?.runOnUiThread {
                                            if (isSuccess) {
                                                try {
                                                    val menusResultPage =
                                                        Gson().fromJson<MutableList<RestaurantMenu>>(
                                                            result,
                                                            object :
                                                                TypeToken<MutableList<RestaurantMenu>>() {}.type
                                                        )

                                                    menus.addAll(menusResultPage)
                                                    menus.distinctBy { it.id }.toMutableList()
                                                    cuisinesMenusAdapter.addItems(menusResultPage)
                                                } catch (e: Exception) { }
                                            }
                                        }
                                    }
                                }
                            }

                            view.search_results_menus.addOnScrollListener(endlessScrollEventListener)

                        } else {
                            view.shimmer_loader.stopShimmer()
                            view.shimmer_loader.visibility = View.GONE

                            view.search_results_menus.visibility = View.GONE
                            view.empty_data.visibility = View.VISIBLE

                            view.refresh_search_results_menus.isRefreshing = false

                            view.empty_data.empty_image.setImageResource(R.drawable.ic_search)
                            view.empty_data.empty_text.text = "No Menu To Show"
                        }

                    } catch (e: Exception) {

                        menusTimer.cancel()

                        view.shimmer_loader.stopShimmer()
                        view.shimmer_loader.visibility = View.GONE

                        view.search_results_menus.visibility = View.GONE
                        view.empty_data.visibility = View.VISIBLE

                        view.refresh_search_results_menus.isRefreshing = false

                        view.empty_data.empty_image.setImageResource(R.drawable.ic_search)
                        view.empty_data.empty_text.text = "No Menu To Show"
                    }
                } else {

                    menusTimer.cancel()

                    view.shimmer_loader.stopShimmer()
                    view.shimmer_loader.visibility = View.GONE

                    view.search_results_menus.visibility = View.GONE
                    view.empty_data.visibility = View.VISIBLE

                    view.refresh_search_results_menus.isRefreshing = false

                    view.empty_data.empty_image.setImageResource(R.drawable.ic_search)
                    view.empty_data.empty_text.text = "No Menu To Show"
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
        try { menusTimer.cancel() } catch (e: Exception) {}
        Bridge.clear(this)
    }

    override fun onPause() {
        super.onPause()
        try { menusTimer.cancel() } catch (e: Exception) {}
    }

    override fun onDetach() {
        super.onDetach()
        try { menusTimer.cancel() } catch (e: Exception) {}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try { menusTimer.cancel() } catch (e: Exception) {}
    }

    companion object {

        private const val REQUEST_FINE_LOCATION = 2
        private const val TIMER_PERIOD = 6000.toLong()

        fun newInstance(query: String) =
            MenuSearchResults().apply {
                arguments = Bundle().apply {
                    putString("query", query)
                }
            }
    }
}
