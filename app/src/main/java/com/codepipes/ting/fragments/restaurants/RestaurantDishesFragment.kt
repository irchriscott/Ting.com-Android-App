package com.codepipes.ting.fragments.restaurants


import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import com.codepipes.ting.R
import com.codepipes.ting.adapters.menu.RestaurantMenuAdapter
import com.codepipes.ting.dialogs.TingToast
import com.codepipes.ting.dialogs.TingToastType
import com.codepipes.ting.models.Branch
import com.codepipes.ting.models.RestaurantMenu
import com.codepipes.ting.utils.Routes
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.livefront.bridge.Bridge
import kotlinx.android.synthetic.main.fragment_restaurant_dishes.view.*
import kotlinx.android.synthetic.main.include_empty_data.view.*
import okhttp3.*
import java.io.IOException
import java.time.Duration
import java.util.concurrent.TimeUnit

class RestaurantDishesFragment : Fragment() {

    private lateinit var branch: Branch
    private lateinit var dishes: MutableList<RestaurantMenu>

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
        val view = inflater.inflate(R.layout.fragment_restaurant_dishes, container, false)
        gson = Gson()
        branch = gson.fromJson(arguments?.getString("resto"), Branch::class.java)

        if(savedInstanceState != null){
            dishes = gson.fromJson<MutableList<RestaurantMenu>>(savedInstanceState.getString("resto", "[]"), object : TypeToken<MutableList<RestaurantMenu>>(){}.type)
            dishes.filter { it.type.id == 3 }.sortedByDescending { it.menu.reviews?.average }
            showMenuDishes(dishes.toMutableList(), view)
        } else {
            dishes = branch.menus.menus!! as MutableList<RestaurantMenu>
            dishes.filter { it.type.id == 3 }.sortedByDescending { it.menu.reviews?.average }
            showMenuDishes(dishes.toMutableList(), view)
        }

        this.loadRestaurantMenuDishes(view)

        return view
    }

    @SuppressLint("SetTextI18n")
    private fun showMenuDishes(_dishes: MutableList<RestaurantMenu>, view: View){
        if(!_dishes.isNullOrEmpty()){
            view.dishes_recycler_view.visibility = View.VISIBLE
            view.progress_loader.visibility = View.GONE
            view.empty_data.visibility = View.GONE
            _dishes.filter { it.type.id == 1 }.sortedByDescending { it.menu.reviews?.average }
            view.dishes_recycler_view.layoutManager = LinearLayoutManager(context)
            view.dishes_recycler_view.adapter = RestaurantMenuAdapter(_dishes.toMutableList(), fragmentManager!!)
        } else {
            view.dishes_recycler_view.visibility = View.GONE
            view.progress_loader.visibility = View.GONE
            view.empty_data.visibility = View.VISIBLE
            view.empty_data.empty_image.setImageResource(R.drawable.ic_restaurants)
            view.empty_data.empty_text.text = "No Menu Dish To Show"
            TingToast(context!!, "No Menu Dish To Show", TingToastType.DEFAULT).showToast(Toast.LENGTH_LONG)
        }
    }

    @SuppressLint("NewApi", "SetTextI18n", "DefaultLocale")
    private fun loadRestaurantMenuDishes(view: View){
        val url = "${Routes().HOST_END_POINT}${branch.urls.apiDishes}"
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(Duration.ofMinutes(5)).build()

        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    view.dishes_recycler_view.visibility = View.GONE
                    view.progress_loader.visibility = View.GONE
                    view.empty_data.visibility = View.VISIBLE
                    view.empty_data.empty_image.setImageResource(R.drawable.ic_restaurants)
                    view.empty_data.empty_text.text = "No Menu Dish To Show"
                    TingToast(context!!, e.message!!.capitalize(), TingToastType.ERROR).showToast(
                        Toast.LENGTH_LONG)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body()!!.string()
                dishes = gson.fromJson<MutableList<RestaurantMenu>>(dataString, object : TypeToken<MutableList<RestaurantMenu>>(){}.type)
                activity?.runOnUiThread{
                    showMenuDishes(dishes, view)
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
            RestaurantDishesFragment().apply {
                arguments = Bundle().apply {
                    putString("resto", resto)
                }
            }
    }
}