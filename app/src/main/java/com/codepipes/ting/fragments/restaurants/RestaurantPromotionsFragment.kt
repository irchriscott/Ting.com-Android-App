package com.codepipes.ting.fragments.restaurants


import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import com.codepipes.ting.R
import com.codepipes.ting.adapters.promotion.RestaurantPromotionAdapter
import com.codepipes.ting.dialogs.TingToast
import com.codepipes.ting.dialogs.TingToastType
import com.codepipes.ting.models.Branch
import com.codepipes.ting.models.MenuPromotion
import com.codepipes.ting.utils.Routes
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.livefront.bridge.Bridge
import kotlinx.android.synthetic.main.fragment_restaurant_promotions.*
import kotlinx.android.synthetic.main.fragment_restaurant_promotions.view.*
import kotlinx.android.synthetic.main.include_empty_data.view.*
import okhttp3.*
import java.io.IOException
import java.lang.Exception
import java.time.Duration
import java.util.concurrent.TimeUnit


class RestaurantPromotionsFragment : Fragment() {

    private lateinit var branch: Branch
    private lateinit var promotions: MutableList<MenuPromotion>

    private lateinit var gson: Gson

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

        if(savedInstanceState != null){
            promotions = gson.fromJson<MutableList<MenuPromotion>>(savedInstanceState.getString("resto", "[]"), object : TypeToken<MutableList<MenuPromotion>>(){}.type)
            promotions.sortBy { it.isOnToday && it.isOn }
            showPromotions(promotions.reversed().toMutableList(), view)
        } else {
            promotions = branch.promotions?.promotions!! as MutableList<MenuPromotion>
            promotions.sortBy { it.isOnToday && it.isOn }
            showPromotions(promotions.reversed().toMutableList(), view)
        }

        this.loadRestaurantPromotions(view)

        return view
    }

    @SuppressLint("SetTextI18n")
    private fun showPromotions(promotions: MutableList<MenuPromotion>, view: View){
        if(!promotions.isNullOrEmpty()){
            view.promotions_recycler_view.visibility = View.VISIBLE
            view.progress_loader.visibility = View.GONE
            view.empty_data.visibility = View.GONE
            promotions.sortBy { it.isOnToday && it.isOn }
            view.promotions_recycler_view.layoutManager = LinearLayoutManager(context)
            view.promotions_recycler_view.adapter = RestaurantPromotionAdapter(promotions.reversed().toMutableList(), fragmentManager!!)
        } else {
            view.promotions_recycler_view.visibility = View.GONE
            view.progress_loader.visibility = View.GONE
            view.empty_data.visibility = View.VISIBLE
            view.empty_data.empty_image.setImageResource(R.drawable.ic_star_filled_gray)
            view.empty_data.empty_text.text = "No Promotion To Show"
            TingToast(context!!, "No Promotion To Show", TingToastType.DEFAULT).showToast(Toast.LENGTH_LONG)
        }
    }

    @SuppressLint("NewApi", "SetTextI18n", "DefaultLocale")
    private fun loadRestaurantPromotions(view: View){
        val url = "${Routes().HOST_END_POINT}${branch.urls.apiPromotions}"
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(Duration.ofMinutes(5)).build()

        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    view.promotions_recycler_view.visibility = View.GONE
                    view.progress_loader.visibility = View.GONE
                    view.empty_data.visibility = View.VISIBLE
                    view.empty_data.empty_image.setImageResource(R.drawable.ic_star_filled_gray)
                    view.empty_data.empty_text.text = "No Promotion To Show"
                    TingToast(context!!, e.message!!.capitalize(), TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body()!!.string()
                promotions = gson.fromJson<MutableList<MenuPromotion>>(dataString, object : TypeToken<MutableList<MenuPromotion>>(){}.type)

                activity?.runOnUiThread{
                    showPromotions(promotions, view)
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
        Bridge.clear(this)
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