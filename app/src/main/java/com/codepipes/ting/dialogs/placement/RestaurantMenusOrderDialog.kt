package com.codepipes.ting.dialogs.placement

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codepipes.ting.CurrentRestaurant
import com.codepipes.ting.R
import com.codepipes.ting.adapters.cuisine.CuisineMenusAdapter
import com.codepipes.ting.adapters.placement.RestaurantMenusOrderAdapter
import com.codepipes.ting.interfaces.RestaurantMenusOrderCloseListener
import com.codepipes.ting.models.Branch
import com.codepipes.ting.models.RestaurantMenu
import com.codepipes.ting.models.User
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.utils.Routes
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.fragment_restaurant_menus_order.view.*
import kotlinx.android.synthetic.main.include_empty_data.view.*
import okhttp3.*
import java.io.IOException
import java.lang.Exception
import java.util.concurrent.TimeUnit

class RestaurantMenusOrderDialog : DialogFragment() {

    private lateinit var userAuthentication: UserAuthentication
    private lateinit var session: User

    private lateinit var restaurantMenusOrderCloseListener: RestaurantMenusOrderCloseListener

    override fun getTheme(): Int = R.style.TransparentDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val view = inflater.inflate(R.layout.fragment_restaurant_menus_order, null, false)

        userAuthentication = UserAuthentication(context!!)
        session = userAuthentication.get()!!

        val menuType = arguments!!.getInt(CurrentRestaurant.MENU_TYPE_KEY)
        val branchId = arguments!!.getInt(CurrentRestaurant.RESTO_BRANCH_KEY)

        view.restaurant_menus_type.text = when (menuType) { 1 -> { "Foods" } 2 -> { "Drinks" } 3 -> { "Dishes" } else -> { "Foods" } }
        view.close_restaurant_menus.setOnClickListener {
            dialog.dismiss()
            restaurantMenusOrderCloseListener.onClose()
        }

        loadMenus(view, menuType, branchId)

        return view
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun loadMenus(view: View, type: Int, branch: Int) {
        val url = Routes().restaurantMenusOrders

        class MenuOrdersInterceptor : Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                val httpUrl = chain.request().url().newBuilder()
                    .addQueryParameter("branch", branch.toString())
                    .addQueryParameter("type", type.toString())
                    .build()
                val request = chain.request().newBuilder()
                    .header("Authorization", session.token!!)
                    .url(httpUrl)
                    .build()
                return chain.proceed(request)
            }
        }

        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60 * 5, TimeUnit.SECONDS)
            .addInterceptor(MenuOrdersInterceptor())
            .build()

        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    view.shimmer_loader.stopShimmer()
                    view.shimmer_loader.visibility = View.GONE

                    view.restaurant_menus.visibility = View.GONE
                    view.empty_data.visibility = View.VISIBLE

                    when (type) {
                        1 -> {
                            view.empty_data.empty_image.setImageResource(R.drawable.ic_spoon_gray)
                            view.empty_data.empty_text.text = "No Food To Show"
                        }
                        2 -> {
                            view.empty_data.empty_image.setImageResource(R.drawable.ic_glass_gray)
                            view.empty_data.empty_text.text = "No Drink To Show"
                        }
                        3 -> {
                            view.empty_data.empty_image.setImageResource(R.drawable.ic_restaurants)
                            view.empty_data.empty_text.text = "No Dish To Show"
                        }
                        else -> {
                            view.empty_data.empty_image.setImageResource(R.drawable.ic_spoon_gray)
                            view.empty_data.empty_text.text = "No Food To Show"
                        }
                    }
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body()!!.string()
                try {
                    val menus = Gson().fromJson<MutableList<RestaurantMenu>>(dataString, object : TypeToken<MutableList<RestaurantMenu>>(){}.type)
                    activity?.runOnUiThread {
                        if (menus.size > 0) {

                            view.shimmer_loader.stopShimmer()
                            view.shimmer_loader.visibility = View.GONE

                            view.restaurant_menus.visibility = View.VISIBLE
                            view.empty_data.visibility = View.GONE

                            view.restaurant_menus.layoutManager = LinearLayoutManager(activity)
                            view.restaurant_menus.adapter = RestaurantMenusOrderAdapter(menus, fragmentManager!!, context!!)
                        } else {
                            view.shimmer_loader.stopShimmer()
                            view.shimmer_loader.visibility = View.GONE

                            view.restaurant_menus.visibility = View.GONE
                            view.empty_data.visibility = View.VISIBLE

                            when (type) {
                                1 -> {
                                    view.empty_data.empty_image.setImageResource(R.drawable.ic_spoon_gray)
                                    view.empty_data.empty_text.text = "No Food To Show"
                                }
                                2 -> {
                                    view.empty_data.empty_image.setImageResource(R.drawable.ic_glass_gray)
                                    view.empty_data.empty_text.text = "No Drink To Show"
                                }
                                3 -> {
                                    view.empty_data.empty_image.setImageResource(R.drawable.ic_restaurants)
                                    view.empty_data.empty_text.text = "No Dish To Show"
                                }
                                else -> {
                                    view.empty_data.empty_image.setImageResource(R.drawable.ic_spoon_gray)
                                    view.empty_data.empty_text.text = "No Food To Show"
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    activity?.runOnUiThread {

                        view.shimmer_loader.stopShimmer()
                        view.shimmer_loader.visibility = View.GONE

                        view.restaurant_menus.visibility = View.GONE
                        view.empty_data.visibility = View.VISIBLE

                        when (type) {
                            1 -> {
                                view.empty_data.empty_image.setImageResource(R.drawable.ic_spoon_gray)
                                view.empty_data.empty_text.text = "No Food To Show"
                            }
                            2 -> {
                                view.empty_data.empty_image.setImageResource(R.drawable.ic_glass_gray)
                                view.empty_data.empty_text.text = "No Drink To Show"
                            }
                            3 -> {
                                view.empty_data.empty_image.setImageResource(R.drawable.ic_restaurants)
                                view.empty_data.empty_text.text = "No Dish To Show"
                            }
                            else -> {
                                view.empty_data.empty_image.setImageResource(R.drawable.ic_spoon_gray)
                                view.empty_data.empty_text.text = "No Food To Show"
                            }
                        }
                    }
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.WRAP_CONTENT
            dialog.window!!.setLayout(width, height)
        }
    }

    fun onDialogClose(listener: RestaurantMenusOrderCloseListener) {
        this.restaurantMenusOrderCloseListener = listener
    }
}