package com.codepipes.ting.adapters.menu

import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codepipes.ting.R
import com.codepipes.ting.customclasses.RoundedCornerImageView
import com.codepipes.ting.fragments.menu.RestaurantMenuBottomSheetFragment
import com.codepipes.ting.models.MenuImage
import com.codepipes.ting.models.RestaurantMenu
import com.codepipes.ting.utils.Routes
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.row_menu_restaurant_list.view.*

class MenuImageListAdapter(private val images: MutableList<MenuImage>) : RecyclerView.Adapter<MenuImageListViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): MenuImageListViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val row = layoutInflater.inflate(R.layout.row_menu_restaurant_list, parent, false)
        return MenuImageListViewHolder(row)
    }

    override fun getItemCount(): Int = if (images.size >= 4) { 4 } else { images.size }

    override fun onBindViewHolder(holder: MenuImageListViewHolder, position: Int) {
        val image = images[position]
        holder.image = image

        val imageView = holder.view.findViewById<RoundedCornerImageView>(R.id.restaurant_menu_image) as RoundedCornerImageView
        imageView.radius = 10.0F
        Picasso.get().load("${Routes().HOST_END_POINT}${image.image}").into(imageView)
    }
}

class MenuImageListViewHolder(val view: View, var image: MenuImage? = null) : RecyclerView.ViewHolder(view){}