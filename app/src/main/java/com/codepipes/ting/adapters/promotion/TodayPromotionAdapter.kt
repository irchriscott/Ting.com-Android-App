package com.codepipes.ting.adapters.promotion

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codepipes.ting.R
import com.codepipes.ting.models.MenuPromotion
import com.codepipes.ting.utils.Routes
import com.codepipes.ting.utils.UtilsFunctions
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.row_today_promotion.view.*
import java.text.NumberFormat

class TodayPromotionAdapter (private val promotions: MutableList<MenuPromotion>) : RecyclerView.Adapter<TodayPromotionViewHolder>(){

    private lateinit var utilsFunctions: UtilsFunctions

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): TodayPromotionViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val row = layoutInflater.inflate(R.layout.row_today_promotion, parent, false)
        return TodayPromotionViewHolder(row)
    }

    override fun getItemCount(): Int = promotions.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: TodayPromotionViewHolder, position: Int) {
        val promotion = promotions[position]
        holder.promotion = promotion

        utilsFunctions = UtilsFunctions(holder.view.context)
        val activity = holder.view.context as Activity

        Picasso.get().load("${Routes.HOST_END_POINT}${promotion.posterImage}").into(holder.view.promotion_poster)
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
            4 -> {
                val index = (0 until promotion.promotionItem.menu?.menu?.images?.count!! - 1).random()
                val image = promotion.promotionItem.menu.menu.images.images[index]
                Picasso.get().load("${Routes.HOST_END_POINT}${image.image}").into(holder.view.promotion_menu_on_image)
                holder.view.promotion_menu_on_text.text = "Promotion On ${promotion.promotionItem.menu.menu.name}"
            }
            5 -> {
                holder.view.promotion_menu_on_text.text = "Promotion On ${promotion.promotionItem.category?.name}"
                Picasso.get().load("${Routes.HOST_END_POINT}${promotion.promotionItem.category?.image}").into(holder.view.promotion_menu_on_image)
            }
            else -> { holder.view.promotion_menu_on.visibility = View.GONE }
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

        holder.view.promotion_restaurant_name.text = "${promotion.restaurant?.name}, ${promotion.branch?.name}"
        Picasso.get().load(promotion.restaurant?.logoURL()).into(holder.view.promotion_restaurant_image)

        holder.view.promotion_created_at.text = utilsFunctions.timeAgo(promotion.createdAt)
        holder.view.promotion_interests.text = NumberFormat.getNumberInstance().format(promotion.interests.count)

        holder.view.setOnClickListener {
            val intent = Intent(activity, com.codepipes.ting.activities.menu.MenuPromotion::class.java)
            intent.putExtra("promo", promotion.id)
            intent.putExtra("url", promotion.urls.apiGet)
            activity.startActivity(intent)
        }
    }
}


class TodayPromotionViewHolder(val view: View, var promotion: MenuPromotion? = null) : RecyclerView.ViewHolder(view){}