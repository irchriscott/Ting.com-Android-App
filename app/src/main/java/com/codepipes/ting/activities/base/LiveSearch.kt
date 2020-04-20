package com.codepipes.ting.activities.base

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.location.Geocoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import com.codepipes.ting.R
import com.codepipes.ting.adapters.others.SearchResultAdapter
import com.codepipes.ting.dialogs.messages.TingToast
import com.codepipes.ting.dialogs.messages.TingToastType
import com.codepipes.ting.models.SearchResult
import com.codepipes.ting.models.User
import com.codepipes.ting.providers.TingClient
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.UtilsFunctions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jakewharton.rxbinding2.widget.RxTextView
import com.jakewharton.rxbinding2.widget.TextViewTextChangeEvent
import com.livefront.bridge.Bridge
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import io.reactivex.functions.Function
import kotlinx.android.synthetic.main.activity_live_search.*
import kotlinx.android.synthetic.main.include_empty_data.view.*
import okhttp3.Interceptor
import java.util.*
import java.util.concurrent.TimeUnit

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
@SuppressLint("SetTextI18n")
class LiveSearch : AppCompatActivity() {

    private lateinit var utilsFunctions: UtilsFunctions

    private lateinit var userAuthentication: UserAuthentication
    private lateinit var session: User

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var country: String = ""
    private var town: String = ""

    private val disposable = CompositeDisposable()
    private val publishSubject = PublishSubject.create<String>()

    @SuppressLint("PrivateResource")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_search)

        Bridge.restoreInstanceState(this, savedInstanceState)
        savedInstanceState?.clear()

        setSupportActionBar(toolbar)
        supportActionBar?.setDefaultDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        try {
            val upArrow = ContextCompat.getDrawable(this@LiveSearch,
                R.drawable.abc_ic_ab_back_material
            )
            upArrow!!.setColorFilter(
                ContextCompat.getColor(this@LiveSearch,
                    R.color.colorPrimary
                ), PorterDuff.Mode.SRC_ATOP)
            supportActionBar!!.setHomeAsUpIndicator(upArrow)
        } catch (e: java.lang.Exception) {}

        input_search.requestFocus()

        utilsFunctions = UtilsFunctions(this@LiveSearch)

        userAuthentication = UserAuthentication(this@LiveSearch)
        session = userAuthentication.get()!!

        country = session.country
        town = session.town

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this@LiveSearch)

        if(utilsFunctions.checkLocationPermissions()) { getCurrentLocation() }
        else {
            ActivityCompat.requestPermissions(
                this@LiveSearch,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_FINE_LOCATION
            )
        }

        val searchResultObserver = object : DisposableObserver<List<SearchResult>>() {

            override fun onComplete() {}

            override fun onNext(results: List<SearchResult>) {

                shimmer_loader.visibility = View.GONE

                if (results.isNotEmpty()) {

                    empty_data.visibility = View.GONE
                    search_result_recycler_view.visibility = View.VISIBLE
                    search_result_recycler_view.layoutManager = LinearLayoutManager(this@LiveSearch)
                    search_result_recycler_view.adapter = SearchResultAdapter(results.toSet().toMutableList())

                } else {

                    empty_data.visibility = View.VISIBLE
                    search_result_recycler_view.visibility = View.GONE
                    empty_data.empty_image.setImageResource(R.drawable.ic_search)
                    empty_data.empty_text.text = "No Result To Show"
                }
            }

            override fun onError(error: Throwable) {

                shimmer_loader.visibility = View.GONE
                search_result_recycler_view.visibility = View.GONE
                empty_data.visibility = View.VISIBLE

                empty_data.visibility = View.VISIBLE
                search_result_recycler_view.visibility = View.GONE
                empty_data.empty_image.setImageResource(R.drawable.ic_search)
                empty_data.empty_text.text = "No Result To Show"

                TingToast(this@LiveSearch, error.message!!, TingToastType.ERROR).showToast(Toast.LENGTH_LONG)
            }
        }

        val searchMenusWatcher = object : DisposableObserver<TextViewTextChangeEvent>() {
            override fun onComplete() {}
            override fun onNext(event: TextViewTextChangeEvent) {
                publishSubject.onNext(event.text().toString())
            }
            override fun onError(error: Throwable) {}
        }

        disposable.add(publishSubject.debounce(300, TimeUnit.MILLISECONDS)
            .distinctUntilChanged()
            .switchMap(Function<String, Observable<List<SearchResult>>> {
                return@Function TingClient.getInstance(this@LiveSearch)
                    .getLiveSearchResults(it, country, town)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())

            }).subscribeWith(searchResultObserver))

        disposable.add(
            RxTextView.textChangeEvents(input_search)
                .skipInitialValue()
                .debounce(300, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(searchMenusWatcher))

        disposable.add(searchResultObserver)
    }

    private fun getCurrentLocation() {
        try {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener {
                if(it != null) {
                    try {
                        val geocoder = Geocoder(this@LiveSearch, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                        country = addresses[0].countryName
                        town = addresses[0].locality
                    } catch (e: Exception) { }
                } else {
                    runOnUiThread {
                        TingToast(
                            this@LiveSearch,
                            "Please, Try Again",
                            TingToastType.ERROR
                        ).showToast(Toast.LENGTH_LONG)
                    }
                }
            }.addOnFailureListener {
                runOnUiThread {
                    TingToast(
                        this@LiveSearch,
                        it.message!!,
                        TingToastType.ERROR
                    ).showToast(Toast.LENGTH_LONG)
                }
            }
        } catch (e: Exception){
            TingToast(
                this@LiveSearch,
                e.message!!,
                TingToastType.ERROR
            ).showToast(Toast.LENGTH_LONG)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            REQUEST_FINE_LOCATION -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getCurrentLocation()
                }
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

    override fun onResume() {
        super.onResume()
        input_search.requestFocus()
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
        disposable.clear()
        Bridge.clear(this)
    }

    companion object {
        private const val REQUEST_FINE_LOCATION = 2
    }
}
