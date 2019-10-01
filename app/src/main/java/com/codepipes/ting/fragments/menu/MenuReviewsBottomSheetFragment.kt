package com.codepipes.ting.fragments.menu

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.codepipes.ting.R
import com.codepipes.ting.adapters.menu.MenuReviewsAdapter
import com.codepipes.ting.dialogs.TingToast
import com.codepipes.ting.dialogs.TingToastType
import com.codepipes.ting.models.MenuReview
import com.codepipes.ting.models.RestaurantMenu
import com.codepipes.ting.utils.Routes
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.fragment_menu_reviews_bottom_sheet.view.*
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit

class MenuReviewsBottomSheetFragment : BottomSheetDialogFragment(){

    lateinit var gson: Gson

    override fun getTheme(): Int = R.style.BaseBottomSheetDialogElse

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        gson = Gson()

        val view = inflater.inflate(R.layout.fragment_menu_reviews_bottom_sheet, container, false)
        val myArgs = this.arguments

        val menu = gson.fromJson(myArgs?.getString("menu")!!, RestaurantMenu::class.java)

        val url = "${Routes().HOST_END_POINT}${menu.urls.apiReviews}"

        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity!!.runOnUiThread {
                    TingToast(activity!!, e.message!!, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body()!!.string()
                val gson = Gson()
                try{
                    val reviews = gson.fromJson<MutableList<MenuReview>>(responseBody, object : TypeToken<MutableList<MenuReview>>(){}.type)
                    activity!!.runOnUiThread {
                        view.progress_loader.visibility = View.GONE
                        view.menu_reviews_recycler_view.visibility = View.VISIBLE
                        view.menu_reviews_recycler_view.layoutManager = LinearLayoutManager(activity)
                        view.menu_reviews_recycler_view.adapter = MenuReviewsAdapter(reviews)
                    }
                } catch (e: Exception){
                    activity!!.runOnUiThread {
                        TingToast(activity!!, "An Error Has Occurred", TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                    }
                }
            }
        })

        return view
    }
}