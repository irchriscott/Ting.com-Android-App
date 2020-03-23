package com.codepipes.ting.adapters.restaurant.specifications

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codepipes.ting.R
import com.codepipes.ting.models.BranchSpecial
import kotlinx.android.synthetic.main.row_restaurant_specification.view.*

class RestaurantSpecialsAdapter (private val specials: List<BranchSpecial>) : RecyclerView.Adapter<RestaurantSpecialsViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, type: Int): RestaurantSpecialsViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val row = layoutInflater.inflate(R.layout.row_restaurant_specification, parent, false)
        return RestaurantSpecialsViewHolder(row)
    }

    override fun getItemCount(): Int  = specials.size

    override fun onBindViewHolder(holder: RestaurantSpecialsViewHolder, position: Int) {
        holder.view.specification_title.text = specials[position].name
    }
}

class RestaurantSpecialsViewHolder(val view: View) : RecyclerView.ViewHolder(view) {}