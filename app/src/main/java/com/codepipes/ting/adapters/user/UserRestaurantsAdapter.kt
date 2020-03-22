package com.codepipes.ting.adapters.user

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codepipes.ting.R
import com.codepipes.ting.activities.restaurant.RestaurantProfile
import com.codepipes.ting.models.UserRestaurant
import com.codepipes.ting.utils.UtilsFunctions
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.row_user_restaurant.view.*


class UserRestaurantsAdapter (val restaurants: MutableList<UserRestaurant>) : RecyclerView.Adapter<UserRestaurantsViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): UserRestaurantsViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val row = layoutInflater.inflate(R.layout.row_user_restaurant, parent, false)
        return UserRestaurantsViewHolder(row)
    }

    override fun getItemCount(): Int = restaurants.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: UserRestaurantsViewHolder, position: Int) {
        val resto = restaurants[position]
        Picasso.get().load(resto.branch.restaurant?.logoURL()).into(holder.view.like_image)

        val utilsFunctions = UtilsFunctions(holder.view.context)

        holder.view.like_name.text = "${resto.branch.restaurant?.name}, ${resto.branch.name}"
        holder.view.like_restaurant_rating.rating = resto.branch.reviews?.average!!
        holder.view.like_address.text = resto.branch.address
        holder.view.like_date.text = utilsFunctions.timeAgo(resto.createdAt)

        holder.view.setOnClickListener {
            val activity = holder.view.context as Activity
            val intent = Intent(holder.view.context, RestaurantProfile::class.java)
            intent.putExtra("resto", resto.branch.id)
            intent.putExtra("tab", 0)
            activity.startActivity(intent)
        }
    }
}


class UserRestaurantsViewHolder(val view: View, var restaurant: UserRestaurant? = null) : RecyclerView.ViewHolder(view){}