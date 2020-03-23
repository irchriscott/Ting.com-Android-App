package com.codepipes.ting.adapters.menu

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.codepipes.ting.R
import com.codepipes.ting.fragments.menu.RestaurantMenuBottomSheetFragment
import com.codepipes.ting.models.RestaurantMenu
import com.codepipes.ting.utils.Routes
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.row_menu_dish_food.view.*


class PromotionMenusListAdapter (private val menus: MutableList<RestaurantMenu>, val fragmentManager: FragmentManager): RecyclerView.Adapter<PromotionMenusListViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): PromotionMenusListViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val row = layoutInflater.inflate(R.layout.row_menu_dish_food, parent, false)
        return PromotionMenusListViewHolder(row)
    }

    override fun getItemCount(): Int = if(menus.size >= 10){ 10 } else { menus.size }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: PromotionMenusListViewHolder, position: Int) {
        val menu = menus[position]

        val index = (0 until menu.menu.images.count - 1).random()
        val image = menu.menu.images.images[index]
        Picasso.get().load("${Routes.HOST_END_POINT}${image.image}").into(holder.view.menu_image)

        holder.view.menu_name.text = menu.menu.name
        holder.view.menu_rating.rating = menu.menu.reviews?.average!!.toFloat()
        holder.view.menu_description.text = menu.menu.description

        holder.view.menu_quantity.visibility = View.GONE

        when {
            menu.menu.foodType != null -> {
                holder.view.menu_type_image.setImageDrawable(holder.view.context.resources.getDrawable(R.drawable.ic_spoon_gray))
                holder.view.menu_type_name.text = "Food"
                holder.view.menu_category_name.text = menu.menu.foodType
            }
            menu.menu.drinkType != null -> {
                holder.view.menu_type_image.setImageDrawable(holder.view.context.resources.getDrawable(R.drawable.ic_glass_gray))
                holder.view.menu_type_name.text = "Drink"
                holder.view.menu_category_name.text = menu.menu.drinkType
            }
            menu.menu.dishTime != null -> {
                holder.view.menu_type_image.setImageDrawable(holder.view.context.resources.getDrawable(R.drawable.ic_restaurants))
                holder.view.menu_type_name.text = "Dish"
                holder.view.menu_category_name.text = menu.menu.dishTime
            }
        }

        if(menu.menu.isAvailable){
            holder.view.menu_availability_text.text = "Available"
            holder.view.menu_availability_view.background = holder.view.context.resources.getDrawable(R.drawable.background_time_green)
            holder.view.menu_availability_icon.setImageDrawable(holder.view.context.resources.getDrawable(R.drawable.ic_check_white_48dp))
        } else {
            holder.view.menu_availability_text.text = "Not Available"
            holder.view.menu_availability_view.background = holder.view.context.resources.getDrawable(R.drawable.background_time_red)
            holder.view.menu_availability_icon.setImageDrawable(holder.view.context.resources.getDrawable(R.drawable.ic_close_white_24dp))
        }

        holder.view.setOnClickListener {
            val menuFragment = RestaurantMenuBottomSheetFragment()
            val bundle =  Bundle()
            bundle.putString("menu", Gson().toJson(menu))
            menuFragment.arguments = bundle
            menuFragment.show(fragmentManager, menuFragment.tag)
        }
    }
}

class PromotionMenusListViewHolder(val view: View, var menu: RestaurantMenu? = null) : RecyclerView.ViewHolder(view){}