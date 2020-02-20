package com.codepipes.ting.adapters.promotion

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codepipes.ting.R
import com.codepipes.ting.adapters.menu.MenuImageListAdapter
import com.codepipes.ting.adapters.restaurant.RestaurantListMenuAdapter
import com.codepipes.ting.fragments.menu.RestaurantMenuBottomSheetFragment
import com.codepipes.ting.models.MenuImage
import com.codepipes.ting.models.MenuPromotion
import com.codepipes.ting.models.RestaurantMenu
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.UtilsFunctions
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.row_restaurant_promotion.view.*
import java.text.NumberFormat


class RestaurantPromotionAdapter (private val promotions: MutableList<MenuPromotion>, private val fragmentManager: FragmentManager) : RecyclerView.Adapter<RestaurantPromotionViewHolder>(){

    private lateinit var utilsFunctions: UtilsFunctions

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): RestaurantPromotionViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val row = layoutInflater.inflate(R.layout.row_restaurant_promotion, parent, false)
        return RestaurantPromotionViewHolder(row)
    }

    override fun getItemCount(): Int = promotions.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RestaurantPromotionViewHolder, position: Int) {
        val promotion = promotions[position]
        holder.promotion = promotion

        utilsFunctions = UtilsFunctions(holder.view.context)
        val activity = holder.view.context as Activity

        Picasso.get().load("${Routes().HOST_END_POINT}${promotion.posterImage}").into(holder.view.promotion_poster)
        holder.view.promotion_title.text = promotion.occasionEvent
        holder.view.promotion_menu_type_on_text.text = "Promotion On ${promotion.promotionItem.type.name}"
        holder.view.promotion_time.text = promotion.period

        if(promotion.isOn && promotion.isOnToday){
            holder.view.promotion_status.background = activity.resources.getDrawable(R.drawable.background_time_green)
            holder.view.promotion_status_icon.setImageDrawable(activity.resources.getDrawable(R.drawable.ic_check_white_48dp))
            holder.view.promotion_status_text.text = "Is On Today"
        } else {
            holder.view.promotion_status.background = activity.resources.getDrawable(R.drawable.background_time_red)
            holder.view.promotion_status_icon.setImageDrawable(activity.resources.getDrawable(R.drawable.ic_close_white_24dp))
            holder.view.promotion_status_text.text = "Is Off Today"
        }

        when(promotion.promotionItem.type.id){
            0 -> {
                if(promotion.menus.menus != null){
                    val menus = promotion.menus.menus
                    if (menus.isNotEmpty()){
                        val layoutManager = LinearLayoutManager(holder.view.context)
                        layoutManager.orientation = LinearLayoutManager.HORIZONTAL

                        holder.view.promotion_data_recycler_view.layoutManager = layoutManager
                        holder.view.promotion_data_recycler_view.adapter =
                            RestaurantListMenuAdapter(menus.shuffled() as MutableList<RestaurantMenu>, fragmentManager)
                    } else { holder.view.promotion_data_recycler_view.visibility = View.GONE }
                } else { holder.view.promotion_data_recycler_view.visibility = View.GONE }
                holder.view.promotion_menu_on.visibility = View.GONE
            }
            1 -> {
                if(promotion.menus.menus != null){
                    val menus = promotion.menus.menus
                    if (menus.isNotEmpty()){
                        val layoutManager = LinearLayoutManager(holder.view.context)
                        layoutManager.orientation = LinearLayoutManager.HORIZONTAL

                        holder.view.promotion_data_recycler_view.layoutManager = layoutManager
                        holder.view.promotion_data_recycler_view.adapter =
                            RestaurantListMenuAdapter(menus.shuffled() as MutableList<RestaurantMenu>, fragmentManager)
                    } else { holder.view.promotion_data_recycler_view.visibility = View.GONE }
                } else { holder.view.promotion_data_recycler_view.visibility = View.GONE }
                holder.view.promotion_menu_on.visibility = View.GONE
            }
            2 -> {
                if(promotion.menus.menus != null){
                    val menus = promotion.menus.menus
                    if (menus.isNotEmpty()){
                        val layoutManager = LinearLayoutManager(holder.view.context)
                        layoutManager.orientation = LinearLayoutManager.HORIZONTAL

                        holder.view.promotion_data_recycler_view.layoutManager = layoutManager
                        holder.view.promotion_data_recycler_view.adapter =
                            RestaurantListMenuAdapter(menus.shuffled() as MutableList<RestaurantMenu>, fragmentManager)
                    } else { holder.view.promotion_data_recycler_view.visibility = View.GONE }
                } else { holder.view.promotion_data_recycler_view.visibility = View.GONE }
                holder.view.promotion_menu_on.visibility = View.GONE
            }
            3 -> {
                if(promotion.menus.menus != null){
                    val menus = promotion.menus.menus
                    if (menus.isNotEmpty()){
                        val layoutManager = LinearLayoutManager(holder.view.context)
                        layoutManager.orientation = LinearLayoutManager.HORIZONTAL

                        holder.view.promotion_data_recycler_view.layoutManager = layoutManager
                        holder.view.promotion_data_recycler_view.adapter =
                            RestaurantListMenuAdapter(menus.shuffled() as MutableList<RestaurantMenu>, fragmentManager)
                    } else { holder.view.promotion_data_recycler_view.visibility = View.GONE }
                } else { holder.view.promotion_data_recycler_view.visibility = View.GONE }
                holder.view.promotion_menu_on.visibility = View.GONE
            }
            4 -> {
                val index = (0 until promotion.promotionItem.menu?.menu?.images?.count!! - 1).random()
                val image = promotion.promotionItem.menu.menu.images.images[index]
                Picasso.get().load("${Routes().HOST_END_POINT}${image.image}").into(holder.view.promotion_menu_on_image)
                holder.view.promotion_menu_on_text.text = "Promotion On ${promotion.promotionItem.menu.menu.name}"

                val layoutManager = LinearLayoutManager(holder.view.context)
                layoutManager.orientation = LinearLayoutManager.HORIZONTAL

                holder.view.promotion_data_recycler_view.setOnClickListener {
                    val restaurantMenuFragment = RestaurantMenuBottomSheetFragment()
                    val bundle =  Bundle()
                    bundle.putString("menu", Gson().toJson(promotion.promotionItem.menu))
                    restaurantMenuFragment.arguments = bundle
                    restaurantMenuFragment.show(fragmentManager, restaurantMenuFragment.tag)
                }

                holder.view.promotion_data_recycler_view.layoutManager = layoutManager
                holder.view.promotion_data_recycler_view.adapter = MenuImageListAdapter(promotion.promotionItem.menu.menu.images.images.shuffled() as MutableList<MenuImage>)
            }
            5 -> {
                if(promotion.menus.menus != null){
                    val menus = promotion.menus.menus
                    if (menus.isNotEmpty()){
                        val layoutManager = LinearLayoutManager(holder.view.context)
                        layoutManager.orientation = LinearLayoutManager.HORIZONTAL

                        holder.view.promotion_data_recycler_view.layoutManager = layoutManager
                        holder.view.promotion_data_recycler_view.adapter =
                            RestaurantListMenuAdapter(menus.shuffled() as MutableList<RestaurantMenu>, fragmentManager)
                    } else { holder.view.promotion_data_recycler_view.visibility = View.GONE }
                } else { holder.view.promotion_data_recycler_view.visibility = View.GONE }

                holder.view.promotion_menu_on_text.text = "Promotion On ${promotion.promotionItem.category?.name}"
                Picasso.get().load("${Routes().HOST_END_POINT}${promotion.promotionItem.category?.image}").into(holder.view.promotion_menu_on_image)
            }
            else -> {
                holder.view.promotion_data_recycler_view.visibility = View.GONE
                holder.view.promotion_menu_on.visibility = View.GONE
            }
        }

        if (promotion.reduction.hasReduction){
            holder.view.promotion_reduction.text = "Order this menu and get ${promotion.reduction.amount} ${promotion.reduction.reductionType} reduction"
        } else {
            holder.view.promotion_reduction_icon.visibility = View.GONE
            holder.view.promotion_reduction.visibility = View.GONE
        }

        if(promotion.supplement.hasSupplement){
            if (!promotion.supplement.isSame){ holder.view.promotion_supplement.text = "Order ${promotion.supplement.minQuantity} of this menu and get ${promotion.supplement.quantity} free ${promotion.supplement.supplement?.menu?.name}" }
            else { holder.view.promotion_supplement.text = "Order ${promotion.supplement.minQuantity} of this menu and get ${promotion.supplement.quantity} more for free" }
        } else {
            holder.view.promotion_supplement_icon.visibility = View.GONE
            holder.view.promotion_supplement.visibility = View.GONE
        }

        holder.view.promotion_created_at.text = utilsFunctions.timeAgo(promotion.createdAt)
        holder.view.promotion_interests.text = NumberFormat.getNumberInstance().format(promotion.interests.count)

        holder.view.setOnClickListener {
            val intent = Intent(activity, com.codepipes.ting.MenuPromotion::class.java)
            intent.putExtra("promo", promotion.id)
            intent.putExtra("url", promotion.urls.apiGet)
            activity.startActivity(intent)
        }
    }
}


class RestaurantPromotionViewHolder(val view: View, var promotion: MenuPromotion? = null) : RecyclerView.ViewHolder(view){}