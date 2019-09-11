package com.codepipes.ting.customclasses

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.View
import com.codepipes.ting.R
import com.codepipes.ting.models.Branch
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.layout_map_info_window.view.*

class RestaurantInfoWindowMap (val context: Context) : GoogleMap.InfoWindowAdapter {

    override fun getInfoWindow(marker: Marker?): View? = null

    @SuppressLint("InflateParams", "SetTextI18n")
    override fun getInfoContents(marker: Marker?): View {
        val activity = context as Activity
        val view = activity.layoutInflater.inflate(R.layout.layout_map_info_window, null, false)
        val data = marker?.title
        val branch = Gson().fromJson(data,Branch::class.java)

        Picasso.get().load(branch.restaurant?.logoURL()).into(view.info_image)
        view.info_name.text = branch.restaurant?.name
        view.info_branch.text = branch.name + " Branch"
        view.info_mail.text = branch.email
        view.info_phone.text = branch.phone
        view.info_time.text = "${branch.restaurant?.opening} - ${branch.restaurant?.closing}"

        return view
    }

}