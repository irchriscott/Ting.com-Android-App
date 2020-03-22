package com.codepipes.ting.adapters.cuisine

import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codepipes.ting.activities.discovery.Cuisine
import com.codepipes.ting.R
import com.codepipes.ting.models.RestaurantCategory
import com.codepipes.ting.utils.Routes
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.row_cuisine.view.*

class CuisinesAdapter(private val cuisines: MutableList<RestaurantCategory>) : RecyclerView.Adapter<CuisinesAdapterViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): CuisinesAdapterViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val row = layoutInflater.inflate(R.layout.row_cuisine, parent, false)
        return CuisinesAdapterViewHolder(row)
    }

    override fun getItemCount(): Int = cuisines.size

    override fun onBindViewHolder(holder: CuisinesAdapterViewHolder, position: Int) {
        val cuisine = cuisines[position]
        Picasso.get().load("${Routes.HOST_END_POINT}${cuisine.image}").into(holder.view.cuisine_image)
        holder.view.cuisine_name.text = cuisine.name
        holder.cuisine = cuisine
        holder.view.setOnClickListener {
            val intent = Intent(holder.view.context, Cuisine::class.java)
            intent.putExtra("cuisine", Gson().toJson(cuisine))
            holder.view.context.startActivity(intent)
        }
    }
}

class CuisinesAdapterViewHolder(val view: View, var cuisine: RestaurantCategory? = null) : RecyclerView.ViewHolder(view) {}

