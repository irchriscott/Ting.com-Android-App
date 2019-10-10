package com.codepipes.ting

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.Toast
import com.codepipes.ting.adapters.restaurant.RestaurantLikesAdapter
import com.codepipes.ting.dialogs.TingToast
import com.codepipes.ting.dialogs.TingToastType
import com.codepipes.ting.models.Branch
import com.codepipes.ting.models.User
import com.codepipes.ting.models.UserRestaurant
import com.codepipes.ting.providers.LocalData
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
import java.text.NumberFormat
import java.time.Duration
import java.util.ArrayList
import java.util.concurrent.TimeUnit

class RestaurantLikes : AppCompatActivity() {

    lateinit var localData: LocalData
    lateinit var userAuthentication: UserAuthentication
    lateinit var utilsFunctions: UtilsFunctions

    lateinit var session: User
    var branchId: Int = 0

    lateinit var branch: Branch
    lateinit var likes: MutableList<UserRestaurant>

    @SuppressLint("DefaultLocale", "PrivateResource")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurant_likes)

        Bridge.restoreInstanceState(this, savedInstanceState)
        savedInstanceState?.clear()

        localData = LocalData(this@RestaurantLikes)
        userAuthentication = UserAuthentication(this@RestaurantLikes)
        utilsFunctions = UtilsFunctions(this@RestaurantLikes)

        session = userAuthentication.get()!!

        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.elevation = 0F
        supportActionBar!!.title = "Restaurant Likes".toUpperCase()

        val upArrow = ContextCompat.getDrawable(this@RestaurantLikes, R.drawable.abc_ic_ab_back_material)
        upArrow!!.setColorFilter(ContextCompat.getColor(this@RestaurantLikes, R.color.colorPrimary), PorterDuff.Mode.SRC_ATOP)
        supportActionBar!!.setHomeAsUpIndicator(upArrow)

        branchId = intent.getIntExtra("resto", 0)
        val apiGet = intent.getStringExtra("apiGet")
        val url = intent.getStringExtra("url")

        if(localData.getRestaurant(branchId) != null){
            branch = localData.getRestaurant(branchId)!!
            likes = branch.likes?.likes!!.toMutableList()
            this.showLikes(likes, branch)
            this.loadRestaurant(branch.id, false)
            this.loadRestaurantLikes("${Routes().HOST_END_POINT}${branch.urls.apiReviews}")
        } else {
            likes = mutableListOf()
            this.loadRestaurant(branchId, true)
            this.loadRestaurantLikes("${Routes().HOST_END_POINT}$url")
        }

        refresh_likes.setColorSchemeColors(resources.getColor(R.color.colorPrimary), resources.getColor(R.color.colorAccentMain), resources.getColor(R.color.colorPrimaryDark), resources.getColor(R.color.colorAccentMain))
        refresh_likes.setOnRefreshListener {
            refresh_likes.isRefreshing = true
            this.loadRestaurant(branchId, true)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showLikes(_likes: MutableList<UserRestaurant>, _branch: Branch){

        shimmerLoader.stopShimmer()
        shimmerLoader.visibility = View.GONE

        restaurant_likes_title.text = "${NumberFormat.getNumberInstance().format(_branch.likes?.count)} LIKES"

        if(_likes.isNotEmpty()){
            restaurant_likes_recycler_view.visibility = View.VISIBLE
            empty_data.visibility = View.GONE
            restaurant_likes_recycler_view.layoutManager = LinearLayoutManager(this@RestaurantLikes)
            restaurant_likes_recycler_view.adapter = RestaurantLikesAdapter(_likes)
        } else {
            restaurant_likes_recycler_view.visibility = View.GONE
            empty_data.visibility = View.VISIBLE
            empty_data.empty_text.text = "No Like For This Restaurant"
            empty_data.empty_image.setImageDrawable(resources.getDrawable(R.drawable.ic_heart_filled_gray))
        }
    }

    @SuppressLint("NewApi", "DefaultLocale")
    private fun loadRestaurant(id: Int, load: Boolean){
        val url = "${Routes().restaurantGet}$id/"
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(Duration.ofMinutes(5)).build()

        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    TingToast(this@RestaurantLikes, e.message!!.capitalize(), TingToastType.ERROR).showToast(
                        Toast.LENGTH_LONG)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body()!!.string()
                branch = Gson().fromJson(dataString, Branch::class.java)
                runOnUiThread {
                    localData.updateRestaurant(branch)
                    if(load){ showLikes(branch.likes?.likes!!.toMutableList(), branch) }
                }
            }
        })
    }

    @SuppressLint("NewApi", "DefaultLocale", "SetTextI18n")
    private fun loadRestaurantLikes(url: String){
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(Duration.ofMinutes(5)).build()

        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    TingToast(this@RestaurantLikes, e.message!!.capitalize(), TingToastType.ERROR).showToast(
                        Toast.LENGTH_LONG)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body()!!.string()
                likes = Gson().fromJson<MutableList<UserRestaurant>>(dataString, object : TypeToken<MutableList<UserRestaurant>>(){}.type)
                runOnUiThread {
                    shimmerLoader.stopShimmer()
                    shimmerLoader.visibility = View.GONE
                    if(likes.isNotEmpty()){
                        restaurant_likes_recycler_view.visibility = View.VISIBLE
                        empty_data.visibility = View.GONE
                        restaurant_likes_recycler_view.layoutManager = LinearLayoutManager(this@RestaurantLikes)
                        restaurant_likes_recycler_view.adapter = RestaurantLikesAdapter(likes)
                    } else {
                        restaurant_likes_recycler_view.visibility = View.GONE
                        empty_data.visibility = View.VISIBLE
                        empty_data.empty_text.text = "No Like For This Restaurant"
                        empty_data.empty_image.setImageDrawable(resources.getDrawable(R.drawable.ic_heart_filled_gray))
                    }
                }
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
        Bridge.clear(this)
    }
}
