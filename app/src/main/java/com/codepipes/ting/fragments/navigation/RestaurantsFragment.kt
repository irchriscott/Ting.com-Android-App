package com.codepipes.ting.fragments.navigation


import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codepipes.ting.R
import com.codepipes.ting.fragments.restaurants.RestaurantsMapFragment
import android.R.attr.start
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.view.ViewAnimationUtils
import android.opengl.ETC1.getHeight
import android.opengl.ETC1.getWidth
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.widget.Toast
import com.codepipes.ting.adapters.restaurant.GlobalRestaurantAdapter
import com.codepipes.ting.customclasses.ActionSheet
import com.codepipes.ting.dialogs.TingToast
import com.codepipes.ting.dialogs.TingToastType
import com.codepipes.ting.interfaces.ActionSheetCallBack
import com.codepipes.ting.interfaces.SuccessDialogCloseListener
import com.codepipes.ting.models.Branch
import com.codepipes.ting.models.User
import com.codepipes.ting.providers.APILoadGlobalRestaurants
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.UtilsFunctions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.include_empty_data.view.*
import okhttp3.*
import java.io.IOException
import java.lang.Exception
import java.time.Duration
import java.util.*
import kotlin.collections.ArrayList


class RestaurantsFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var mOpenRestaurantMapButton: FloatingActionButton
    private lateinit var mRestaurantsRecyclerView: RecyclerView
    private lateinit var mProgressLoader: View
    private lateinit var mEmptyDataView: View
    private lateinit var mRefreshRestaurant: SwipeRefreshLayout

    private lateinit var activity: Activity

    private val routes: Routes = Routes()
    private val gson: Gson = Gson()

    private var isAsync: Boolean = false
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mUtilFunctions: UtilsFunctions

    private lateinit var session: User
    private lateinit var userAuthentication: UserAuthentication

    private lateinit var restaurants: MutableList<Branch>

    private var selectedLatitude: Double = 0.0
    private var selectedLongitude: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @SuppressLint("SetTextI18n", "NewApi", "MissingPermission")
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_restaurants, container, false)

        restaurants = ArrayList()

        activity = context!! as Activity
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
        mUtilFunctions = UtilsFunctions(context!!)

        userAuthentication = UserAuthentication(context!!)
        session = userAuthentication.get()!!

        mOpenRestaurantMapButton = view.findViewById(R.id.open_restaurant_map) as FloatingActionButton
        mOpenRestaurantMapButton.setOnClickListener {
            val cx = (mOpenRestaurantMapButton.x + mOpenRestaurantMapButton.width / 2).toInt()
            val cy = (mOpenRestaurantMapButton.y + mOpenRestaurantMapButton.height + 56).toInt()

            val mapFragment =  RestaurantsMapFragment()
            val args: Bundle = Bundle()

            args.putInt("cx", cx)
            args.putInt("cy", cy)
            args.putDouble("lat", selectedLatitude)
            args.putDouble("lng", selectedLongitude)

            if(!restaurants.isNullOrEmpty()){
                args.putString("restos", gson.toJson(restaurants))
            }

            mapFragment.arguments = args
            mapFragment.show(fragmentManager!!, mapFragment.tag)
        }

        mOpenRestaurantMapButton.isClickable = false

        val menuList = mutableListOf<String>()
        menuList.add("Current Location")
        session.addresses?.addresses!!.forEach {
            menuList.add("${it.type} - ${it.address}")
        }

        mOpenRestaurantMapButton.setOnLongClickListener {
            val actionSheet = ActionSheet(context!!, menuList)
                .setTitle("Restaurant Near Location")
                .setColorData(activity.resources.getColor(R.color.colorGray))
                .setColorTitleCancel(activity.resources.getColor(R.color.colorGoogleRedTwo))
                .setColorSelected(activity.resources.getColor(R.color.colorPrimary))
                .setCancelTitle("Cancel")

            actionSheet.create(object : ActionSheetCallBack {

                override fun data(data: String, position: Int) {
                    if(!restaurants.isNullOrEmpty()){
                        if(position == 0){
                            if(mUtilFunctions.checkLocationPermissions()){
                                try {
                                    fusedLocationClient.lastLocation.addOnSuccessListener {
                                        if(it != null){
                                            val from = LatLng(it.latitude, it.longitude)
                                            selectedLatitude = it.latitude
                                            selectedLongitude = it.longitude
                                            restaurants.forEach { b ->
                                                val to = LatLng(b.latitude, b.longitude)
                                                val dist = mUtilFunctions.calculateDistance(from, to)
                                                b.dist = dist
                                                b.fromLocation = from
                                            }
                                        } else {
                                            val from = LatLng(session.addresses!!.addresses[0].latitude, session.addresses!!.addresses[0].longitude)
                                            selectedLatitude = session.addresses!!.addresses[0].latitude
                                            selectedLongitude = session.addresses!!.addresses[0].longitude
                                            restaurants.forEach { b ->
                                                val to = LatLng(b.latitude, b.longitude)
                                                val dist = mUtilFunctions.calculateDistance(from, to)
                                                b.dist = dist
                                                b.fromLocation = from
                                            }
                                        }
                                        restaurants.sortBy { b -> b.dist }
                                        mRestaurantsRecyclerView.layoutManager = LinearLayoutManager(context)
                                        mRestaurantsRecyclerView.adapter = GlobalRestaurantAdapter(restaurants, fragmentManager!!)
                                    }.addOnFailureListener {
                                        val from = LatLng(session.addresses!!.addresses[0].latitude, session.addresses!!.addresses[0].longitude)
                                        selectedLatitude = session.addresses!!.addresses[0].latitude
                                        selectedLongitude = session.addresses!!.addresses[0].longitude
                                        restaurants.forEach { b ->
                                            val to = LatLng(b.latitude, b.longitude)
                                            val dist = mUtilFunctions.calculateDistance(from, to)
                                            b.dist = dist
                                            b.fromLocation = from
                                        }
                                        restaurants.sortBy { b -> b.dist }
                                        mRestaurantsRecyclerView.layoutManager = LinearLayoutManager(context)
                                        mRestaurantsRecyclerView.adapter = GlobalRestaurantAdapter(restaurants,fragmentManager!!)
                                        TingToast(context!!, it.message!!, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                                    }
                                } catch (e: Exception){ TingToast(context!!, e.message!!.capitalize(), TingToastType.ERROR).showToast(Toast.LENGTH_LONG) }
                            }
                        } else {
                            val address = session.addresses?.addresses!![position - 1]
                            val from = LatLng(address.latitude, address.longitude)
                            selectedLatitude = address.latitude
                            selectedLongitude = address.longitude
                            restaurants.forEach { b ->
                                val to = LatLng(b.latitude, b.longitude)
                                val dist = mUtilFunctions.calculateDistance(from, to)
                                b.dist = dist
                                b.fromLocation = from
                            }
                            restaurants.sortBy { b -> b.dist }
                            mRestaurantsRecyclerView.layoutManager = LinearLayoutManager(context)
                            mRestaurantsRecyclerView.adapter = GlobalRestaurantAdapter(restaurants, fragmentManager!!)
                        }
                    }
                }
            })

            return@setOnLongClickListener true
        }

        mRestaurantsRecyclerView = view.findViewById<RecyclerView>(R.id.restaurants_recycler_view) as RecyclerView
        mProgressLoader = view.findViewById<View>(R.id.progress_loader) as View
        mEmptyDataView = view.findViewById<View>(R.id.empty_data) as View
        mRefreshRestaurant = view.findViewById<SwipeRefreshLayout>(R.id.refresh_restaurants) as SwipeRefreshLayout

        mRefreshRestaurant.setColorSchemeColors(context!!.getColor(R.color.colorPrimary), context!!.getColor(R.color.colorAccentMain), context!!.getColor(R.color.colorPrimaryDark), context!!.getColor(R.color.colorAccentMain))
        mRefreshRestaurant.setOnRefreshListener(this)

        if(isAsync){
            val asyncTask = APILoadGlobalRestaurants(context!!).execute()
            val restaurants = asyncTask.get()

            if(!restaurants.isNullOrEmpty()){
                mOpenRestaurantMapButton.isClickable = false
                mRestaurantsRecyclerView.visibility = View.VISIBLE
                mProgressLoader.visibility = View.GONE
                mEmptyDataView.visibility = View.GONE
                mRestaurantsRecyclerView.layoutManager = LinearLayoutManager(context)
                mRestaurantsRecyclerView.adapter = GlobalRestaurantAdapter(restaurants, fragmentManager!!)
            } else {
                mOpenRestaurantMapButton.isClickable = false
                mRestaurantsRecyclerView.visibility = View.GONE
                mProgressLoader.visibility = View.GONE
                mEmptyDataView.visibility = View.VISIBLE
                mEmptyDataView.empty_image.setImageResource(R.drawable.ic_restaurants)
                mEmptyDataView.empty_text.text = "No Restaurant To Show"
                TingToast(context!!, "No Restaurant To Show", TingToastType.DEFAULT).showToast(Toast.LENGTH_LONG)
            }
        }
        this.getRestaurants()

        return view
    }

    @SuppressLint("SetTextI18n", "MissingPermission", "NewApi")
    private fun getRestaurants(){
        val url = routes.restaurantsGlobal
        val client = OkHttpClient.Builder().callTimeout(Duration.ofMinutes(5)).build()

        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                activity.runOnUiThread {
                    mRestaurantsRecyclerView.visibility = View.GONE
                    mProgressLoader.visibility = View.GONE
                    mEmptyDataView.visibility = View.VISIBLE
                    mEmptyDataView.empty_image.setImageResource(R.drawable.ic_restaurants)
                    mEmptyDataView.empty_text.text = "No Restaurant To Show"
                    TingToast(context!!, e.message!!.capitalize(), TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body()!!.string()
                restaurants = gson.fromJson<MutableList<Branch>>(dataString, object : TypeToken<MutableList<Branch>>(){}.type)

                activity.runOnUiThread{
                    mRefreshRestaurant.isRefreshing = false
                    if(!restaurants.isNullOrEmpty()){
                        if(mUtilFunctions.checkLocationPermissions()){
                            try {
                                fusedLocationClient.lastLocation.addOnSuccessListener {
                                    if(it != null){
                                        val from = LatLng(it.latitude, it.longitude)
                                        selectedLatitude = it.latitude
                                        selectedLongitude = it.longitude
                                        restaurants.forEach { b ->
                                            val to = LatLng(b.latitude, b.longitude)
                                            val dist = mUtilFunctions.calculateDistance(from, to)
                                            b.dist = dist
                                            b.fromLocation = from
                                        }
                                    } else {
                                        val from = LatLng(session.addresses!!.addresses[0].latitude, session.addresses!!.addresses[0].longitude)
                                        selectedLatitude = session.addresses!!.addresses[0].latitude
                                        selectedLongitude = session.addresses!!.addresses[0].longitude
                                        restaurants.forEach { b ->
                                            val to = LatLng(b.latitude, b.longitude)
                                            val dist = mUtilFunctions.calculateDistance(from, to)
                                            b.dist = dist
                                            b.fromLocation = from
                                        }
                                    }
                                    restaurants.sortBy { b -> b.dist }
                                    mOpenRestaurantMapButton.isClickable = false
                                    mRestaurantsRecyclerView.visibility = View.VISIBLE
                                    mProgressLoader.visibility = View.GONE
                                    mEmptyDataView.visibility = View.GONE
                                    mRestaurantsRecyclerView.layoutManager = LinearLayoutManager(context)
                                    mRestaurantsRecyclerView.adapter = GlobalRestaurantAdapter(restaurants, fragmentManager!!)
                                }.addOnFailureListener {
                                    val from = LatLng(session.addresses!!.addresses[0].latitude, session.addresses!!.addresses[0].longitude)
                                    selectedLatitude = session.addresses!!.addresses[0].latitude
                                    selectedLongitude = session.addresses!!.addresses[0].longitude
                                    restaurants.forEach { b ->
                                        val to = LatLng(b.latitude, b.longitude)
                                        val dist = mUtilFunctions.calculateDistance(from, to)
                                        b.dist = dist
                                        b.fromLocation = from
                                    }
                                    mOpenRestaurantMapButton.isClickable = false
                                    mRestaurantsRecyclerView.visibility = View.VISIBLE
                                    mProgressLoader.visibility = View.GONE
                                    mEmptyDataView.visibility = View.GONE
                                    mRestaurantsRecyclerView.layoutManager = LinearLayoutManager(context)
                                    mRestaurantsRecyclerView.adapter = GlobalRestaurantAdapter(restaurants,fragmentManager!!)
                                    TingToast(context!!, it.message!!.capitalize(), TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                                }
                            } catch (e: Exception){ TingToast(context!!, e.message!!.capitalize(), TingToastType.ERROR).showToast(Toast.LENGTH_LONG) }
                        }
                    } else {
                        mRestaurantsRecyclerView.visibility = View.GONE
                        mProgressLoader.visibility = View.GONE
                        mEmptyDataView.visibility = View.VISIBLE
                        mEmptyDataView.empty_image.setImageResource(R.drawable.ic_restaurants)
                        mEmptyDataView.empty_text.text = "No Restaurant To Show"
                        TingToast(context!!, "No Restaurant To Show", TingToastType.DEFAULT).showToast(Toast.LENGTH_LONG)
                    }
                }
            }
        })
    }

    override fun onRefresh() {
        mRefreshRestaurant.isRefreshing = true
        this.getRestaurants()
    }
}