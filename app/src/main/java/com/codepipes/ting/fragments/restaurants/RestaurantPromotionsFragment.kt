package com.codepipes.ting.fragments.restaurants


import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.codepipes.ting.R
import com.codepipes.ting.abstracts.EndlessScrollEventListener
import com.codepipes.ting.adapters.promotion.RestaurantPromotionAdapter
import com.codepipes.ting.models.Branch
import com.codepipes.ting.models.MenuPromotion
import com.codepipes.ting.models.RestaurantMenu
import com.codepipes.ting.providers.TingClient
import com.codepipes.ting.utils.Routes
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.livefront.bridge.Bridge
import kotlinx.android.synthetic.main.fragment_restaurant_promotions.view.*
import kotlinx.android.synthetic.main.include_empty_data.view.*
import okhttp3.*
import java.io.IOException
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit


class RestaurantPromotionsFragment : Fragment() {

    private lateinit var branch: Branch
    private lateinit var promotions: MutableList<MenuPromotion>

    private lateinit var gson: Gson

    private lateinit var promotionsTimer: Timer
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
        val view = inflater.inflate(R.layout.fragment_restaurant_promotions, container, false)

        gson = Gson()
        branch = gson.fromJson(arguments?.getString("resto"), Branch::class.java)

        promotionsTimer = Timer()

        if(savedInstanceState != null){
            promotions = gson.fromJson<MutableList<MenuPromotion>>(savedInstanceState.getString("resto", "[]"), object : TypeToken<MutableList<MenuPromotion>>(){}.type)
            promotions.sortBy { it.isOnToday && it.isOn }
            showPromotions(promotions.reversed().toMutableList(), view)
        } else {
            if(!branch.promotions?.promotions.isNullOrEmpty()) {
                promotions = branch.promotions?.promotions!! as MutableList<MenuPromotion>
                promotions.sortBy { it.isOnToday && it.isOn }
                showPromotions(promotions.reversed().toMutableList(), view)
            }
        }

        promotionsTimer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() { loadRestaurantPromotions(view) }
        }, TIMER_PERIOD, TIMER_PERIOD)
        this.loadRestaurantPromotions(view)

        return view
    }

    @SuppressLint("SetTextI18n")
    private fun showPromotions(promotions: MutableList<MenuPromotion>, view: View){
        if(!promotions.isNullOrEmpty()){

            val linearLayoutManager = LinearLayoutManager(context)

            view.promotions_recycler_view.visibility = View.VISIBLE
            view.progress_loader.visibility = View.GONE
            view.empty_data.visibility = View.GONE
            view.shimmer_loader.visibility = View.GONE
            promotions.sortBy { it.isOnToday && it.isOn }

            val restaurantPromotionAdapter = RestaurantPromotionAdapter(promotions.reversed().toMutableList(), fragmentManager!!)
            view.promotions_recycler_view.layoutManager = linearLayoutManager
            view.promotions_recycler_view.adapter = restaurantPromotionAdapter

            val endlessScrollEventListener = object : EndlessScrollEventListener(linearLayoutManager) {
                override fun onLoadMore(pageNum: Int, recyclerView: RecyclerView?) {
                    val url = "${Routes.HOST_END_POINT}${branch.urls.apiPromotions}?page=${pageNum + 1}"
                    TingClient.getRequest(url, null, null) { _, isSuccess, result ->
                        if(isSuccess) {
                            try {
                                val promotionsResultPage =
                                    Gson().fromJson<MutableList<MenuPromotion>>(
                                        result,
                                        object :
                                            TypeToken<MutableList<MenuPromotion>>() {}.type
                                    )
                                restaurantPromotionAdapter.addItems(promotionsResultPage)
                            } catch (e: Exception) {}
                        }
                    }
                }
            }

        } else {
            view.promotions_recycler_view.visibility = View.GONE
            view.progress_loader.visibility = View.GONE
            view.shimmer_loader.visibility = View.GONE
            view.empty_data.visibility = View.VISIBLE
            view.empty_data.empty_image.setImageResource(R.drawable.ic_star_filled_gray)
            view.empty_data.empty_text.text = "No Promotion To Show"
        }
    }

    @SuppressLint("NewApi", "SetTextI18n", "DefaultLocale")
    private fun loadRestaurantPromotions(view: View){
        val url = "${Routes.HOST_END_POINT}${branch.urls.apiPromotions}"
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60 * 5, TimeUnit.SECONDS).build()

        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    if (view.promotions_recycler_view.visibility != View.VISIBLE) {
                        view.promotions_recycler_view.visibility = View.GONE
                        view.progress_loader.visibility = View.GONE
                        view.empty_data.visibility = View.VISIBLE

                        view.empty_data.empty_image.setImageResource(R.drawable.ic_star_filled_gray)
                        view.empty_data.empty_text.text = "No Promotion To Show"
                    }
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body()!!.string()
                try {
                    promotions = gson.fromJson<MutableList<MenuPromotion>>(dataString, object : TypeToken<MutableList<MenuPromotion>>(){}.type)
                    activity?.runOnUiThread {
                        promotionsTimer.cancel()
                        showPromotions(promotions, view)
                    }
                } catch (e: Exception) {}
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
        try { promotionsTimer.cancel() } catch (e: Exception) {}
        Bridge.clear(this)
    }

    override fun onPause() {
        super.onPause()
        try { promotionsTimer.cancel() } catch (e: Exception) {}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try { promotionsTimer.cancel() } catch (e: Exception) {}
    }

    override fun onDetach() {
        super.onDetach()
        try { promotionsTimer.cancel() } catch (e: Exception) {}
    }

    companion object {

        fun newInstance(resto: String) =
            RestaurantPromotionsFragment().apply {
                arguments = Bundle().apply {
                    putString("resto", resto)
                }
            }
    }
}
