package com.codepipes.ting.adapters.restaurant.specifications

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codepipes.ting.R
import com.codepipes.ting.models.RestaurantCategory
import com.codepipes.ting.utils.Routes
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.row_restaurant_specification.view.*

class RestaurantCuisinesAdapter (private val cuisines: List<RestaurantCategory>) : RecyclerView.Adapter<RestaurantCuisinesViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, type: Int): RestaurantCuisinesViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val row = layoutInflater.inflate(R.layout.row_restaurant_specification, parent, false)
        return RestaurantCuisinesViewHolder(row)
    }

    override fun getItemCount(): Int  = cuisines.size

    override fun onBindViewHolder(holder: RestaurantCuisinesViewHolder, position: Int) {
        Picasso.get().load("${Routes().HOST_END_POINT}${cuisines[position].image}").into(holder.view.specification_image)
        holder.view.specification_title.text = cuisines[position].name
    }
}

class RestaurantCuisinesViewHolder(val view: View) : RecyclerView.ViewHolder(view) {}