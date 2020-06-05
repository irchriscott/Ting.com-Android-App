package com.codepipes.ting.adapters.promotion

import android.annotation.SuppressLint
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codepipes.ting.R
import com.codepipes.ting.models.MenuPromotion
import com.codepipes.ting.utils.Routes
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.row_restaurant_menu_promotion.view.*


class PromotionRestaurantMenuAdapter (private val promotions: MutableList<MenuPromotion>) : RecyclerView.Adapter<PromotionRestaurantMenuViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): PromotionRestaurantMenuViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val row = layoutInflater.inflate(R.layout.row_restaurant_menu_promotion, parent, false)
        return PromotionRestaurantMenuViewHolder(row)
    }

    override fun getItemCount(): Int = promotions.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: PromotionRestaurantMenuViewHolder, position: Int) {
        val promotion = promotions[position]
        Picasso.get().load("${Routes.HOST_END_POINT}${promotion.posterImage}").into(holder.view.promotion_poster)
        holder.view.promotion_title.text = promotion.occasionEvent
        holder.view.promotion_time.text = promotion.period

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
    }
}


class PromotionRestaurantMenuViewHolder(val view: View, val promotion: MenuPromotion? = null) : RecyclerView.ViewHolder(view){}