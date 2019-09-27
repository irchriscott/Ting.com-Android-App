package com.codepipes.ting.fragments.menu

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.design.widget.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codepipes.ting.R
import com.codepipes.ting.models.RestaurantMenu
import com.codepipes.ting.utils.Routes
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_restaurant_menu_bottom_sheet.view.*
import java.text.NumberFormat

class RestaurantMenuBottomSheetFragment : BottomSheetDialogFragment() {

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
        val menu = Gson().fromJson(myArgs?.get("menu").toString(), RestaurantMenu::class.java)

        view.menu_name.text = menu.menu.name
        view.menu_rating.rating = menu.menu.reviews?.average!!.toFloat()
        view.menu_description.text = menu.menu.description

        val index = (0 until menu.menu.images.count - 1).random()
        val image = menu.menu.images.images[index]
        Picasso.get().load("${Routes().HOST_END_POINT}${image.image}").into(view.menu_image)

        if(menu.type.id != 2){
            view.menu_subcategory_name.text = menu.menu.category?.name
            Picasso.get().load("${Routes().HOST_END_POINT}${menu.menu.category?.image}").into(view.menu_subcategory_image)
        } else { view.menu_subcategory.visibility = View.GONE }

        view.menu_type_name.text = menu.type.name.capitalize()
        view.menu_new_price.text = "${menu.menu.currency} ${NumberFormat.getNumberInstance().format(menu.menu.price)}".toUpperCase()
        if(menu.menu.price != menu.menu.lastPrice){
            view.menu_old_price.text = "${menu.menu.currency} ${NumberFormat.getNumberInstance().format(menu.menu.lastPrice)}".toUpperCase()
            view.menu_old_price.paintFlags = view.menu_old_price.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else { view.menu_old_price.visibility = View.GONE }


        when(menu.type.id){
            1 -> {
                view.menu_type_image.setImageDrawable(view.context.resources.getDrawable(R.drawable.ic_spoon_gray))
                view.menu_category_name.text = menu.menu.foodType
                if(menu.menu.isCountable){ view.menu_quantity.text = "${menu.menu.quantity} pieces / packs" }
                else { view.menu_quantity.visibility = View.GONE }
            }
            2 -> {
                view.menu_type_image.setImageDrawable(view.context.resources.getDrawable(R.drawable.ic_glass_gray))
                view.menu_category_name.text = menu.menu.drinkType
                if(menu.menu.isCountable){ view.menu_quantity.text = "${menu.menu.quantity} cups / bottles" }
                else { view.menu_quantity.visibility = View.GONE }
            }
            3 -> {
                view.menu_type_image.setImageDrawable(view.context.resources.getDrawable(R.drawable.ic_fork_knife_gray))
                view.menu_category_image.setImageDrawable(view.context.resources.getDrawable(R.drawable.ic_clock_gray_24dp))
                view.menu_category_name.text = menu.menu.dishTime
                if(menu.menu.isCountable){ view.menu_quantity.text = "${menu.menu.quantity} plates / packs" }
                else { view.menu_quantity.visibility = View.GONE }
            }
        }

        view.menu_likes.text = NumberFormat.getNumberInstance().format(menu.menu.likes?.count)
        view.menu_reviews.text = NumberFormat.getNumberInstance().format(menu.menu.reviews.count)


        if(menu.menu.isAvailable){
            view.menu_availability_text.text = "Available"
            view.menu_availability_view.background = view.context.resources.getDrawable(R.drawable.background_time_green)
            view.menu_availability_icon.setImageDrawable(view.context.resources.getDrawable(R.drawable.ic_check_white_48dp))
        } else {
            view.menu_availability_text.text = "Not Available"
            view.menu_availability_view.background = view.context.resources.getDrawable(R.drawable.background_time_red)
            view.menu_availability_icon.setImageDrawable(view.context.resources.getDrawable(R.drawable.ic_close_white_24dp))
        }

        view.menu_image.setOnClickListener {
            val intent = Intent(activity!!, com.codepipes.ting.RestaurantMenu::class.java)
            intent.putExtra("menu", Gson().toJson(menu))
            activity!!.startActivity(intent)
        }

        return view
    }
}