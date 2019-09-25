package com.codepipes.ting.adapters.restaurant

import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codepipes.ting.R
import com.codepipes.ting.customclasses.RoundedCornerImageView
import com.codepipes.ting.fragments.menu.RestaurantMenuBottomSheetFragment
import com.codepipes.ting.models.RestaurantMenu
import com.codepipes.ting.utils.Routes
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.row_menu_restaurant_list.view.*

class RestaurantListMenuAdapter(private val menus: MutableList<RestaurantMenu>, private val fragmentManager: FragmentManager) : RecyclerView.Adapter<RestaurantListMenuAdapterViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): RestaurantListMenuAdapterViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val row = layoutInflater.inflate(R.layout.row_menu_restaurant_list, parent, false)
        return RestaurantListMenuAdapterViewHolder(row)
    }

    override fun getItemCount(): Int = if (menus.size >= 4) { 4 } else { menus.size }

    override fun onBindViewHolder(holder: RestaurantListMenuAdapterViewHolder, position: Int) {
        val menu = menus[position]
        holder.menu = menu
        val index = (0 until menu.menu.images.count - 1).random()
        val image = menu.menu.images.images[index]

        val imageView = holder.view.findViewById<RoundedCornerImageView>(R.id.restaurant_menu_image) as RoundedCornerImageView
        imageView.radius = 10.0F

        Picasso.get().load("${Routes().HOST_END_POINT}${image.image}").into(imageView)
        imageView.setOnClickListener {
            val restaurantMenuFragment = RestaurantMenuBottomSheetFragment()
            val bundle =  Bundle()
            bundle.putString("menu", Gson().toJson(menu))
            restaurantMenuFragment.arguments = bundle
            restaurantMenuFragment.show(fragmentManager, restaurantMenuFragment.tag)
        }
    }
}

class RestaurantListMenuAdapterViewHolder(val view: View, var menu: RestaurantMenu? = null) : RecyclerView.ViewHolder(view){}