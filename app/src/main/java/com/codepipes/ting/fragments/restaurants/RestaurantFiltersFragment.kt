package com.codepipes.ting.fragments.restaurants


import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.codepipes.ting.R
import com.codepipes.ting.fragments.navigation.RestaurantsFragment
import com.codepipes.ting.interfaces.FilterRestaurantsClickListener
import com.codepipes.ting.models.Filter
import com.codepipes.ting.models.RestaurantFilters
import com.codepipes.ting.providers.LocalData
import com.codepipes.ting.utils.Routes
import com.google.gson.Gson
import com.livefront.bridge.Bridge
import kotlinx.android.synthetic.main.fragment_restaurant_filters.view.*
import kotlinx.android.synthetic.main.row_filter_checkbox.view.*
import okhttp3.*
import java.io.IOException
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


class RestaurantFiltersFragment : BottomSheetDialogFragment() {

    private lateinit var mLocalData: LocalData
    private var filterType = RestaurantsFragment.AVAILABILITY_KEY
    private lateinit var filters: List<Filter>
    private lateinit var routes: Routes

    private lateinit var selectedFilters: MutableList<Int>
    private lateinit var mFilterRestaurantsClickListener: FilterRestaurantsClickListener

    override fun getTheme(): Int = R.style.BaseBottomSheetDialogElse

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Bridge.restoreInstanceState(this, savedInstanceState)
        savedInstanceState?.clear()
    }

    @SuppressLint("DefaultLocale")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_restaurant_filters, container, false)

        routes = Routes()
        mLocalData = LocalData(context!!)
        val type = arguments!!.getString(RestaurantsFragment.FILTER_KEY)
        if(type != null) { filterType = type }

        view.filter_title.text = when (filterType) {
            RestaurantsFragment.AVAILABILITY_KEY -> { "Filter By Availability".toUpperCase() }
            RestaurantsFragment.CUISINES_KEY -> { "Filter By Cuisines".toUpperCase() }
            RestaurantsFragment.SERVICES_KEY -> { "Filter By Services".toUpperCase() }
            RestaurantsFragment.SPECIALS_KEY -> { "Filter By Specials".toUpperCase() }
            RestaurantsFragment.TYPES_KEY -> { "Filter By Types".toUpperCase() }
            RestaurantsFragment.RATINGS_KEY -> { "Filter By Ratings".toUpperCase() }
            else -> { "Filter By Distance" }
        }

        val selectedAllFilters = mLocalData.getParametersFilters()
        selectedFilters = when (filterType) {
            RestaurantsFragment.AVAILABILITY_KEY -> { selectedAllFilters.availability.toMutableList() }
            RestaurantsFragment.CUISINES_KEY -> { selectedAllFilters.cuisines.toMutableList() }
            RestaurantsFragment.SERVICES_KEY -> { selectedAllFilters.services.toMutableList() }
            RestaurantsFragment.SPECIALS_KEY -> { selectedAllFilters.specials.toMutableList() }
            RestaurantsFragment.TYPES_KEY -> { selectedAllFilters.types.toMutableList() }
            RestaurantsFragment.RATINGS_KEY -> { selectedAllFilters.ratings.toMutableList() }
            else -> { ArrayList() }
        }

        val allFilters = mLocalData.getFilters()
        if(allFilters != null) { assignFilters(allFilters, view) } else { getFilters(view) }

        view.filter_restaurant_button.setOnClickListener {
            mFilterRestaurantsClickListener.onFilterRestaurantsClickListener(filterType, selectedFilters)
        }

        return view
    }

    private fun assignFilters(allFilters: RestaurantFilters, view: View) {
        filters = when(filterType) {
            RestaurantsFragment.AVAILABILITY_KEY -> { allFilters.availability }
            RestaurantsFragment.CUISINES_KEY -> { allFilters.cuisines }
            RestaurantsFragment.SERVICES_KEY -> { allFilters.services }
            RestaurantsFragment.SPECIALS_KEY -> { allFilters.specials }
            RestaurantsFragment.TYPES_KEY -> { allFilters.types }
            RestaurantsFragment.RATINGS_KEY -> { allFilters.ratings}
            else -> { ArrayList() }
        }
        loadFiltersToView(view)
    }

    private fun loadFiltersToView(view: View) {
        for (i in filters.indices) {
            val row = layoutInflater.inflate(R.layout.row_filter_checkbox, null)
            row.filter_name.text = filters[i].title
            row.filter_checkbox.tag = filters[i].id
            if(selectedFilters.contains(filters[i].id)) { row.filter_checkbox.isChecked = true }
            row.filter_checkbox.setOnCheckedChangeListener { _, isChecked ->
                if(isChecked) { selectedFilters.add(filters[i].id) }
                else { selectedFilters.remove(filters[i].id) }
            }
            view.filter_list.addView(row)
        }
    }

    private fun getFilters(view: View) {
        val url = routes.restaurantFilters
        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60 * 5, TimeUnit.SECONDS).build()

        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                activity!!.runOnUiThread {}
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body()!!.string()
                activity!!.runOnUiThread {
                    mLocalData.saveFilters(dataString)
                    assignFilters(Gson().fromJson(dataString, RestaurantFilters::class.java), view)
                }
            }
        })
    }

    fun onFilterRestaurantsListener(filterRestaurantsClickListener: FilterRestaurantsClickListener) {
        this.mFilterRestaurantsClickListener = filterRestaurantsClickListener
    }

    override fun onCancel(dialog: DialogInterface?) {
        super.onCancel(dialog)
        try {
            val paramsFilters = mLocalData.getParametersFilters()
            when(filterType) {
                RestaurantsFragment.AVAILABILITY_KEY -> { paramsFilters.availability = selectedFilters }
                RestaurantsFragment.CUISINES_KEY -> { paramsFilters.cuisines = selectedFilters }
                RestaurantsFragment.SPECIALS_KEY -> { paramsFilters.specials = selectedFilters }
                RestaurantsFragment.SERVICES_KEY -> { paramsFilters.services = selectedFilters }
                RestaurantsFragment.TYPES_KEY -> { paramsFilters.types = selectedFilters }
                RestaurantsFragment.RATINGS_KEY -> { paramsFilters.ratings = selectedFilters }
            }
            mLocalData.saveParametersFilters(Gson().toJson(paramsFilters))
        } catch (e: Exception) { }
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
}
