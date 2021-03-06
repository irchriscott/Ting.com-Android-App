package com.codepipes.ting.activities.restaurant

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v4.content.ContextCompat
import com.codepipes.ting.R
import com.codepipes.ting.models.Branch
import com.codepipes.ting.models.User
import com.codepipes.ting.providers.LocalData
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.UtilsFunctions
import com.google.gson.Gson
import com.livefront.bridge.Bridge
import kotlinx.android.synthetic.main.activity_restaurant_about.*
import okhttp3.*
import java.io.IOException
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit

class RestaurantAbout : AppCompatActivity() {

    private lateinit var localData: LocalData
    private lateinit var userAuthentication: UserAuthentication
    private lateinit var utilsFunctions: UtilsFunctions

    private lateinit var session: User
    private var branchId: Int = 0

    private lateinit var branch: Branch
    private lateinit var branchTimer: Timer
    private val TIMER_PERIOD = 6000.toLong()

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
        branchTimer = Timer()

        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.elevation = 0F
        supportActionBar!!.title = "Restaurant About".toUpperCase()

        try {
            val upArrow = ContextCompat.getDrawable(this@RestaurantAbout,
                R.drawable.abc_ic_ab_back_material
            )
            upArrow!!.setColorFilter(ContextCompat.getColor(this@RestaurantAbout,
                R.color.colorPrimary
            ), PorterDuff.Mode.SRC_ATOP)
            supportActionBar!!.setHomeAsUpIndicator(upArrow)
        } catch (e: Exception) {}

        branchId = intent.getIntExtra("resto", 0)
        val apiGet = intent.getStringExtra("apiGet")
        branchTimer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() { loadRestaurant(branchId) }
        }, TIMER_PERIOD, TIMER_PERIOD)
        this.loadRestaurant(branchId)

        if(localData.getRestaurant(branchId) != null){
            branch = localData.getRestaurant(branchId)!!
            this.showRestaurantProfile(branch)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showRestaurantProfile(_branch: Branch){
        restaurant_name.text = _branch.restaurant?.name
        restaurant_branch.text = _branch.name
        restaurant_address.text = "${_branch.town}, ${_branch.country}"
        restaurant_open_close.text = "${_branch.restaurant?.opening} - ${_branch.restaurant?.closing}"
        restaurant_motto.text = _branch.restaurant?.motto

        restaurant_email.text = _branch.email
        restaurant_phone.text = _branch.phone
        restaurant_full_address.text = _branch.address

        restaurant_foods.text = _branch.menus.type.foods.count.toString()
        restaurant_drinks.text = _branch.menus.type.drinks.toString()
        restaurant_dishes.text = _branch.menus.type.dishes.toString()
        restaurant_likes.text = _branch.likes?.count.toString()
        restaurant_reviews.text = _branch.reviews?.count.toString()
        review_rating.rating = _branch.reviews?.average!!

        restaurant_currency.text = _branch.restaurant?.config?.currency
        restaurant_vat.text = "${_branch.restaurant?.config?.tax} %"
        restaurant_late_reservation.text = "${_branch.restaurant?.config?.cancelLateBooking} minutes"
        restaurant_reservation_with_advance.text = if(_branch.restaurant?.config?.bookWithAdvance!!){ "YES"} else { "NO" }
        restaurant_reservation_advance.text = if(_branch.restaurant.config.bookWithAdvance){ "${_branch.restaurant.config.currency} ${_branch.restaurant.config.bookingAdvance}"} else { "-" }
        restaurant_refund_after_cancelation.text = if(_branch.restaurant.config.bookingCancelationRefund) { "YES" } else { "NO" }
        restaurant_booking_payment_mode.text = _branch.restaurant.config.bookingPaymentMode
        restaurant_days_before_booking.text = "${_branch.restaurant.config.daysBeforeReservation} Days"
        restaurant_can_take_away.text = if(_branch.restaurant.config.canTakeAway) { "YES" } else { "NO" }
        restaurant_pay_before.text = if(_branch.restaurant.config.userShouldPayBefore) { "YES" } else { "NO" }
    }

    @SuppressLint("DefaultLocale")
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
                val dataString = response.body()!!.string()
                branch = Gson().fromJson(dataString, Branch::class.java)
                runOnUiThread {
                    branchTimer.cancel()
                    localData.updateRestaurant(branch)
                    showRestaurantProfile(branch)
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
        try { branchTimer.cancel() } catch (e: Exception) {}
        Bridge.clear(this)
    }

    override fun onPause() {
        super.onPause()
        try { branchTimer.cancel() } catch (e: Exception) {}
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        try { branchTimer.cancel() } catch (e: Exception) {}
    }

    override fun onStop() {
        super.onStop()
        try { branchTimer.cancel() } catch (e: Exception) {}
    }
}
