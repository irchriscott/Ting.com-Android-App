package com.codepipes.ting.adapters.menu

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codepipes.ting.R
import com.codepipes.ting.fragments.menu.MenuBottomSheetFragment
import com.codepipes.ting.models.DishFood
import com.codepipes.ting.utils.Routes
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.row_menu_dish_food.view.*


class MenuFoodsAdapter (private val menus: MutableList<DishFood>, private val fragmentManager: FragmentManager) : RecyclerView.Adapter<MenuFoodsViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): MenuFoodsViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val row = layoutInflater.inflate(R.layout.row_menu_dish_food, parent, false)
        return MenuFoodsViewHolder(row)
    }

    override fun getItemCount(): Int = menus.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MenuFoodsViewHolder, position: Int) {
        val menu = menus[position]

        val index = (0 until menu.food.images.count - 1).random()
        val image = menu.food.images.images[index]
        Picasso.get().load("${Routes().HOST_END_POINT}${image.image}").into(holder.view.menu_image)

        holder.view.menu_name.text = menu.food.name
        holder.view.menu_rating.rating = menu.food.reviews?.average!!.toFloat()
        holder.view.menu_description.text = menu.food.description

        if(menu.isCountable){
            holder.view.menu_quantity.text = "Quantity : ${menu.quantity} pieces / packs"
        } else { holder.view.menu_quantity.visibility = View.GONE }

        if(menu.food.foodType != null){
            holder.view.menu_type_image.setImageDrawable(holder.view.context.resources.getDrawable(R.drawable.ic_spoon_gray))
            holder.view.menu_type_name.text = "Food"
            holder.view.menu_category_name.text = menu.food.foodType
        } else if(menu.food.drinkType != null) {
            holder.view.menu_type_image.setImageDrawable(holder.view.context.resources.getDrawable(R.drawable.ic_glass_gray))
            holder.view.menu_type_name.text = "Drink"
            holder.view.menu_category_name.text = menu.food.drinkType
        }

        if(menu.food.isAvailable){
            holder.view.menu_availability_text.text = "Available"
            holder.view.menu_availability_view.background = holder.view.context.resources.getDrawable(R.drawable.background_time_green)
            holder.view.menu_availability_icon.setImageDrawable(holder.view.context.resources.getDrawable(R.drawable.ic_check_white_48dp))
        } else {
            holder.view.menu_availability_text.text = "Not Available"
            holder.view.menu_availability_view.background = holder.view.context.resources.getDrawable(R.drawable.background_time_red)
            holder.view.menu_availability_icon.setImageDrawable(holder.view.context.resources.getDrawable(R.drawable.ic_close_white_24dp))
        }

        holder.view.setOnClickListener {
            val menuFragment = MenuBottomSheetFragment()
            val bundle =  Bundle()
            bundle.putString("menu", Gson().toJson(menu.food))
            menuFragment.arguments = bundle
            menuFragment.show(fragmentManager, menuFragment.tag)
        }
    }
}


class MenuFoodsViewHolder(val view: View, val menu: DishFood? = null) : RecyclerView.ViewHolder(view){}