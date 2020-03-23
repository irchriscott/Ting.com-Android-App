package com.codepipes.ting.adapters.menu

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Paint
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codepipes.ting.R
import com.codepipes.ting.models.RestaurantMenu
import com.codepipes.ting.utils.Routes
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.row_restaurant_menu.view.*
import java.text.NumberFormat


class RestaurantMenuAdapter (private val menus: MutableList<RestaurantMenu>, val fragmentManager: FragmentManager) : RecyclerView.Adapter<RestaurantMenuViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): RestaurantMenuViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val row = layoutInflater.inflate(R.layout.row_restaurant_menu, parent, false)
        return RestaurantMenuViewHolder(row)
    }

    override fun getItemCount(): Int = menus.size

    @SuppressLint("SetTextI18n", "DefaultLocale")
    override fun onBindViewHolder(holder: RestaurantMenuViewHolder, position: Int) {
        val menu = menus[position]
        val activity = holder.view.context as Activity

        holder.menu = menu

        val index = (0 until menu.menu.images.count - 1).random()
        val image = menu.menu.images.images[index]
        Picasso.get().load("${Routes.HOST_END_POINT}${image.image}").into(holder.view.menu_image)

        holder.view.menu_name.text = menu.menu.name
        holder.view.menu_rating.rating = menu.menu.reviews?.average!!.toFloat()
        holder.view.menu_description.text = menu.menu.description

        holder.view.menu_type_name.text = menu.type.name.capitalize()
        holder.view.menu_new_price.text =
            "${menu.menu.currency} ${NumberFormat.getNumberInstance().format(menu.menu.price)}".toUpperCase()
        if (menu.menu.price != menu.menu.lastPrice) {
            holder.view.menu_old_price.text =
                "${menu.menu.currency} ${NumberFormat.getNumberInstance().format(menu.menu.lastPrice)}".toUpperCase()
            holder.view.menu_old_price.paintFlags = holder.view.menu_old_price.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else { holder.view.menu_old_price.visibility = View.GONE }


        when (menu.type.id) {
            1 -> {
                holder.view.menu_type_name.text = menu.menu.foodType
                holder.view.menu_category_name.text = menu.menu.category?.name
                Picasso.get().load("${Routes.HOST_END_POINT}${menu.menu.category?.image}").into(holder.view.menu_category_image)
                holder.view.menu_cuisine_name.text = menu.menu.cuisine?.name
                Picasso.get().load("${Routes.HOST_END_POINT}${menu.menu.cuisine?.image}").into(holder.view.menu_cuisine_image)
                if (menu.menu.isCountable) {
                    holder.view.menu_quantity.text = "${menu.menu.quantity} pieces / packs"
                } else { holder.view.menu_quantity.visibility = View.GONE }
            }
            2 -> {
                holder.view.menu_type_name.text = menu.menu.drinkType
                holder.view.menu_category_view.visibility = View.GONE
                holder.view.menu_cuisine_view.visibility = View.GONE
                if (menu.menu.isCountable) {
                    holder.view.menu_quantity.text = "${menu.menu.quantity} cups / bottles"
                } else { holder.view.menu_quantity.visibility = View.GONE }
            }
            3 -> {
                holder.view.menu_type_image.setImageDrawable(activity.resources.getDrawable(R.drawable.ic_clock_gray_24dp))
                holder.view.menu_category_name.text = menu.menu.category?.name
                Picasso.get().load("${Routes.HOST_END_POINT}${menu.menu.category?.image}").into(holder.view.menu_category_image)
                holder.view.menu_type_name.text = menu.menu.dishTime
                holder.view.menu_cuisine_name.text = menu.menu.cuisine?.name
                Picasso.get().load("${Routes.HOST_END_POINT}${menu.menu.cuisine?.image}").into(holder.view.menu_cuisine_image)
                if (menu.menu.isCountable) {
                    holder.view.menu_quantity.text = "${menu.menu.quantity} plates / packs"
                } else { holder.view.menu_quantity.visibility = View.GONE }
            }
        }

        holder.view.menu_likes.text = NumberFormat.getNumberInstance().format(menu.menu.likes?.count)
        holder.view.menu_reviews.text = NumberFormat.getNumberInstance().format(menu.menu.reviews.count)

        if(menu.menu.isAvailable){
            holder.view.menu_availability_text.text = "Available"
            holder.view.menu_availability_view.background = holder.view.context.resources.getDrawable(
                R.drawable.background_time_green)
            holder.view.menu_availability_icon.setImageDrawable(holder.view.context.resources.getDrawable(
                R.drawable.ic_check_white_48dp))
        } else {
            holder.view.menu_availability_text.text = "Not Available"
            holder.view.menu_availability_view.background = holder.view.context.resources.getDrawable(
                R.drawable.background_time_red)
            holder.view.menu_availability_icon.setImageDrawable(holder.view.context.resources.getDrawable(
                R.drawable.ic_close_white_24dp))
        }

        holder.view.setOnClickListener {
            val intent = Intent(activity, com.codepipes.ting.activities.menu.RestaurantMenu::class.java)
            intent.putExtra("menu", menu.id)
            intent.putExtra("url", menu.urls.apiGet)
            holder.view.context.startActivity(intent)
        }
    }

    public fun addItems(menusOthers : MutableList<RestaurantMenu>) {
        val lastPosition = menus.size
        menus.addAll(menusOthers)
        notifyItemRangeInserted(lastPosition, menusOthers.size)
    }
}


class RestaurantMenuViewHolder(val view: View, var menu: RestaurantMenu? = null) : RecyclerView.ViewHolder(view){}