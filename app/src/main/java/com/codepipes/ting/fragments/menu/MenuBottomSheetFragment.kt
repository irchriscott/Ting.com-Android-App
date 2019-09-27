package com.codepipes.ting.fragments.menu

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codepipes.ting.R
import com.codepipes.ting.models.Menu
import com.codepipes.ting.models.RestaurantMenu
import com.codepipes.ting.utils.Routes
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_restaurant_menu_bottom_sheet.view.*
import java.text.NumberFormat

class MenuBottomSheetFragment : BottomSheetDialogFragment(){

    override fun getTheme(): Int = R.style.BaseBottomSheetDialogElse

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_restaurant_menu_bottom_sheet, container, false)
        val myArgs = this.arguments
        val menu = Gson().fromJson(myArgs?.get("menu").toString(), Menu::class.java)

        view.menu_name.text = menu.name
        view.menu_rating.rating = menu.reviews?.average!!.toFloat()
        view.menu_description.text = menu.description

        val index = (0 until menu.images.count - 1).random()
        val image = menu.images.images[index]
        Picasso.get().load("${Routes().HOST_END_POINT}${image.image}").into(view.menu_image)

        if(menu.foodType != null || menu.dishTime != null){
            view.menu_subcategory_name.text = menu.category?.name
            Picasso.get().load("${Routes().HOST_END_POINT}${menu.category?.image}").into(view.menu_subcategory_image)
        } else { view.menu_subcategory.visibility = View.GONE }

        view.menu_type_name.text = menu.name.capitalize()
        view.menu_new_price.text = "${menu.currency} ${NumberFormat.getNumberInstance().format(menu.price)}".toUpperCase()
        if(menu.price != menu.lastPrice){
            view.menu_old_price.text = "${menu.currency} ${NumberFormat.getNumberInstance().format(menu.lastPrice)}".toUpperCase()
            view.menu_old_price.paintFlags = view.menu_old_price.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else { view.menu_old_price.visibility = View.GONE }


        if(menu.foodType != null){
            view.menu_type_image.setImageDrawable(view.context.resources.getDrawable(R.drawable.ic_spoon_gray))
            view.menu_category_name.text = menu.foodType
            if(menu.isCountable){ view.menu_quantity.text = "${menu.quantity} pieces / packs" }
            else { view.menu_quantity.visibility = View.GONE }
        }

        if(menu.drinkType != null){
            view.menu_type_image.setImageDrawable(view.context.resources.getDrawable(R.drawable.ic_glass_gray))
            view.menu_category_name.text = menu.drinkType
            if(menu.isCountable){ view.menu_quantity.text = "${menu.quantity} cups / bottles" }
            else { view.menu_quantity.visibility = View.GONE }
        }

        if(menu.dishTime != null){
            view.menu_type_image.setImageDrawable(view.context.resources.getDrawable(R.drawable.ic_fork_knife_gray))
            view.menu_category_image.setImageDrawable(view.context.resources.getDrawable(R.drawable.ic_clock_gray_24dp))
            view.menu_category_name.text = menu.dishTime
            if(menu.isCountable){ view.menu_quantity.text = "${menu.quantity} plates / packs" }
            else { view.menu_quantity.visibility = View.GONE }
        }

        view.menu_likes.text = NumberFormat.getNumberInstance().format(menu.likes?.count)
        view.menu_reviews.text = NumberFormat.getNumberInstance().format(menu.reviews.count)


        if(menu.isAvailable){
            view.menu_availability_text.text = "Available"
            view.menu_availability_view.background = view.context.resources.getDrawable(R.drawable.background_time_green)
            view.menu_availability_icon.setImageDrawable(view.context.resources.getDrawable(R.drawable.ic_check_white_48dp))
        } else {
            view.menu_availability_text.text = "Not Available"
            view.menu_availability_view.background = view.context.resources.getDrawable(R.drawable.background_time_red)
            view.menu_availability_icon.setImageDrawable(view.context.resources.getDrawable(R.drawable.ic_close_white_24dp))
        }

        return view
    }
}