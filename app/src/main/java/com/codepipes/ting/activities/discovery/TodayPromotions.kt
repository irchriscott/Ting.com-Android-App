package com.codepipes.ting.activities.discovery

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.View
import androidx.core.content.ContextCompat
import com.codepipes.ting.R
import com.codepipes.ting.adapters.promotion.TodayPromotionAdapter
import com.codepipes.ting.models.MenuPromotion
import com.codepipes.ting.models.User
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.utils.Routes
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.livefront.bridge.Bridge
import kotlinx.android.synthetic.main.activity_today_promotions.*
import kotlinx.android.synthetic.main.include_empty_data.view.*
import okhttp3.*
import java.io.IOException
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit

class TodayPromotions : AppCompatActivity() {

    private lateinit var session: User
    private lateinit var userAuthentication: UserAuthentication

    private lateinit var gson: Gson

    private lateinit var promotionsTimer: Timer
    private val TIMER_PERIOD = 6000.toLong()

    @SuppressLint("DefaultLocale", "PrivateResource")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_today_promotions)

        Bridge.restoreInstanceState(this, savedInstanceState)
        savedInstanceState?.clear()

        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.elevation = 0F
        supportActionBar!!.title = "Today's Promotions".toUpperCase()

        try {
            val upArrow = ContextCompat.getDrawable(this@TodayPromotions,
                R.drawable.abc_ic_ab_back_material
            )
            upArrow!!.setColorFilter(ContextCompat.getColor(this@TodayPromotions,
                R.color.colorPrimary
            ), PorterDuff.Mode.SRC_ATOP)
            supportActionBar!!.setHomeAsUpIndicator(upArrow)
        } catch (e: java.lang.Exception) {}

        userAuthentication = UserAuthentication(this@TodayPromotions)
        session = userAuthentication.get()!!

        gson = Gson()

        shimmer_loader.startShimmer()

        promotionsTimer = Timer()
        promotionsTimer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() { loadTodayPromotions() }
        }, TIMER_PERIOD, TIMER_PERIOD)
        this.loadTodayPromotions()

        refresh_promotions.setColorSchemeColors(resources.getColor(R.color.colorPrimary), resources.getColor(
            R.color.colorAccentMain
        ), resources.getColor(R.color.colorPrimaryDark), resources.getColor(R.color.colorAccentMain))
        refresh_promotions.setOnRefreshListener {
            refresh_promotions.isRefreshing = true
            this.loadTodayPromotions()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadTodayPromotions() {
        val url = Routes.discoverTodayPromosAll
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60 * 5, TimeUnit.SECONDS).build()

        val request = Request.Builder().header("Authorization", session.token!!).url(url).get().build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { refresh_promotions.isRefreshing = false }
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body!!.string()
                val promotions = gson.fromJson<MutableList<MenuPromotion>>(dataString, object : TypeToken<MutableList<MenuPromotion>>(){}.type)
                runOnUiThread{
                    try {
                        promotionsTimer.cancel()
                        refresh_promotions.isRefreshing = false
                        if(!promotions.isNullOrEmpty()){
                            promotions_recycler_view.visibility = View.VISIBLE
                            empty_data.visibility = View.GONE
                            if(shimmer_loader != null) { shimmer_loader.visibility = View.GONE }

                            promotions.sortBy { it.isOnToday && it.isOn }
                            promotions_recycler_view.layoutManager = LinearLayoutManager(this@TodayPromotions)
                            promotions_recycler_view.adapter = TodayPromotionAdapter(promotions.reversed().toMutableList())
                        } else {
                            promotions_recycler_view.visibility = View.GONE
                            empty_data.visibility = View.VISIBLE
                            if(shimmer_loader != null) { shimmer_loader.visibility = View.GONE }

                            empty_data.empty_image.setImageResource(R.drawable.ic_star_filled_gray)
                            empty_data.empty_text.text = "No Promotion To Show"
                        }
                    } catch (e: Exception) { refresh_promotions.isRefreshing = false }
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
        try { promotionsTimer.cancel() } catch (e: Exception) {}
        Bridge.clear(this)
    }

    override fun onPause() {
        super.onPause()
        try { promotionsTimer.cancel() } catch (e: Exception) {}
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        try { promotionsTimer.cancel() } catch (e: Exception) {}
    }

    override fun onStop() {
        super.onStop()
        try { promotionsTimer.cancel() } catch (e: Exception) {}
    }
}
