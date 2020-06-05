package com.codepipes.ting.fragments.cuisine


import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import com.codepipes.ting.R
import com.codepipes.ting.abstracts.EndlessScrollEventListener
import com.codepipes.ting.adapters.cuisine.CuisineMenusAdapter
import com.codepipes.ting.models.RestaurantCategory
import com.codepipes.ting.models.RestaurantMenu
import com.codepipes.ting.models.User
import com.codepipes.ting.providers.LocalData
import com.codepipes.ting.providers.TingClient
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.UtilsFunctions
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
import kotlin.collections.ArrayList

class CuisineMenusFragment : Fragment() {

    private lateinit var cuisine: RestaurantCategory

    private lateinit var mUtilFunctions: UtilsFunctions
    private lateinit var mLocalData: LocalData

    private lateinit var session: User
    private lateinit var userAuthentication: UserAuthentication

    private lateinit var cuisineMenusTimer: Timer

    private val menus: MutableList<RestaurantMenu> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Bridge.restoreInstanceState(this, savedInstanceState)
        savedInstanceState?.clear()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

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
        val url = "${Routes.cuisineMenus}${cuisine.id}/"

        TingClient.getRequest(url, null, session.token) { _, isSuccess, result ->
            activity?.runOnUiThread {
                if(isSuccess) {
                    try {
                        val menusResult = Gson().fromJson<MutableList<RestaurantMenu>>(result, object : TypeToken<MutableList<RestaurantMenu>>(){}.type)
                        cuisineMenusTimer.cancel()

                        menus.addAll(menusResult)
                        menus.toSet().toMutableList()

                        if (menus.distinctBy { it.id }.isNotEmpty()) {
                            view.shimmer_loader.stopShimmer()
                            view.shimmer_loader.visibility = View.GONE

                            view.cuisine_menus.visibility = View.VISIBLE
                            view.empty_data.visibility = View.GONE

                            view.refresh_cuisine_menus.isRefreshing = false

                            val linearLayoutManager = LinearLayoutManager(context)
                            val cuisinesMenusAdapter = CuisineMenusAdapter(menus.distinctBy { it.id }.toMutableSet())
                            view.cuisine_menus.layoutManager = linearLayoutManager
                            view.cuisine_menus.adapter = cuisinesMenusAdapter

                            ViewCompat.setNestedScrollingEnabled(view.cuisine_menus, false)

                            val endlessScrollEventListener = object: EndlessScrollEventListener(linearLayoutManager) {
                                override fun onLoadMore(pageNum: Int, recyclerView: RecyclerView?) {
                                    val urlPage = "${Routes.cuisineMenus}${cuisine.id}/?page=${pageNum + 1}"
                                    TingClient.getRequest(urlPage, null, session.token) { _, isSuccess, result ->
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

                            view.cuisine_menus.addOnScrollListener(endlessScrollEventListener)

                        } else {
                            view.shimmer_loader.stopShimmer()
                            view.shimmer_loader.visibility = View.GONE

                            view.cuisine_menus.visibility = View.GONE
                            view.empty_data.visibility = View.VISIBLE

                            view.refresh_cuisine_menus.isRefreshing = false

                            view.empty_data.empty_image.setImageResource(R.drawable.ic_restaurants)
                            view.empty_data.empty_text.text = "No Menu To Show"
                        }

                    } catch (e: Exception) {

                        cuisineMenusTimer.cancel()

                        view.shimmer_loader.stopShimmer()
                        view.shimmer_loader.visibility = View.GONE

                        view.cuisine_menus.visibility = View.GONE
                        view.empty_data.visibility = View.VISIBLE

                        view.refresh_cuisine_menus.isRefreshing = false

                        view.empty_data.empty_image.setImageResource(R.drawable.ic_restaurants)
                        view.empty_data.empty_text.text = "No Menu To Show"
                    }
                } else {

                    cuisineMenusTimer.cancel()

                    view.shimmer_loader.stopShimmer()
                    view.shimmer_loader.visibility = View.GONE

                    view.cuisine_menus.visibility = View.GONE
                    view.empty_data.visibility = View.VISIBLE

                    view.refresh_cuisine_menus.isRefreshing = false

                    view.empty_data.empty_image.setImageResource(R.drawable.ic_spoon_gray)
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
        try { cuisineMenusTimer.cancel() } catch (e: Exception) {}
        Bridge.clear(this)
    }

    override fun onPause() {
        super.onPause()
        try { cuisineMenusTimer.cancel() } catch (e: Exception) {}
    }

    override fun onDetach() {
        super.onDetach()
        try { cuisineMenusTimer.cancel() } catch (e: Exception) {}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try { cuisineMenusTimer.cancel() } catch (e: Exception) {}
        Bridge.clear(this)
    }

    companion object {

        private const val TIMER_PERIOD = 6000.toLong()

        fun newInstance(cuisine: String) =
            CuisineMenusFragment().apply {
                arguments = Bundle().apply {
                    putString("cuisine", cuisine)
                }
            }
    }
}
