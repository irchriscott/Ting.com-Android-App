package com.codepipes.ting

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import com.codepipes.ting.fragments.user.UserAbout
import com.codepipes.ting.fragments.user.UserMoments
import com.codepipes.ting.fragments.user.UserRestaurants
import com.codepipes.ting.models.Branch
import com.codepipes.ting.models.User
import com.codepipes.ting.providers.UserAuthentication
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import android.R.menu
import android.content.Intent
import android.support.v4.app.SupportActivity
import android.support.v4.app.SupportActivity.ExtraData
import android.support.v4.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.os.Build
import android.os.PersistableBundle
import android.support.v7.view.menu.MenuBuilder
import android.view.View
import android.widget.Toast
import com.codepipes.ting.customclasses.ActionSheet
import com.codepipes.ting.dialogs.TingToast
import com.codepipes.ting.dialogs.TingToastType
import com.codepipes.ting.fragments.restaurants.*
import com.codepipes.ting.interfaces.ActionSheetCallBack
import com.codepipes.ting.providers.LocalData
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.UtilsFunctions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.livefront.bridge.Bridge
import kotlinx.android.synthetic.main.activity_restaurant_profile.*
import okhttp3.*
import java.io.IOException
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


class RestaurantProfile : AppCompatActivity() {

    private lateinit var mUserProfileName: TextView
    private lateinit var mUserProfileAddress: TextView
    private lateinit var mUserProfileImage: CircleImageView

    private lateinit var userAuthentication: UserAuthentication

    private lateinit var mUserTabLayout: TabLayout
    private lateinit var mUserViewPager: ViewPager
    private lateinit var mUserToolbar: Toolbar

    private lateinit var session: User
    private lateinit var branch: Branch

    private lateinit var utilsFunctions: UtilsFunctions
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var localData: LocalData

    private var selectedLatitude: Double = 0.0
    private var selectedLongitude: Double = 0.0

    private lateinit var restaurantTimer: Timer
    private val TIMER_PERIOD = 6000.toLong()

    @SuppressLint("SetTextI18n", "PrivateResource", "MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurant_profile)

        Bridge.restoreInstanceState(this, savedInstanceState)
        savedInstanceState?.clear()

        userAuthentication = UserAuthentication(this@RestaurantProfile)
        session = userAuthentication.get()!!
        restaurantTimer = Timer()

        utilsFunctions = UtilsFunctions(this@RestaurantProfile)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this@RestaurantProfile)
        localData = LocalData(this@RestaurantProfile)

        mUserToolbar = findViewById<Toolbar>(R.id.userToolbar) as Toolbar
        setSupportActionBar(mUserToolbar)

        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.elevation = 0F
        supportActionBar!!.title = ""
        supportActionBar!!.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.colorPrimaryDark)))

        try {
            val upArrow = ContextCompat.getDrawable(this@RestaurantProfile, R.drawable.abc_ic_ab_back_material)
            upArrow!!.setColorFilter(ContextCompat.getColor(this@RestaurantProfile, R.color.colorWhite), PorterDuff.Mode.SRC_ATOP)
            supportActionBar!!.setHomeAsUpIndicator(upArrow)
        } catch (e: java.lang.Exception) {}

        val restoId = intent.getIntExtra("resto", 0)
        val localBranch  = localData.getRestaurant(restoId)

        if(localBranch != null) {
            branch = localBranch
            this.showBranch()
            restaurantTimer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() { loadRestaurant(restoId, false) }
            }, TIMER_PERIOD, TIMER_PERIOD)
            this.loadRestaurant(restoId, false)
        } else {
            shimmerLoader.startShimmer()
            userProfileData.visibility = View.GONE
            shimmerLoader.visibility = View.VISIBLE
            restaurantTimer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() { loadRestaurant(restoId, false) }
            }, TIMER_PERIOD, TIMER_PERIOD)
            this.loadRestaurant(restoId, true)
        }
    }

    @SuppressLint("DefaultLocale")
    private fun loadRestaurant(id: Int, load: Boolean){
        val url = "${Routes().restaurantGet}$id/"
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
                try{
                    branch = Gson().fromJson(dataString, Branch::class.java)
                    runOnUiThread {
                        restaurantTimer.cancel()
                        localData.updateRestaurant(branch)
                        if(load){ showBranch() }
                    }
                } catch(e: Exception){}
            }
        })
    }

    @SuppressLint("SetTextI18n", "MissingPermission", "DefaultLocale")
    private fun showBranch(){

        userProfileData.visibility = View.VISIBLE
        shimmerLoader.stopShimmer()
        shimmerLoader.visibility = View.GONE

        mUserProfileName = findViewById<TextView>(R.id.userProfileName) as TextView
        mUserProfileAddress = findViewById<TextView>(R.id.userProfileAddress) as TextView
        mUserProfileImage = findViewById<CircleImageView>(R.id.userProfileImage) as CircleImageView

        mUserProfileName.text = "${branch.restaurant?.name}, ${branch.name}"
        mUserProfileAddress.text = branch.address
        Picasso.get().load(branch.restaurant?.logoURL()).into(mUserProfileImage)
        restaurant_rating.rating = branch.reviews?.average!!


        mUserTabLayout = findViewById<TabLayout>(R.id.userTabLayout) as TabLayout
        mUserViewPager = findViewById<ViewPager>(R.id.userViewPager) as ViewPager

        val adapter = RestaurantProfileViewPagerAdapter(supportFragmentManager)
        adapter.addFragment(RestaurantPromotionsFragment.newInstance(Gson().toJson(branch)), "PROMOS")
        adapter.addFragment(RestaurantFoodsFragment.newInstance(Gson().toJson(branch)), "FOODS")
        adapter.addFragment(RestaurantDrinksFragment.newInstance(Gson().toJson(branch)), "DRINKS")
        adapter.addFragment(RestaurantDishesFragment.newInstance(Gson().toJson(branch)), "DISHES")

        mUserViewPager.adapter = adapter
        mUserTabLayout.setupWithViewPager(mUserViewPager)

        mUserViewPager.currentItem = intent.getIntExtra("tab", 0)

        val menuList = mutableListOf<String>()
        menuList.add("Current Location")
        session.addresses?.addresses!!.forEach { menuList.add("${it.type} - ${it.address}") }

        restaurant_distance_view.setOnClickListener {
            val cx = (it.x + it.width / 2).toInt()
            val cy = (it.y + it.height).toInt()

            val mapFragment =  RestaurantsMapFragment()
            val args: Bundle = Bundle()

            args.putInt("cx", cx)
            args.putInt("cy", cy)
            args.putDouble("lat", selectedLatitude)
            args.putDouble("lng", selectedLongitude)
            args.putString("resto", Gson().toJson(branch))

            mapFragment.arguments = args
            mapFragment.show(supportFragmentManager, mapFragment.tag)
        }

        restaurant_distance_view.setOnLongClickListener {
            val actionSheet = ActionSheet(this@RestaurantProfile, menuList)
                .setTitle("Restaurant Near Location")
                .setColorData(resources.getColor(R.color.colorGray))
                .setColorTitleCancel(resources.getColor(R.color.colorGoogleRedTwo))
                .setColorSelected(resources.getColor(R.color.colorPrimary))
                .setCancelTitle("Cancel")

            actionSheet.create(object : ActionSheetCallBack {

                override fun data(data: String, position: Int) {
                    if (position == 0) {
                        if (utilsFunctions.checkLocationPermissions()) {
                            try {
                                fusedLocationClient.lastLocation.addOnSuccessListener {
                                    if (it != null) {
                                        val from = LatLng(it.latitude, it.longitude)
                                        selectedLatitude = it.latitude
                                        selectedLongitude = it.longitude
                                        val to = LatLng(branch.latitude, branch.longitude)
                                        val dist = utilsFunctions.calculateDistance(from, to)
                                        branch.dist = dist
                                        branch.fromLocation = from
                                        runOnUiThread { restaurant_distance.text = "${dist.toString()} Km" }
                                    } else {
                                        val from = LatLng(
                                            session.addresses!!.addresses[0].latitude,
                                            session.addresses!!.addresses[0].longitude
                                        )
                                        selectedLatitude = session.addresses!!.addresses[0].latitude
                                        selectedLongitude = session.addresses!!.addresses[0].longitude
                                        val to = LatLng(branch.latitude, branch.longitude)
                                        val dist = utilsFunctions.calculateDistance(from, to)
                                        branch.dist = dist
                                        branch.fromLocation = from
                                        runOnUiThread { restaurant_distance.text = "${dist.toString()} Km" }
                                    }
                                }.addOnFailureListener {
                                    val from = LatLng(
                                        session.addresses!!.addresses[0].latitude,
                                        session.addresses!!.addresses[0].longitude
                                    )
                                    selectedLatitude = session.addresses!!.addresses[0].latitude
                                    selectedLongitude = session.addresses!!.addresses[0].longitude
                                    val to = LatLng(branch.latitude, branch.longitude)
                                    val dist = utilsFunctions.calculateDistance(from, to)
                                    branch.dist = dist
                                    branch.fromLocation = from
                                    runOnUiThread { restaurant_distance.text = "${dist.toString()} Km" }
                                    TingToast(this@RestaurantProfile, it.message!!, TingToastType.ERROR).showToast(
                                        Toast.LENGTH_LONG
                                    )
                                }
                            } catch (e: java.lang.Exception) {
                                TingToast(
                                    this@RestaurantProfile,
                                    e.message!!.capitalize(),
                                    TingToastType.ERROR
                                ).showToast(Toast.LENGTH_LONG)
                            }
                        }
                    } else {
                        val address = session.addresses?.addresses!![position - 1]
                        val from = LatLng(address.latitude, address.longitude)
                        selectedLatitude = address.latitude
                        selectedLongitude = address.longitude
                        val to = LatLng(branch.latitude, branch.longitude)
                        val dist = utilsFunctions.calculateDistance(from, to)
                        branch.dist = dist
                        branch.fromLocation = from
                        runOnUiThread { restaurant_distance.text = "${dist.toString()} Km" }
                    }
                }
            })

            return@setOnLongClickListener true
        }

        if (branch.isAvailable) {

            val status =
                utilsFunctions.statusWorkTime(branch.restaurant?.opening!!, branch.restaurant?.closing!!)
            restaurant_time.text = status?.get("msg")

            when (status?.get("clr")) {
                "green" -> {
                    restaurant_work_status.background =
                        resources.getDrawable(R.drawable.background_time_green)
                }
                "orange" -> {
                    restaurant_work_status.background =
                        resources.getDrawable(R.drawable.background_time_orange)
                }
                "red" -> {
                    restaurant_work_status.background =
                        resources.getDrawable(R.drawable.background_time_red)
                }
            }

            Timer().scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    runOnUiThread {

                        val statusTimer = utilsFunctions.statusWorkTime(
                            branch.restaurant?.opening!!,
                            branch.restaurant?.closing!!
                        )
                        restaurant_time.text = statusTimer?.get("msg")

                        when (statusTimer?.get("clr")) {
                            "green" -> {
                                restaurant_work_status.background =
                                    resources.getDrawable(R.drawable.background_time_green)
                            }
                            "orange" -> {
                                restaurant_work_status.background =
                                    resources.getDrawable(R.drawable.background_time_orange)
                            }
                            "red" -> {
                                restaurant_work_status.background =
                                    resources.getDrawable(R.drawable.background_time_red)
                            }
                        }
                    }
                }
            }, 0, 10000)
        } else {
            restaurant_work_status.background = resources.getDrawable(R.drawable.background_time_red)
            restaurant_work_status_icon.setImageDrawable(resources.getDrawable(R.drawable.ic_close_white_24dp))
            restaurant_time.text = "Not Available"
        }

        if (utilsFunctions.checkLocationPermissions()) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener {
                    if (it != null) {
                        val from = LatLng(it.latitude, it.longitude)
                        selectedLatitude = it.latitude
                        selectedLongitude = it.longitude
                        val to = LatLng(branch.latitude, branch.longitude)
                        val dist = utilsFunctions.calculateDistance(from, to)
                        branch.dist = dist
                        branch.fromLocation = from
                        runOnUiThread { restaurant_distance.text = "${dist.toString()} Km" }
                    } else {
                        val from = LatLng(
                            session.addresses!!.addresses[0].latitude,
                            session.addresses!!.addresses[0].longitude
                        )
                        selectedLatitude = session.addresses!!.addresses[0].latitude
                        selectedLongitude = session.addresses!!.addresses[0].longitude
                        val to = LatLng(branch.latitude, branch.longitude)
                        val dist = utilsFunctions.calculateDistance(from, to)
                        branch.dist = dist
                        branch.fromLocation = from
                        runOnUiThread { restaurant_distance.text = "${dist.toString()} Km" }
                    }
                }.addOnFailureListener {
                    val from = LatLng(
                        session.addresses!!.addresses[0].latitude,
                        session.addresses!!.addresses[0].longitude
                    )
                    selectedLatitude = session.addresses!!.addresses[0].latitude
                    selectedLongitude = session.addresses!!.addresses[0].longitude
                    val to = LatLng(branch.latitude, branch.longitude)
                    val dist = utilsFunctions.calculateDistance(from, to)
                    branch.dist = dist
                    branch.fromLocation = from
                    runOnUiThread { restaurant_distance.text = "${dist.toString()} Km" }
                    TingToast(
                        this@RestaurantProfile,
                        it.message!!,
                        TingToastType.ERROR
                    ).showToast(Toast.LENGTH_LONG)
                }
            } catch (e: java.lang.Exception) {
                TingToast(
                    this@RestaurantProfile,
                    e.message!!.capitalize(),
                    TingToastType.ERROR
                ).showToast(Toast.LENGTH_LONG)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.restaurant_profile_menu, menu)
        if (menu is MenuBuilder) {
            val m = menu as MenuBuilder
            m.setOptionalIconsVisible(true)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.restaurant_profile_reviews -> {
                val intent = Intent(this@RestaurantProfile, RestaurantReviews::class.java)
                intent.putExtra("resto", branch.id)
                intent.putExtra("apiGet", branch.urls.apiGet)
                intent.putExtra("url", branch.urls.apiReviews)
                startActivity(intent)
                return true
            }
            R.id.restaurant_profile_likes -> {
                val intent = Intent(this@RestaurantProfile, RestaurantLikes::class.java)
                intent.putExtra("resto", branch.id)
                intent.putExtra("apiGet", branch.urls.apiGet)
                intent.putExtra("url", branch.urls.apiLikes)
                startActivity(intent)
                return true
            }
            R.id.restaurant_profile_about -> {
                val intent = Intent(this@RestaurantProfile, RestaurantAbout::class.java)
                intent.putExtra("resto", branch.id)
                intent.putExtra("apiGet", branch.urls.apiGet)
                startActivity(intent)
                return true
            }
        }
        return false
    }


    internal class RestaurantProfileViewPagerAdapter(fm: FragmentManager): FragmentPagerAdapter(fm) {

        private val fragments: MutableList<Fragment> = ArrayList()
        private val fragmentsTitle: MutableList<String> = ArrayList()

        override fun getItem(position: Int): Fragment = fragments[position]

        override fun getCount(): Int = fragments.size

        override fun getPageTitle(position: Int): CharSequence? = fragmentsTitle[position]

        public fun addFragment(fragment: Fragment, title: String){
            this.fragments.add(fragment)
            this.fragmentsTitle.add(title)
        }
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
        try { restaurantTimer.cancel() } catch (e: java.lang.Exception) {}
        Bridge.clear(this)
    }

    override fun onPause() {
        super.onPause()
        try { restaurantTimer.cancel() } catch (e: java.lang.Exception) {}
    }
}
