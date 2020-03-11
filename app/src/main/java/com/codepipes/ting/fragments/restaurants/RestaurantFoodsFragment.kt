package com.codepipes.ting.fragments.restaurants


import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.ViewCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.codepipes.ting.R
import com.codepipes.ting.abstracts.EndlessScrollEventListener
import com.codepipes.ting.adapters.menu.RestaurantMenuAdapter
import com.codepipes.ting.models.Branch
import com.codepipes.ting.models.RestaurantMenu
import com.codepipes.ting.providers.TingClient
import com.codepipes.ting.utils.Routes
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.livefront.bridge.Bridge
import kotlinx.android.synthetic.main.fragment_restaurant_foods.view.*
import kotlinx.android.synthetic.main.include_empty_data.view.*
import okhttp3.*
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class RestaurantFoodsFragment : Fragment() {

    private lateinit var branch: Branch
    private lateinit var foods: MutableList<RestaurantMenu>

    private lateinit var gson: Gson

    private lateinit var foodsTimer: Timer
    private val TIMER_PERIOD = 10000.toLong()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Bridge.restoreInstanceState(this, savedInstanceState)
        savedInstanceState?.clear()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_restaurant_foods, container, false)

        gson = Gson()
        branch = gson.fromJson(arguments?.getString("resto"), Branch::class.java)

        foodsTimer = Timer()

        if(savedInstanceState != null){
            foods = gson.fromJson<MutableList<RestaurantMenu>>(savedInstanceState.getString("resto", "[]"), object : TypeToken<MutableList<RestaurantMenu>>(){}.type).filter { it.type.id == 1 }.toMutableList()
            foods.filter { it.type.id == 1 }.sortedByDescending { it.menu.reviews?.average }
            showMenuFoods(foods.toMutableList(), view)
        } else {
            if(!branch.menus.menus.isNullOrEmpty()) {
                foods = branch.menus.menus!!.filter { it.type.id == 1 } as MutableList<RestaurantMenu>
                foods.filter { it.type.id == 1 }.sortedByDescending { it.menu.reviews?.average }
                showMenuFoods(foods.toMutableList(), view)
            }
        }

        foodsTimer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() { loadRestaurantMenuFoods(view) }
        }, TIMER_PERIOD, TIMER_PERIOD)
        this.loadRestaurantMenuFoods(view)

        return view
    }

    @SuppressLint("SetTextI18n")
    private fun showMenuFoods(_foods: MutableList<RestaurantMenu>, view: View){
        if(!_foods.isNullOrEmpty()){

            val linearLayoutManager = LinearLayoutManager(context)

            view.foods_recycler_view.visibility = View.VISIBLE
            view.progress_loader.visibility = View.GONE
            view.empty_data.visibility = View.GONE
            view.shimmer_loader.visibility = View.GONE
            _foods.filter { it.type.id == 1 }.sortedByDescending { it.menu.reviews?.average }

            val restaurantMenuAdapter = RestaurantMenuAdapter(_foods.toMutableList(), fragmentManager!!)
            view.foods_recycler_view.layoutManager = linearLayoutManager
            view.foods_recycler_view.adapter = restaurantMenuAdapter

            val endlessScrollEventListener = object : EndlessScrollEventListener(linearLayoutManager) {
                override fun onLoadMore(pageNum: Int, recyclerView: RecyclerView?) {
                    val url = "${Routes.HOST_END_POINT}${branch.urls.apiFoods}?page=${pageNum + 1}"
                    TingClient.getRequest(url, null, null) { _, isSuccess, result ->
                        if(isSuccess) {
                            try {
                                val menusResultPage =
                                    Gson().fromJson<MutableList<RestaurantMenu>>(
                                        result,
                                        object :
                                            TypeToken<MutableList<RestaurantMenu>>() {}.type
                                    )
                                restaurantMenuAdapter.addItems(menusResultPage)
                            } catch (e: java.lang.Exception) {}
                        }
                    }
                }
            }
            //view.foods_recycler_view.addOnScrollListener(endlessScrollEventListener)
        } else {
            view.foods_recycler_view.visibility = View.GONE
            view.progress_loader.visibility = View.GONE
            view.shimmer_loader.visibility = View.GONE
            view.empty_data.visibility = View.VISIBLE
            view.empty_data.empty_image.setImageResource(R.drawable.ic_spoon_gray)
            view.empty_data.empty_text.text = "No Menu Food To Show"
        }
    }

    @SuppressLint("NewApi", "SetTextI18n", "DefaultLocale")
    private fun loadRestaurantMenuFoods(view: View){
        val url = "${Routes.HOST_END_POINT}${branch.urls.apiFoods}"
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60 * 5, TimeUnit.SECONDS).build()

        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    if(view.foods_recycler_view.visibility != View.VISIBLE) {
                        view.foods_recycler_view.visibility = View.GONE
                        view.progress_loader.visibility = View.GONE
                        view.empty_data.visibility = View.VISIBLE

                        view.empty_data.empty_image.setImageResource(R.drawable.ic_spoon_gray)
                        view.empty_data.empty_text.text = "No Menu Food To Show"
                    }
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body()!!.string()
                try {
                    foods = gson.fromJson<MutableList<RestaurantMenu>>(dataString, object : TypeToken<MutableList<RestaurantMenu>>(){}.type).filter { it.type.id == 1 }.toMutableList()
                    activity?.runOnUiThread {
                        foodsTimer.cancel()
                        showMenuFoods(foods.filter { it.type.id == 1 }.toMutableList(), view)
                    }
                } catch (e: java.lang.Exception){}
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
        try { foodsTimer.cancel() } catch (e: Exception) {}
        Bridge.clear(this)
    }

    override fun onPause() {
        super.onPause()
        try { foodsTimer.cancel() } catch (e: Exception) {}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try { foodsTimer.cancel() } catch (e: Exception) {}
    }

    override fun onDetach() {
        super.onDetach()
        try { foodsTimer.cancel() } catch (e: Exception) {}
    }

    companion object {

        fun newInstance(resto: String) =
            RestaurantFoodsFragment().apply {
                arguments = Bundle().apply {
                    putString("resto", resto)
                }
            }
    }
}
