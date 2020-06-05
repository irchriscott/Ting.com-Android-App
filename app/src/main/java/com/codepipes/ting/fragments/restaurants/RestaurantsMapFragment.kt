package com.codepipes.ting.fragments.restaurants

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.res.AssetManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import android.view.*
import android.widget.ImageButton
import android.widget.Toast
import com.caverock.androidsvg.SVG
import com.codepipes.ting.R
import com.codepipes.ting.activities.restaurant.RestaurantProfile
import com.codepipes.ting.custom.ProgressWheel
import com.codepipes.ting.custom.RestaurantInfoWindowMap
import com.codepipes.ting.dialogs.messages.TingToast
import com.codepipes.ting.dialogs.messages.TingToastType
import com.codepipes.ting.interfaces.RetrofitGoogleMapsRoute
import com.codepipes.ting.models.Branch
import com.codepipes.ting.models.MapPin
import com.codepipes.ting.models.PolylineMapRoute
import com.codepipes.ting.models.User
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.UtilsFunctions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.livefront.bridge.Bridge
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_restaurants_map.view.*
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.hypot


@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class RestaurantsMapFragment : DialogFragment(), OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener {

    private lateinit var mCloseRestaurantMapButton: ImageButton
    private lateinit var mMapViewContainer: ConstraintLayout
    private lateinit var mProgressWheel: ProgressWheel

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var geocoder: Geocoder
    private lateinit var mUtilFunctions: UtilsFunctions
    private lateinit var gson: Gson

    private var cx: Int = 0
    private var cy: Int = 0

    private lateinit var branch: Branch
    private lateinit var timer: Timer

    private lateinit var polyline: Polyline
    private lateinit var session: User
    private lateinit var userAuthentication: UserAuthentication

    private var handler: Handler? = null
    private var mapCenter: LatLng = LatLng(0.00, 0.00)

    private lateinit var assetManager: AssetManager

    private lateinit var userMapPin: BitmapDescriptor
    private lateinit var restaurantMapPin: BitmapDescriptor

    private lateinit var fromLocation: LatLng

    private var restaurant: String? = null
    private var restaurantsString: String? = null

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private val runnable: Runnable = Runnable {
        mProgressWheel.visibility = View.GONE
        this.showRestaurantMap(mMapViewContainer, cx, cy)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme_FullScreenDialog)
        super.onCreate(savedInstanceState)
        Bridge.restoreInstanceState(this, savedInstanceState)
        MapsInitializer.initialize(context)
        savedInstanceState?.clear()
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_restaurants_map, container, false)

        gson = Gson()
        timer = Timer()

        userAuthentication = UserAuthentication(context!!)
        session = userAuthentication.get()!!

        val mArgs = arguments
        cx = mArgs!!.getInt("cx")
        cy = mArgs.getInt("cy")

        mapCenter = LatLng(mArgs.getDouble("lat"), mArgs.getDouble("lng"))

        mMapViewContainer = view.findViewById<ConstraintLayout>(R.id.view_map_container) as ConstraintLayout
        mProgressWheel = view.findViewById<ProgressWheel>(R.id.progress_wheel) as ProgressWheel

        val mapFragment = fragmentManager!!.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        geocoder = Geocoder(context, Locale.getDefault())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity!!)
        mUtilFunctions = UtilsFunctions(activity!!)

        mCloseRestaurantMapButton = view.findViewById(R.id.close_restaurant_map) as ImageButton
        mCloseRestaurantMapButton.setOnClickListener { this.hideRestaurantMap(view, cx, cy, dialog!!) }

        dialog?.setOnKeyListener { _, keyCode, _ ->
            if(keyCode == KeyEvent.KEYCODE_BACK){
                this.hideRestaurantMap(view, cx, cy, this.dialog!!)
                return@setOnKeyListener  true
            }
            return@setOnKeyListener false
        }

        assetManager = context!!.assets

        val restoSharp = SVG.getFromAsset(assetManager, "restaurant_pin.svg")
        restaurantMapPin = mUtilFunctions.vectorToBitmap(restoSharp)

        val userSharp = SVG.getFromAsset(assetManager, "user_pin.svg")
        userMapPin = mUtilFunctions.vectorToBitmap(userSharp)

        requestUserMapPin()

        handler = Handler()
        handler?.postDelayed(runnable, 2000)

        if(!restaurant.isNullOrEmpty() && !restaurant.isNullOrBlank()){
            view.restaurant_view.visibility = View.VISIBLE
            branch = gson.fromJson(restaurant, Branch::class.java)

            Picasso.get().load(branch.restaurant?.logoURL()).into(view.restaurant_image)
            view.restaurant_name.text = "${branch.restaurant?.name}, ${branch.name}"
            view.restaurant_rating.rating = branch.reviews?.average!!.toFloat()
            view.restaurant_address.text = branch.address
            view.restaurant_distance.text = "${branch.dist} km"

            requestRestaurantMapPin(branch.id)

            if(branch.isAvailable) {

                timer.scheduleAtFixedRate(object : TimerTask() {
                    override fun run() {
                        activity?.runOnUiThread {
                            val status = mUtilFunctions.statusWorkTime(
                                branch.restaurant?.opening!!,
                                branch.restaurant?.closing!!
                            )
                            view.restaurant_time.text = status["msg"]

                            when (status["clr"]) {
                                "green" -> {
                                    view.restaurant_work_status.background =
                                        view.context.resources.getDrawable(R.drawable.background_time_green)
                                }
                                "orange" -> {
                                    view.restaurant_work_status.background =
                                        view.context.resources.getDrawable(R.drawable.background_time_orange)
                                }
                                "red" -> {
                                    view.restaurant_work_status.background =
                                        view.context.resources.getDrawable(R.drawable.background_time_red)
                                }
                            }
                        }
                    }
                }, 0, 10000)
            } else {
                view.restaurant_work_status.background = view.context.resources.getDrawable(R.drawable.background_time_red)
                view.restaurant_work_status_icon.setImageDrawable(view.context.resources.getDrawable(R.drawable.ic_close_white_24dp))
                view.restaurant_time.text = "Not Available"
            }

        } else { view.restaurant_view.visibility = View.GONE }

        return view
    }


    @SuppressLint("MissingPermission", "DefaultLocale")
    override fun onMapReady(googleMap: GoogleMap) {

        mMap = googleMap

        val mArgs = arguments

        fromLocation = LatLng(session.addresses!!.addresses[0].latitude, session.addresses!!.addresses[0].longitude)

        if(!restaurant.isNullOrBlank() && !restaurant.isNullOrEmpty()){

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(branch.latitude, branch.longitude), GOOGLE_MAPS_ZOOM))

            val branchString = gson.toJson(branch)
            val marker = MarkerOptions().position(LatLng(branch.latitude, branch.longitude)).title(branchString).icon(restaurantMapPin)
            val infoWindowAdapter = RestaurantInfoWindowMap(context!!)
            mMap.setInfoWindowAdapter(infoWindowAdapter)
            mMap.addMarker(marker)

            mMap.setOnInfoWindowClickListener(this)

            val destination = LatLng(branch.latitude, branch.longitude)
            view?.restaurant_direction_driving?.setOnClickListener {
                view?.restaurant_direction_driving?.background = view?.context?.resources?.getDrawable(R.drawable.background_label_button_active)
                view?.restaurant_direction_walking?.background = view?.context?.resources?.getDrawable(R.drawable.background_labeled_button)
                this.getPolyline(branch.fromLocation ?: fromLocation, destination, "driving")
            }
            view?.restaurant_direction_walking?.setOnClickListener {
                view?.restaurant_direction_walking?.background = view?.context?.resources?.getDrawable(R.drawable.background_label_button_active)
                view?.restaurant_direction_driving?.background = view?.context?.resources?.getDrawable(R.drawable.background_labeled_button)
                this.getPolyline(branch.fromLocation ?: fromLocation, destination, "walking")
            }
        } else {

            if(mUtilFunctions.checkLocationPermissions()){ mMap.isMyLocationEnabled = true }

            if(mUtilFunctions.checkLocationPermissions()){
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener {
                        if(it != null) { this.getLocation(it) }
                    }.addOnFailureListener {
                        activity?.runOnUiThread {
                            TingToast(
                                context!!,
                                it.message!!,
                                TingToastType.ERROR
                            ).showToast(Toast.LENGTH_LONG)
                        }
                    }
                } catch (e: java.lang.Exception){
                    TingToast(
                        context!!,
                        activity!!.resources.getString(R.string.error_internet),
                        TingToastType.ERROR
                    ).showToast(
                        Toast.LENGTH_LONG)
                }
            }

            if(!restaurantsString.isNullOrBlank() && !restaurantsString.isNullOrEmpty()){

                val branches = gson.fromJson<MutableList<Branch>>(restaurantsString, object : TypeToken<MutableList<Branch>>(){}.type)

                if(branches.size > 0){

                    for(b in branches){
                        val branchString = gson.toJson(b)
                        val marker = MarkerOptions().position(LatLng(b.latitude, b.longitude)).title(branchString).icon(restaurantMapPin)
                        val infoWindowAdapter = RestaurantInfoWindowMap(context!!)
                        mMap.setInfoWindowAdapter(infoWindowAdapter)

                        val mMarker = mMap.addMarker(marker)
                        mMarker.tag = b.id
                        mMarker.zIndex = b.id.toFloat()
                    }
                    mMap.setOnInfoWindowClickListener(this)
                }
            }
        }

        mMap.setOnMyLocationButtonClickListener {
            if(mUtilFunctions.checkLocationPermissions()){
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener {
                        this.getLocation(it)
                    }.addOnFailureListener {
                        activity?.runOnUiThread {
                            fromLocation = LatLng(session.addresses!!.addresses[0].latitude, session.addresses!!.addresses[0].longitude)
                            TingToast(
                                context!!,
                                it.message!!.capitalize(),
                                TingToastType.ERROR
                            ).showToast(Toast.LENGTH_LONG)
                        }
                    }
                } catch (e: Exception){
                    fromLocation = LatLng(session.addresses!!.addresses[0].latitude, session.addresses!!.addresses[0].longitude)
                    TingToast(
                        context!!,
                        activity!!.resources.getString(R.string.error_internet),
                        TingToastType.ERROR
                    ).showToast(
                        Toast.LENGTH_LONG)
                }
            }
            true
        }
    }

    private fun getLocation(location: Location){
        fromLocation = try {
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (mapCenter.latitude != 0.0 && mapCenter.longitude != 0.0) { mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mapCenter, GOOGLE_MAPS_ZOOM)) } else { mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), GOOGLE_MAPS_ZOOM)) }
            LatLng(location.latitude, location.longitude)
        } catch (e: Exception){
            TingToast(
                context!!,
                activity!!.resources.getString(R.string.error_internet),
                TingToastType.ERROR
            ).showToast(
                Toast.LENGTH_LONG)
            LatLng(session.addresses!!.addresses[0].latitude, session.addresses!!.addresses[0].longitude)
        }
    }

    private fun requestUserMapPin() {
        val url = "${Routes.userMapPin}${session.id}/"
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .header("Authorization", session.token!!)
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {

            override fun onFailure(call: okhttp3.Call, e: IOException) {
                activity!!.runOnUiThread {
                    val assetManager = context!!.assets
                    val sharp = SVG.getFromAsset(assetManager, "user_pin.svg")
                    userMapPin = mUtilFunctions.vectorToBitmap(sharp)
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val responseBody = response.body!!.string()
                userMapPin = try {
                    val pin = gson.fromJson(responseBody, MapPin::class.java)
                    val sharp = SVG.getFromString(pin.pin)
                    mUtilFunctions.vectorToBitmap(sharp)
                } catch (e: Exception) {
                    val assetManager = context!!.assets
                    val sharp = SVG.getFromAsset(assetManager, "user_pin.svg")
                    mUtilFunctions.vectorToBitmap(sharp)
                }
            }
        })
    }

    private fun requestRestaurantMapPin(restoId: Int) {
        val url = "${Routes.restaurantMapPin}${restoId}/"
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .header("Authorization", session.token!!)
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {

            override fun onFailure(call: okhttp3.Call, e: IOException) {
                activity!!.runOnUiThread {
                    val assetManager = context!!.assets
                    val sharp = SVG.getFromAsset(assetManager, "restaurant_pin.svg")
                    restaurantMapPin = mUtilFunctions.vectorToBitmap(sharp)
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val responseBody = response.body!!.string()
                restaurantMapPin = try {
                    val pin = gson.fromJson(responseBody, MapPin::class.java)
                    val sharp = SVG.getFromString(pin.pin)
                    mUtilFunctions.vectorToBitmap(sharp)
                } catch (e: Exception) {
                    val assetManager = context!!.assets
                    val sharp = SVG.getFromAsset(assetManager, "restaurant_pin.svg")
                    mUtilFunctions.vectorToBitmap(sharp)
                }
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun showRestaurantMap(view: View, cx: Int, cy: Int) {

        val w = view.width
        val h = view.height

        val endRadius = hypot(w.toDouble(), h.toDouble()).toInt()

        val revealAnimator = ViewAnimationUtils.createCircularReveal(view, cx, cy.toInt(), 0f, endRadius.toFloat())

        view.visibility = View.VISIBLE
        revealAnimator.duration = 700
        revealAnimator.start()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun hideRestaurantMap(view: View, cx: Int, cy: Int, dialog: Dialog){
        val w = view.width
        val h = view.height

        val endRadius = hypot(w.toDouble(), h.toDouble()).toInt()

        val anim = ViewAnimationUtils.createCircularReveal(view, cx, cy.toInt(), endRadius.toFloat(), 0f)

        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                dialog.dismiss()
            }
        })
        anim.duration = 700
        anim.start()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        val touchOutsideView = dialog.window?.findViewById<View>(com.google.android.material.R.id.touch_outside)
        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer.cancel()
        val f = fragmentManager!!.findFragmentById(R.id.map)
        fragmentManager!!.beginTransaction().remove(f!!).commit()
        Bridge.clear(this)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        timer.cancel()
        handler?.removeCallbacks(runnable)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(WindowManager.LayoutParams.FILL_PARENT, WindowManager.LayoutParams.FILL_PARENT)
    }

    override fun onInfoWindowClick(marker: Marker?) {

        val data = marker?.title
        val intent = Intent(activity!!, RestaurantProfile::class.java)
        val branch = gson.fromJson(data, Branch::class.java)
        intent.putExtra("resto", branch.id)
        intent.putExtra("tab", 0)
        activity?.startActivity(intent)
    }

    @SuppressLint("DefaultLocale")
    private fun getPolyline(origin: LatLng?, destination: LatLng?, type: String){
        val originLocation = origin ?: fromLocation
        if (destination != null) {
            val url = "https://maps.googleapis.com/maps/"

            val retrofit = Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(RetrofitGoogleMapsRoute::class.java)
            val call = service.getDistanceDuration("metric",  "${originLocation.latitude},${originLocation.longitude}", "${destination.latitude},${destination.longitude}", type)

            call.enqueue(object : Callback<PolylineMapRoute> {


                override fun onFailure(call: Call<PolylineMapRoute>, t: Throwable) {
                    activity?.runOnUiThread {
                        TingToast(
                            context!!,
                            t.message!!.capitalize(),
                            TingToastType.ERROR
                        ).showToast(Toast.LENGTH_LONG)
                    }
                }

                @SuppressLint("SetTextI18n")
                override fun onResponse(call: Call<PolylineMapRoute>, response: Response<PolylineMapRoute>) {
                    try {
                        activity?.runOnUiThread {

                            mMap.clear()

                            val branchString = gson.toJson(branch)
                            val marker = MarkerOptions().position(LatLng(branch.latitude, branch.longitude)).title(branchString).icon(restaurantMapPin)
                            val infoWindowAdapter = RestaurantInfoWindowMap(context!!)
                            mMap.setInfoWindowAdapter(infoWindowAdapter)
                            mMap.addMarker(marker)

                            mMap.addMarker(MarkerOptions().position(LatLng(originLocation.latitude, originLocation.longitude)).icon(userMapPin))

                            response.body()?.routes!!.forEach {
                                val i = response.body()?.routes!!.indexOf(it)
                                val  distance = it.legs[i].distance.text
                                val time = it.legs[i].duration.text
                                view?.restaurant_direction_text!!.visibility = View.VISIBLE
                                view?.restaurant_direction_text!!.text = "$time ($distance)"

                                val encodedString = response.body()?.routes!![0].overview_polyline.points
                                val linesList = mUtilFunctions.decodePoly(encodedString)
                                polyline = mMap.addPolyline(
                                    PolylineOptions()
                                        .addAll(linesList)
                                        .width(20.0f)
                                        .color(Color.parseColor("#BBB56FE8"))
                                        .geodesic(true))
                            }

                            mMap.setMinZoomPreference(2.0f)
                            mMap.setMaxZoomPreference(17.0f)

                            val latLngBounds = LatLngBounds.builder()
                            latLngBounds.include(origin)
                            latLngBounds.include(destination)

                            val bounds = latLngBounds.build()
                            mMap.setPadding(120, 120, 120, 120)
                            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0))

                        }
                    } catch (e: java.lang.Exception) {
                        activity?.runOnUiThread {
                            TingToast(
                                context!!,
                                activity!!.resources.getString(R.string.error_internet),
                                TingToastType.ERROR
                            ).showToast(Toast.LENGTH_LONG)
                        }
                    }
                }
            })
        } else { TingToast(
            context!!,
            "An Error Has occurred",
            TingToastType.ERROR
        ).showToast(Toast.LENGTH_LONG) }
    }

    public fun setRestaurant(resto: String) {
        restaurant = resto
    }

    public fun setRestaurants(restos: String) {
        restaurantsString = restos
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Bridge.saveInstanceState(this, outState)
        outState.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        Bridge.clear(this)
    }

    companion object {
        private const val GOOGLE_MAPS_ZOOM = 16.0f
    }
}
