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
import kotlinx.android.synthetic.main.fragment_restaurant_drinks.view.*
import kotlinx.android.synthetic.main.include_empty_data.view.*
import okhttp3.*
import java.io.IOException
import java.time.Duration
import java.util.concurrent.TimeUnit


class RestaurantDrinksFragment : Fragment() {

    private lateinit var branch: Branch
    private lateinit var drinks: MutableList<RestaurantMenu>

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
        val view = inflater.inflate(R.layout.fragment_restaurant_drinks, container, false)

        gson = Gson()
        branch = gson.fromJson(arguments?.getString("resto"), Branch::class.java)

        if(savedInstanceState != null){
            drinks = gson.fromJson<MutableList<RestaurantMenu>>(savedInstanceState.getString("resto", "[]"), object : TypeToken<MutableList<RestaurantMenu>>(){}.type)
            drinks.filter { it.type.id == 2 }.sortedByDescending { it.menu.reviews?.average }
            showMenuDrinks(drinks.toMutableList(), view)
        } else {
            drinks = branch.menus.menus!! as MutableList<RestaurantMenu>
            drinks.filter { it.type.id == 2 }.sortedByDescending { it.menu.reviews?.average }
            showMenuDrinks(drinks.toMutableList(), view)
        }

        this.loadRestaurantMenuDrinks(view)

        return view
    }

    @SuppressLint("SetTextI18n")
    private fun showMenuDrinks(_drinks: MutableList<RestaurantMenu>, view: View){
        if(!_drinks.isNullOrEmpty()){
            view.drinks_recycler_view.visibility = View.VISIBLE
            view.progress_loader.visibility = View.GONE
            view.empty_data.visibility = View.GONE
            _drinks.filter { it.type.id == 1 }.sortedByDescending { it.menu.reviews?.average }
            view.drinks_recycler_view.layoutManager = LinearLayoutManager(context)
            view.drinks_recycler_view.adapter = RestaurantMenuAdapter(_drinks.toMutableList(), fragmentManager!!)
        } else {
            view.drinks_recycler_view.visibility = View.GONE
            view.progress_loader.visibility = View.GONE
            view.empty_data.visibility = View.VISIBLE
            view.empty_data.empty_image.setImageResource(R.drawable.ic_glass_gray)
            view.empty_data.empty_text.text = "No Menu Drink To Show"
            TingToast(context!!, "No Menu Drink To Show", TingToastType.DEFAULT).showToast(Toast.LENGTH_LONG)
        }
    }

    @SuppressLint("NewApi", "SetTextI18n", "DefaultLocale")
    private fun loadRestaurantMenuDrinks(view: View){
        val url = "${Routes().HOST_END_POINT}${branch.urls.apiDrinks}"
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(Duration.ofMinutes(5)).build()

        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    view.drinks_recycler_view.visibility = View.GONE
                    view.progress_loader.visibility = View.GONE
                    view.empty_data.visibility = View.VISIBLE
                    view.empty_data.empty_image.setImageResource(R.drawable.ic_glass_gray)
                    view.empty_data.empty_text.text = "No Menu Drink To Show"
                    TingToast(context!!, e.message!!.capitalize(), TingToastType.ERROR).showToast(
                        Toast.LENGTH_LONG)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body()!!.string()
                drinks = gson.fromJson<MutableList<RestaurantMenu>>(dataString, object : TypeToken<MutableList<RestaurantMenu>>(){}.type)
                activity?.runOnUiThread{
                    showMenuDrinks(drinks, view)
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
            RestaurantDrinksFragment().apply {
                arguments = Bundle().apply {
                    putString("resto", resto)
                }
            }
    }
}