package com.codepipes.ting.activities.restaurant

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.View
import androidx.core.widget.NestedScrollView
import com.codepipes.ting.R
import com.codepipes.ting.adapters.restaurant.RestaurantLikesAdapter
import com.codepipes.ting.models.Branch
import com.codepipes.ting.models.User
import com.codepipes.ting.models.UserRestaurant
import com.codepipes.ting.providers.LocalData
import com.codepipes.ting.providers.TingClient
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.UtilsFunctions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.livefront.bridge.Bridge
import kotlinx.android.synthetic.main.activity_restaurant_likes.*
import kotlinx.android.synthetic.main.include_empty_data.view.*
import okhttp3.*
import java.io.IOException
import java.lang.Exception
import java.text.NumberFormat
import java.util.*
import java.util.concurrent.TimeUnit

class RestaurantLikes : AppCompatActivity() {

    private lateinit var localData: LocalData
    private lateinit var userAuthentication: UserAuthentication
    private lateinit var utilsFunctions: UtilsFunctions

    private lateinit var session: User
    private var branchId: Int = 0

    private lateinit var likesTimer: Timer
    private val TIMER_PERIOD = 6000.toLong()

    @SuppressLint("DefaultLocale", "PrivateResource", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurant_likes)

        Bridge.restoreInstanceState(this, savedInstanceState)
        savedInstanceState?.clear()

        localData = LocalData(this@RestaurantLikes)
        userAuthentication = UserAuthentication(this@RestaurantLikes)
        utilsFunctions = UtilsFunctions(this@RestaurantLikes)

        session = userAuthentication.get()!!
        likesTimer = Timer()

        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.elevation = 0F
        supportActionBar!!.title = "Restaurant Likes".toUpperCase()

        try {
            val upArrow = ContextCompat.getDrawable(this@RestaurantLikes,
                R.drawable.abc_ic_ab_back_material
            )
            upArrow!!.setColorFilter(ContextCompat.getColor(this@RestaurantLikes,
                R.color.colorPrimary
            ), PorterDuff.Mode.SRC_ATOP)
            supportActionBar!!.setHomeAsUpIndicator(upArrow)
        } catch (e: Exception) {}

        branchId = intent.getIntExtra("resto", 0)
        val apiGet = intent.getStringExtra("apiGet")
        val url = intent.getStringExtra("url")

        val branch = localData.getRestaurant(branchId)
        if(branch != null) { restaurant_likes_title.text = "${branch.likes?.count} Likes".toUpperCase() }
        else {
            TingClient.getRequest("${Routes.HOST_END_POINT}$apiGet", null, session.token) {_, isSuccess, result ->
                if(isSuccess){
                    runOnUiThread {
                        try {
                            val branchElse = Gson().fromJson(result, Branch::class.java)
                            restaurant_likes_title.text = "${branchElse.likes?.count} Likes".toUpperCase()
                        } catch (e: Exception) {}
                    }
                }
            }
        }

        likesTimer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() { loadRestaurantLikes("${Routes.HOST_END_POINT}$url") }
        }, TIMER_PERIOD, TIMER_PERIOD)
        this.loadRestaurantLikes("${Routes.HOST_END_POINT}$url")

        refresh_likes.setColorSchemeColors(resources.getColor(R.color.colorPrimary), resources.getColor(
            R.color.colorAccentMain
        ), resources.getColor(R.color.colorPrimaryDark), resources.getColor(R.color.colorAccentMain))
        refresh_likes.setOnRefreshListener {
            refresh_likes.isRefreshing = true
            this.loadRestaurantLikes(url)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showLikes(_likes: MutableList<UserRestaurant>, url: String){

        shimmerLoader.stopShimmer()
        shimmerLoader.visibility = View.GONE

        if(_likes.isNotEmpty()){

            val linearLayoutManager = LinearLayoutManager(this@RestaurantLikes)
            val restaurantLikesAdapter = RestaurantLikesAdapter(_likes)

            restaurant_likes_recycler_view.visibility = View.VISIBLE
            empty_data.visibility = View.GONE
            restaurant_likes_recycler_view.layoutManager = linearLayoutManager
            restaurant_likes_recycler_view.adapter = restaurantLikesAdapter
            ViewCompat.setNestedScrollingEnabled(restaurant_likes_recycler_view, false)

            var pageNum = 1

            scroll_view.setOnScrollChangeListener { view: NestedScrollView?, _: Int, scrollY: Int, _: Int, oldScrollY: Int ->

                if (view?.getChildAt(view.childCount - 1) != null) {
                    if ((scrollY >= (view.getChildAt(view.childCount - 1)!!.measuredHeight - view.measuredHeight)) && scrollY > oldScrollY) {

                        val visibleItemCount = linearLayoutManager.childCount
                        val totalItemCount = linearLayoutManager.itemCount
                        val pastVisibleItems = linearLayoutManager.findLastVisibleItemPosition()

                        if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {

                            pageNum++

                            TingClient.getRequest("$url?page=$pageNum", null, session.token) { _, isSuccess, result ->
                                if(isSuccess) {
                                    runOnUiThread {
                                        try {
                                            val likes = Gson().fromJson<MutableList<UserRestaurant>>(result, object : TypeToken<MutableList<UserRestaurant>>(){}.type)
                                            restaurantLikesAdapter.addItems(likes)
                                        } catch (e: Exception){}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            restaurant_likes_recycler_view.visibility = View.GONE
            empty_data.visibility = View.VISIBLE
            empty_data.empty_text.text = "No Like For This Restaurant"
            empty_data.empty_image.setImageDrawable(resources.getDrawable(R.drawable.ic_heart_filled_gray))
        }
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun loadRestaurantLikes(url: String){
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60 * 5, TimeUnit.SECONDS).build()

        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {}
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body!!.string()
                try {
                    val likes = Gson().fromJson<MutableList<UserRestaurant>>(dataString, object : TypeToken<MutableList<UserRestaurant>>(){}.type)
                    runOnUiThread { showLikes(likes, url) }
                } catch (e: Exception) {}
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Bridge.saveInstanceState(this, outState)
        outState.clear()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)
        Bridge.saveInstanceState(this, outState)
        outState.clear()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { outPersistentState?.clear() }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { likesTimer.cancel() } catch (e: Exception) {}
        Bridge.clear(this)
    }

    override fun onPause() {
        super.onPause()
        try { likesTimer.cancel() } catch (e: Exception) {}
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        try { likesTimer.cancel() } catch (e: Exception) {}
    }

    override fun onStop() {
        super.onStop()
        try { likesTimer.cancel() } catch (e: Exception) {}
    }
}
