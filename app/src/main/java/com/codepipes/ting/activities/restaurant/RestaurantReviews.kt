package com.codepipes.ting.activities.restaurant

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.widget.NestedScrollView
import com.codepipes.ting.R
import com.codepipes.ting.adapters.restaurant.RestaurantReviewsAdapter
import com.codepipes.ting.customclasses.XAxisValueFormatter
import com.codepipes.ting.dialogs.messages.TingToast
import com.codepipes.ting.dialogs.messages.TingToastType
import com.codepipes.ting.models.Branch
import com.codepipes.ting.models.RestaurantReview
import com.codepipes.ting.models.ServerResponse
import com.codepipes.ting.models.User
import com.codepipes.ting.providers.LocalData
import com.codepipes.ting.providers.TingClient
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.UtilsFunctions
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.livefront.bridge.Bridge
import com.stepstone.apprating.AppRatingDialog
import com.stepstone.apprating.listener.RatingDialogListener
import kotlinx.android.synthetic.main.activity_restaurant_reviews.*
import kotlinx.android.synthetic.main.include_empty_data.view.*
import okhttp3.*
import java.io.IOException
import java.text.NumberFormat
import java.util.*
import java.util.concurrent.TimeUnit

class RestaurantReviews : AppCompatActivity(), RatingDialogListener {

    private lateinit var localData: LocalData
    private lateinit var userAuthentication: UserAuthentication
    private lateinit var utilsFunctions: UtilsFunctions

    private lateinit var session: User
    private var branchId: Int = 0

    private lateinit var branch: Branch
    private lateinit var reviews: MutableList<RestaurantReview>

    private lateinit var reviewsTimer: Timer
    private val TIMER_PERIOD = 6000.toLong()

    @SuppressLint("DefaultLocale", "PrivateResource")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurant_reviews)

        Bridge.restoreInstanceState(this, savedInstanceState)
        savedInstanceState?.clear()

        localData = LocalData(this@RestaurantReviews)
        userAuthentication = UserAuthentication(this@RestaurantReviews)
        utilsFunctions = UtilsFunctions(this@RestaurantReviews)

        session = userAuthentication.get()!!
        reviewsTimer = Timer()

        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.elevation = 0F
        supportActionBar!!.title = "Restaurant Reviews".toUpperCase()

        try {
            val upArrow = ContextCompat.getDrawable(this@RestaurantReviews,
                R.drawable.abc_ic_ab_back_material
            )
            upArrow!!.setColorFilter(ContextCompat.getColor(this@RestaurantReviews,
                R.color.colorPrimary
            ), PorterDuff.Mode.SRC_ATOP)
            supportActionBar!!.setHomeAsUpIndicator(upArrow)
        } catch (e: java.lang.Exception) {}

        branchId = intent.getIntExtra("resto", 0)
        val apiGet = intent.getStringExtra("apiGet")
        val url = intent.getStringExtra("url")

        if(localData.getRestaurant(branchId) != null){
            branch = localData.getRestaurant(branchId)!!
            reviews = mutableListOf()
            this.showReviews(branch)
            this.loadRestaurant(branch.id, false)
            reviewsTimer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() { loadRestaurantReviews("${Routes.HOST_END_POINT}${branch.urls.apiReviews}") }
            }, TIMER_PERIOD, TIMER_PERIOD)
            this.loadRestaurantReviews("${Routes.HOST_END_POINT}${branch.urls.apiReviews}")
        } else {
            reviews = mutableListOf()
            this.loadRestaurant(branchId, true)
            reviewsTimer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() { loadRestaurantReviews("${Routes.HOST_END_POINT}$url") }
            }, TIMER_PERIOD, TIMER_PERIOD)
            this.loadRestaurantReviews("${Routes.HOST_END_POINT}$url")
        }

        refresh_reviews.setColorSchemeColors(resources.getColor(R.color.colorPrimary), resources.getColor(
            R.color.colorAccentMain
        ), resources.getColor(R.color.colorPrimaryDark), resources.getColor(R.color.colorAccentMain))
        refresh_reviews.setOnRefreshListener {
            refresh_reviews.isRefreshing = true
            this.loadRestaurant(branchId, true)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showReviews(_branch: Branch){

        shimmerLoader.stopShimmer()
        shimmerLoader.visibility = View.GONE

        restaurant_reviews_average.text = _branch.reviews?.average.toString()
        restaurant_reviews_title.text = "${NumberFormat.getNumberInstance().format(_branch.reviews?.count)} REVIEWS"

        val ratingChart = findViewById<HorizontalBarChart>(R.id.restaurant_rating_percents)
        ratingChart.setDrawBarShadow(false)
        val description = Description()
        description.text = ""
        ratingChart.description = description
        ratingChart.legend.isEnabled = false
        ratingChart.setPinchZoom(false)
        ratingChart.setDrawValueAboveBar(false)
        ratingChart.axisLeft.textColor = resources.getColor(R.color.colorGray)
        ratingChart.xAxis.textColor = resources.getColor(R.color.colorGray)

        val xAxis = ratingChart.xAxis
        xAxis.setDrawGridLines(false)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.isEnabled = true
        xAxis.setDrawAxisLine(false)


        val yLeft = ratingChart.axisLeft

        yLeft.axisMaximum = 100f
        yLeft.axisMinimum = 0f
        yLeft.isEnabled = false

        xAxis.labelCount = _branch.reviews?.percents!!.size

        val values = arrayOf("1 ★", "2 ★", "3 ★", "4 ★", "5 ★")
        xAxis.valueFormatter = XAxisValueFormatter(values)

        val yRight = ratingChart.axisRight
        yRight.setDrawAxisLine(true)
        yRight.setDrawGridLines(false)
        yRight.isEnabled = false

        val entries = ArrayList<BarEntry>()
        entries.add(BarEntry(0f, _branch.reviews.percents[0].toFloat()))
        entries.add(BarEntry(1f, _branch.reviews.percents[1].toFloat()))
        entries.add(BarEntry(2f, _branch.reviews.percents[2].toFloat()))
        entries.add(BarEntry(3f, _branch.reviews.percents[3].toFloat()))
        entries.add(BarEntry(4f, _branch.reviews.percents[4].toFloat()))

        val barDataSet = BarDataSet(entries, "Bar Data Set")
        barDataSet.setColors(
            ContextCompat.getColor(ratingChart.context, R.color.colorGray),
            ContextCompat.getColor(ratingChart.context, R.color.colorGray),
            ContextCompat.getColor(ratingChart.context, R.color.colorGray),
            ContextCompat.getColor(ratingChart.context, R.color.colorGray),
            ContextCompat.getColor(ratingChart.context, R.color.colorGray)
        )

        barDataSet.setDrawValues(false)

        ratingChart.animateY(1000)
        ratingChart.setDrawBarShadow(true)
        barDataSet.barShadowColor = Color.argb(40, 150, 150, 150)

        val data = BarData(barDataSet)
        data.barWidth = 0.9f
        ratingChart.data = data
        ratingChart.invalidate()
    }

    @SuppressLint("NewApi", "DefaultLocale")
    private fun loadRestaurant(id: Int, load: Boolean){
        val url = "${Routes.restaurantGet}$id/"
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60 * 5, TimeUnit.SECONDS).build()

        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    TingToast(
                        this@RestaurantReviews,
                        e.message!!.capitalize(),
                        TingToastType.ERROR
                    ).showToast(
                        Toast.LENGTH_LONG)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body!!.string()
                try {
                    branch = Gson().fromJson(dataString, Branch::class.java)
                    runOnUiThread {
                        localData.updateRestaurant(branch)
                        showReviews(branch)
                    }
                } catch (e: Exception){}
            }
        })
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun loadRestaurantReviews(url: String){
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
                    reviews = Gson().fromJson<MutableList<RestaurantReview>>(dataString, object : TypeToken<MutableList<RestaurantReview>>(){}.type)
                    runOnUiThread {
                        reviewsTimer.cancel()
                        shimmerLoader.stopShimmer()
                        shimmerLoader.visibility = View.GONE
                        if(reviews.isNotEmpty()){
                            restaurant_reviews_recycler_view.visibility = View.VISIBLE
                            empty_data.visibility = View.GONE

                            val linearLayoutManager = LinearLayoutManager(this@RestaurantReviews)
                            val restaurantReviewsAdapter = RestaurantReviewsAdapter(reviews)
                            restaurant_reviews_recycler_view.layoutManager = linearLayoutManager
                            restaurant_reviews_recycler_view.adapter = restaurantReviewsAdapter
                            ViewCompat.setNestedScrollingEnabled(restaurant_reviews_recycler_view, false)

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
                                                            val reviews = Gson().fromJson<MutableList<RestaurantReview>>(result, object : TypeToken<MutableList<RestaurantReview>>(){}.type)
                                                            restaurantReviewsAdapter.addItems(reviews)
                                                        } catch (e: Exception){}
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                        } else {
                            restaurant_reviews_recycler_view.visibility = View.GONE
                            empty_data.visibility = View.VISIBLE
                            empty_data.empty_text.text = "No Review For This Restaurant"
                            empty_data.empty_image.setImageDrawable(resources.getDrawable(R.drawable.ic_comments_gray))
                        }
                    }
                } catch (e: java.lang.Exception){}
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.restaurant_menu_review, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.menu_rate -> {

                val restaurantReview = utilsFunctions.userRestaurantReview(reviews, session)

                val ratingDialog = AppRatingDialog.Builder()
                    .setPositiveButtonText("Submit")
                    .setNegativeButtonText("Cancel")
                    .setNeutralButtonText("Later")
                    .setNoteDescriptions(listOf("Very Bad", "Not good", "Quite Ok", "Very Good", "Excellent !!!"))
                    .setTitle("Rate this Restaurant")
                    .setDescription("Please select a rate and write a review")
                    .setCommentInputEnabled(true)
                    .setStarColor(R.color.colorYellowRate)
                    .setNoteDescriptionTextColor(R.color.colorGray)
                    .setTitleTextColor(R.color.colorGray)
                    .setDescriptionTextColor(R.color.colorLightGray)
                    .setHint("Please write your review here ...")
                    .setHintTextColor(R.color.colorGray)
                    .setCommentTextColor(R.color.colorGray)
                    .setCommentBackgroundColor(R.color.colorPrimaryDarkTransparentDark)
                    .setWindowAnimation(R.style.RateDialogFadeAnimation)
                    .setCancelable(false)
                    .setCanceledOnTouchOutside(false)

                val url = Routes.checkUserRestaurantReview
                val client = OkHttpClient.Builder()
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .callTimeout(60 * 5, TimeUnit.SECONDS).build()

                val form = MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("resto", branchId.toString())
                    .build()

                val request = Request.Builder()
                    .header("Authorization", session.token!!)
                    .url(url)
                    .post(form)
                    .build()

                client.newCall(request).enqueue(object : Callback {

                    override fun onFailure(call: Call, e: IOException) {
                        runOnUiThread {
                            ratingDialog.setDefaultRating(1)
                            ratingDialog.create(this@RestaurantReviews).show()
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val dataString = response.body!!.string()
                        runOnUiThread {
                            try {
                                if(response.code == 200) {
                                    val review = Gson().fromJson<RestaurantReview>(dataString, RestaurantReview::class.java)
                                    ratingDialog.setDefaultRating(review.review)
                                    ratingDialog.setDefaultComment(review.comment)
                                    ratingDialog.create(this@RestaurantReviews).show()
                                } else {
                                    ratingDialog.setDefaultRating(1)
                                    ratingDialog.create(this@RestaurantReviews).show()
                                }
                            } catch (e: java.lang.Exception) {
                                ratingDialog.setDefaultRating(1)
                                ratingDialog.create(this@RestaurantReviews).show()
                            }
                        }
                    }
                })

                return true
            }
        }
        return false
    }

    override fun onPositiveButtonClicked(rate: Int, comment: String) {

        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("review", rate.toString())
            .addFormDataPart("comment", comment)
            .build()

        val request = Request.Builder()
            .header("Authorization", session.token!!)
            .url("${Routes.HOST_END_POINT}${branch.urls.apiAddReview}")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    TingToast(
                        this@RestaurantReviews,
                        e.message!!,
                        TingToastType.ERROR
                    ).showToast(Toast.LENGTH_LONG)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body!!.string()
                try{
                    val serverResponse = Gson().fromJson(responseBody, ServerResponse::class.java)
                    runOnUiThread {
                        loadRestaurant(branchId, true)
                        if (serverResponse.status == 200){
                            TingToast(
                                this@RestaurantReviews,
                                serverResponse.message,
                                TingToastType.SUCCESS
                            ).showToast(Toast.LENGTH_LONG)
                        } else { TingToast(
                            this@RestaurantReviews,
                            serverResponse.message,
                            TingToastType.ERROR
                        ).showToast(Toast.LENGTH_LONG) }
                    }
                } catch (e: Exception){
                    runOnUiThread { TingToast(
                        this@RestaurantReviews,
                        "An Error Has Occurred",
                        TingToastType.ERROR
                    ).showToast(Toast.LENGTH_LONG) }
                }
            }
        })
    }

    override fun onNegativeButtonClicked() {}

    override fun onNeutralButtonClicked() {}

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
        try { reviewsTimer.cancel() } catch (e: java.lang.Exception) {}
        Bridge.clear(this)
    }

    override fun onPause() {
        super.onPause()
        try { reviewsTimer.cancel() } catch (e: java.lang.Exception) {}
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        try { reviewsTimer.cancel() } catch (e: java.lang.Exception) {}
    }

    override fun onStop() {
        super.onStop()
        try { reviewsTimer.cancel() } catch (e: java.lang.Exception) {}
    }
}
