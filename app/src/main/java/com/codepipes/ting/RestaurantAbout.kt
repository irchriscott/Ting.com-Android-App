package com.codepipes.ting

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v4.content.ContextCompat
import android.widget.Toast
import com.codepipes.ting.dialogs.TingToast
import com.codepipes.ting.dialogs.TingToastType
import com.codepipes.ting.models.Branch
import com.codepipes.ting.models.User
import com.codepipes.ting.providers.LocalData
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.UtilsFunctions
import com.google.gson.Gson
import com.livefront.bridge.Bridge
import okhttp3.*
import java.io.IOException
import java.time.Duration
import java.util.concurrent.TimeUnit

class RestaurantAbout : AppCompatActivity() {

    lateinit var localData: LocalData
    lateinit var userAuthentication: UserAuthentication
    lateinit var utilsFunctions: UtilsFunctions

    lateinit var session: User
    var branchId: Int = 0

    lateinit var branch: Branch

    @SuppressLint("DefaultLocale", "PrivateResource")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurant_about)

        Bridge.restoreInstanceState(this, savedInstanceState)
        savedInstanceState?.clear()

        localData = LocalData(this@RestaurantAbout)
        userAuthentication = UserAuthentication(this@RestaurantAbout)
        utilsFunctions = UtilsFunctions(this@RestaurantAbout)

        session = userAuthentication.get()!!

        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.elevation = 0F
        supportActionBar!!.title = "Restaurant About".toUpperCase()

        val upArrow = ContextCompat.getDrawable(this@RestaurantAbout, R.drawable.abc_ic_ab_back_material)
        upArrow!!.setColorFilter(ContextCompat.getColor(this@RestaurantAbout, R.color.colorPrimary), PorterDuff.Mode.SRC_ATOP)
        supportActionBar!!.setHomeAsUpIndicator(upArrow)

        branchId = intent.getIntExtra("resto", 0)
        val apiGet = intent.getStringExtra("apiGet")
        this.loadRestaurant(branchId)
    }

    @SuppressLint("NewApi", "DefaultLocale")
    private fun loadRestaurant(id: Int){
        val url = "${Routes().restaurantGet}$id/"
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(Duration.ofMinutes(5)).build()

        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    TingToast(this@RestaurantAbout, e.message!!.capitalize(), TingToastType.ERROR).showToast(
                        Toast.LENGTH_LONG)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body()!!.string()
                branch = Gson().fromJson(dataString, Branch::class.java)
                runOnUiThread {
                    localData.updateRestaurant(branch)
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
