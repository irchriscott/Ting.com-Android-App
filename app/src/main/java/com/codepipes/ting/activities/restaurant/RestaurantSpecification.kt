package com.codepipes.ting.activities.restaurant

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.codepipes.ting.R
import com.codepipes.ting.adapters.restaurant.specifications.RestaurantCuisinesAdapter
import com.codepipes.ting.adapters.restaurant.specifications.RestaurantFoodCategoryAdapter
import com.codepipes.ting.adapters.restaurant.specifications.RestaurantSpecialsAdapter
import com.codepipes.ting.models.Branch
import com.codepipes.ting.models.User
import com.codepipes.ting.providers.LocalData
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.UtilsFunctions
import com.google.gson.Gson
import com.livefront.bridge.Bridge
import kotlinx.android.synthetic.main.activity_restaurant_specification.*
import okhttp3.*
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class RestaurantSpecification : AppCompatActivity() {

    private lateinit var localData: LocalData
    private lateinit var userAuthentication: UserAuthentication
    private lateinit var utilsFunctions: UtilsFunctions

    private lateinit var session: User
    private var branchId: Int = 0

    private lateinit var branch: Branch
    private lateinit var branchTimer: Timer
    private val TIMER_PERIOD = 6000.toLong()

    @SuppressLint("PrivateResource", "DefaultLocale")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurant_specification)

        localData = LocalData(this@RestaurantSpecification)
        userAuthentication = UserAuthentication(this@RestaurantSpecification)
        utilsFunctions = UtilsFunctions(this@RestaurantSpecification)

        session = userAuthentication.get()!!
        branchTimer = Timer()

        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.elevation = 0F
        supportActionBar!!.title = "Specifications".toUpperCase()

        try {
            val upArrow = ContextCompat.getDrawable(this@RestaurantSpecification,
                R.drawable.abc_ic_ab_back_material
            )
            upArrow!!.setColorFilter(ContextCompat.getColor(this@RestaurantSpecification,
                R.color.colorPrimary
            ), PorterDuff.Mode.SRC_ATOP)
            supportActionBar!!.setHomeAsUpIndicator(upArrow)
        } catch (e: java.lang.Exception) {}

        branchId = intent.getIntExtra("resto", 0)
        val apiGet = intent.getStringExtra("apiGet")
        branchTimer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() { loadRestaurant(branchId) }
        }, TIMER_PERIOD, TIMER_PERIOD)
        this.loadRestaurant(branchId)

        if(localData.getRestaurant(branchId) != null){
            branch = localData.getRestaurant(branchId)!!
            this.showRestaurantSpecifications(branch)
        }
    }

    private fun showRestaurantSpecifications(branch: Branch) {
        restaurant_specials.layoutManager = LinearLayoutManager(this@RestaurantSpecification)
        restaurant_specials.adapter = RestaurantSpecialsAdapter(branch.specials)

        restaurant_services.layoutManager = LinearLayoutManager(this@RestaurantSpecification)
        restaurant_services.adapter = RestaurantSpecialsAdapter(branch.services)

        restaurant_categories.layoutManager = LinearLayoutManager(this@RestaurantSpecification)
        restaurant_categories.adapter = if (!branch.restaurant?.foodCategories?.categories.isNullOrEmpty()) {
            RestaurantFoodCategoryAdapter(branch.restaurant?.foodCategories!!.categories)
        } else { RestaurantFoodCategoryAdapter(ArrayList()) }

        restaurant_cuisines.layoutManager = LinearLayoutManager(this@RestaurantSpecification)
        restaurant_cuisines.adapter = RestaurantCuisinesAdapter(branch.categories.categories)
    }

    private fun loadRestaurant(id: Int){
        val url = "${Routes.restaurantGet}$id/"
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
                branch = Gson().fromJson(dataString, Branch::class.java)
                runOnUiThread {
                    branchTimer.cancel()
                    localData.updateRestaurant(branch)
                    showRestaurantSpecifications(branch)
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
