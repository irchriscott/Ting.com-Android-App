package com.codepipes.ting.fragments.restaurants

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.annotation.RequiresApi
import android.support.constraint.ConstraintLayout
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentContainer
import android.view.*
import android.widget.ImageButton
import android.widget.Toast
import com.caverock.androidsvg.SVG
import com.codepipes.ting.R
import com.codepipes.ting.dialogs.TingToast
import com.codepipes.ting.dialogs.TingToastType
import com.codepipes.ting.interfaces.SuccessDialogCloseListener
import com.codepipes.ting.models.Branch
import com.codepipes.ting.utils.UtilsFunctions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import com.pnikosis.materialishprogress.ProgressWheel
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_restaurants_map.view.*
import java.util.*


@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class RestaurantsMapFragment : DialogFragment(), OnMapReadyCallback {

    private lateinit var mCloseRestaurantMapButton: ImageButton
    private lateinit var mMapViewContainer: ConstraintLayout
    private lateinit var mProgressWheel: ProgressWheel

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val GOOGLE_MAPS_ZOOM = 16.0f

    private lateinit var geocoder: Geocoder
    private lateinit var mUtilFunctions: UtilsFunctions
    private lateinit var gson: Gson

    private var cx: Int = 0
    private var cy: Int = 0

    private lateinit var branch: Branch
    private lateinit var timer: Timer

    private var handler: Handler? = null
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private val runnable: Runnable = Runnable {
        mProgressWheel.visibility = View.GONE
        this.showRestaurantMap(mMapViewContainer, cx, cy)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme_FullScreenDialog)
        super.onCreate(savedInstanceState)
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

        val mArgs = arguments
        cx = mArgs!!.getInt("cx")
        cy = mArgs.getInt("cy")

        mMapViewContainer = view.findViewById<ConstraintLayout>(R.id.view_map_container) as ConstraintLayout
        mProgressWheel = view.findViewById<ProgressWheel>(R.id.progress_wheel) as ProgressWheel

        val mapFragment = fragmentManager!!.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        geocoder = Geocoder(activity, Locale.getDefault())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity!!)
        mUtilFunctions = UtilsFunctions(activity!!)

        mCloseRestaurantMapButton = view.findViewById<ImageButton>(R.id.close_restaurant_map) as ImageButton
        mCloseRestaurantMapButton.setOnClickListener { this.hideRestaurantMap(view, cx, cy, dialog) }

        dialog.setOnKeyListener { _, keyCode, _ ->
            if(keyCode == KeyEvent.KEYCODE_BACK){
                this.hideRestaurantMap(view, cx, cy, this.dialog)
                return@setOnKeyListener  true
            }
            return@setOnKeyListener false
        }

        handler = Handler()
        handler?.postDelayed(runnable, 2000)

        val restaurant = mArgs.getString("resto")

        if(!restaurant.isNullOrEmpty() && !restaurant.isNullOrBlank()){
            view.restaurant_view.visibility = View.VISIBLE
            branch = gson.fromJson(restaurant, Branch::class.java)

            Picasso.get().load(branch.restaurant?.logoURL()).into(view.restaurant_image)
            view.restaurant_name.text = "${branch.restaurant?.name}, ${branch.name}"
            view.restaurant_rating.rating = branch.reviews?.average!!.toFloat()
            view.restaurant_address.text = branch.address
            view.restaurant_distance.text = "${branch.dist} km"

            timer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    activity!!.runOnUiThread {
                        val status = mUtilFunctions.statusWorkTime(branch.restaurant?.opening!!, branch.restaurant?.closing!!)
                        view.restaurant_time.text = status?.get("msg")

                        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
                        when(status?.get("clr")){
                            "green" -> {
                                view.restaurant_work_status.background = view.context.getDrawable(R.drawable.background_time_green)
                            }
                            "orange" -> {
                                view.restaurant_work_status.background = view.context.getDrawable(R.drawable.background_time_orange)
                            }
                            "red" -> {
                                view.restaurant_work_status.background = view.context.getDrawable(R.drawable.background_time_red)
                            }
                        }
                    }
                }
            }, 0, 10000)

        } else { view.restaurant_view.visibility = View.GONE }

        return view
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {

        mMap = googleMap

        if(mUtilFunctions.checkLocationPermissions()){ mMap.isMyLocationEnabled = true }

        val mArgs = arguments
        val restaurant = mArgs?.getString("resto")

        if(!restaurant.isNullOrBlank() && !restaurant.isNullOrEmpty()){
            val sharp = SVG.getFromString(branch.restaurant?.pinImg)
            val mapPin = mUtilFunctions.vectorToBitmap(sharp)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(branch.latitude, branch.longitude), GOOGLE_MAPS_ZOOM))
            mMap.addMarker(MarkerOptions().position(LatLng(branch.latitude, branch.longitude)).title(branch.address).icon(mapPin))
        } else {
            if(mUtilFunctions.checkLocationPermissions()){
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener {
                        mMap.clear()
                        this.getLocation(it)
                    }.addOnFailureListener {
                        activity!!.runOnUiThread {
                            TingToast(context!!, it.message!!, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                        }
                    }
                } catch (e: java.lang.Exception){
                    TingToast(context!!, activity!!.resources.getString(R.string.error_internet), TingToastType.ERROR).showToast(
                        Toast.LENGTH_LONG)
                }
            }
        }

        mMap.setOnMyLocationButtonClickListener {
            if(mUtilFunctions.checkLocationPermissions()){
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener {
                        this.getLocation(it)
                    }.addOnFailureListener {
                        activity!!.runOnUiThread {
                            TingToast(context!!, it.message!!, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
                        }
                    }
                } catch (e: Exception){
                    TingToast(context!!, activity!!.resources.getString(R.string.error_internet), TingToastType.ERROR).showToast(
                        Toast.LENGTH_LONG)
                }
            }
            true
        }
    }

    private fun getLocation(location: Location){
        try {
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), GOOGLE_MAPS_ZOOM))
        } catch (e: Exception){
            TingToast(context!!, activity!!.resources.getString(R.string.error_internet), TingToastType.ERROR).showToast(
                Toast.LENGTH_LONG)
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun showRestaurantMap(view: View, cx: Int, cy: Int) {

        val w = view.width
        val h = view.height

        val endRadius = Math.hypot(w.toDouble(), h.toDouble()).toInt()

        val revealAnimator = ViewAnimationUtils.createCircularReveal(view, cx, cy.toInt(), 0f, endRadius.toFloat())

        view.visibility = View.VISIBLE
        revealAnimator.duration = 700
        revealAnimator.start()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun hideRestaurantMap(view: View, cx: Int, cy: Int, dialog: Dialog){
        val w = view.width
        val h = view.height

        val endRadius = Math.hypot(w.toDouble(), h.toDouble()).toInt()

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
        val touchOutsideView = dialog.window.findViewById<View>(android.support.design.R.id.touch_outside)
        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer.cancel()
        val f = fragmentManager!!.findFragmentById(R.id.map)
        fragmentManager!!.beginTransaction().remove(f!!).commit()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        timer.cancel()
        handler?.removeCallbacks(runnable)
    }

    override fun onStart() {
        super.onStart()
        dialog.window.setLayout(WindowManager.LayoutParams.FILL_PARENT, WindowManager.LayoutParams.FILL_PARENT)
    }
}
