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
import kotlinx.android.synthetic.main.fragment_restaurant_foods.view.*
import kotlinx.android.synthetic.main.include_empty_data.view.*
import okhttp3.*
import java.io.IOException
import java.time.Duration
import java.util.concurrent.TimeUnit

class RestaurantFoodsFragment : Fragment() {

    private lateinit var branch: Branch
    private lateinit var foods: MutableList<RestaurantMenu>

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
        val view = inflater.inflate(R.layout.fragment_restaurant_foods, container, false)

        gson = Gson()
        branch = gson.fromJson(arguments?.getString("resto"), Branch::class.java)

        if(savedInstanceState != null){
            foods = gson.fromJson<MutableList<RestaurantMenu>>(savedInstanceState.getString("resto", "[]"), object : TypeToken<MutableList<RestaurantMenu>>(){}.type).filter { it.type.id == 1 }.toMutableList()
            foods.filter { it.type.id == 1 }.sortedByDescending { it.menu.reviews?.average }
            showMenuFoods(foods.toMutableList(), view)
        } else {
            foods = branch.menus.menus!!.filter { it.type.id == 1 } as MutableList<RestaurantMenu>
            foods.filter { it.type.id == 1 }.sortedByDescending { it.menu.reviews?.average }
            showMenuFoods(foods.toMutableList(), view)
        }

        this.loadRestaurantMenuFoods(view)

        return view
    }

    @SuppressLint("SetTextI18n")
    private fun showMenuFoods(_foods: MutableList<RestaurantMenu>, view: View){
        if(!_foods.isNullOrEmpty()){
            view.foods_recycler_view.visibility = View.VISIBLE
            view.progress_loader.visibility = View.GONE
            view.empty_data.visibility = View.GONE
            _foods.filter { it.type.id == 1 }.sortedByDescending { it.menu.reviews?.average }
            view.foods_recycler_view.layoutManager = LinearLayoutManager(context)
            view.foods_recycler_view.adapter = RestaurantMenuAdapter(_foods.toMutableList(), fragmentManager!!)
        } else {
            view.foods_recycler_view.visibility = View.GONE
            view.progress_loader.visibility = View.GONE
            view.empty_data.visibility = View.VISIBLE
            view.empty_data.empty_image.setImageResource(R.drawable.ic_spoon_gray)
            view.empty_data.empty_text.text = "No Menu Food To Show"
            TingToast(context!!, "No Menu Food To Show", TingToastType.DEFAULT).showToast(Toast.LENGTH_LONG)
        }
    }

    @SuppressLint("NewApi", "SetTextI18n", "DefaultLocale")
    private fun loadRestaurantMenuFoods(view: View){
        val url = "${Routes().HOST_END_POINT}${branch.urls.apiFoods}"
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(Duration.ofMinutes(5)).build()

        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    view.foods_recycler_view.visibility = View.GONE
                    view.progress_loader.visibility = View.GONE
                    view.empty_data.visibility = View.VISIBLE
                    view.empty_data.empty_image.setImageResource(R.drawable.ic_spoon_gray)
                    view.empty_data.empty_text.text = "No Menu Food To Show"
                    TingToast(context!!, e.message!!.capitalize(), TingToastType.ERROR).showToast(
                        Toast.LENGTH_LONG)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body()!!.string()
                foods = gson.fromJson<MutableList<RestaurantMenu>>(dataString, object : TypeToken<MutableList<RestaurantMenu>>(){}.type).filter { it.type.id == 1 }.toMutableList()
                activity?.runOnUiThread{
                    showMenuFoods(foods.filter { it.type.id == 1 }.toMutableList(), view)
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
            RestaurantFoodsFragment().apply {
                arguments = Bundle().apply {
                    putString("resto", resto)
                }
            }
    }
}