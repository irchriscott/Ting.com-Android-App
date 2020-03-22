package com.codepipes.ting.adapters.restaurant.specifications

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codepipes.ting.R
import com.codepipes.ting.models.FoodCategory
import com.codepipes.ting.utils.Routes
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.row_restaurant_specification.view.*

class RestaurantFoodCategoryAdapter (private val categories: List<FoodCategory>) : RecyclerView.Adapter<RestaurantFoodCategoryViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, type: Int): RestaurantFoodCategoryViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val row = layoutInflater.inflate(R.layout.row_restaurant_specification, parent, false)
        return RestaurantFoodCategoryViewHolder(row)
    }

    override fun getItemCount(): Int  = categories.size

    override fun onBindViewHolder(holder: RestaurantFoodCategoryViewHolder, position: Int) {
        Picasso.get().load("${Routes.HOST_END_POINT}${categories[position].image}").into(holder.view.specification_image)
        holder.view.specification_title.text = categories[position].name
    }
}

class RestaurantFoodCategoryViewHolder(val view: View) : RecyclerView.ViewHolder(view) {}