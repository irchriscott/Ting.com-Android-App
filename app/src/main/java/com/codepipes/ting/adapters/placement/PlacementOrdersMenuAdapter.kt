package com.codepipes.ting.adapters.placement

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codepipes.ting.R
import com.codepipes.ting.dialogs.placement.OrderFormDialog
import com.codepipes.ting.interfaces.PlacementOrdersMenuEventsListener
import com.codepipes.ting.interfaces.SubmitOrderListener
import com.codepipes.ting.models.Order
import com.codepipes.ting.utils.Routes
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.row_placement_order_menu.view.*
import java.text.NumberFormat

class PlacementOrdersMenuAdapter (private val orders: MutableList<Order>, private val fragmentManager: FragmentManager, private val ordersMenuEventsListener: PlacementOrdersMenuEventsListener) : RecyclerView.Adapter<PlacementOrdersMenuViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): PlacementOrdersMenuViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val row = layoutInflater.inflate(R.layout.row_placement_order_menu, parent, false)
        return PlacementOrdersMenuViewHolder(row)
    }

    override fun getItemCount(): Int = orders.size

    @SuppressLint("SetTextI18n", "DefaultLocale")
    override fun onBindViewHolder(holder: PlacementOrdersMenuViewHolder, position: Int) {
        val order = orders[position]
        holder.order = order
        val menu = order.menu

        val index = (0 until menu.menu.images.count - 1).random()
        val image = menu.menu.images.images[index]
        Picasso.get().load("${Routes.HOST_END_POINT}${image.image}").into(holder.view.menu_image)

        holder.view.menu_name.text = menu.menu.name
        holder.view.menu_rating.rating = menu.menu.reviews?.average!!.toFloat()

        holder.view.menu_order_quantity.text = "${order.quantity} Pieces / Packs / Bottles"
        holder.view.menu_order_total_price.text =
            "${order.currency} ${NumberFormat.getNumberInstance().format(order.price * order.quantity)}".toUpperCase()
        holder.view.menu_order_single_price.text =
                "${order.currency} ${NumberFormat.getNumberInstance().format(order.price)}".toUpperCase()

        if (!order.isDeclined && !order.isAccepted) {
            holder.view.menu_reorder_button.visibility = View.GONE
            holder.view.menu_order_pending_view.visibility = View.VISIBLE
            holder.view.menu_notify_order_button.setOnClickListener {
                ordersMenuEventsListener.onNotify(order.id)
            }
            holder.view.menu_cancel_order_button.setOnClickListener {
                ordersMenuEventsListener.onCancel(order.id, position)
            }
        } else {
            if (order.isAccepted) {
                holder.view.menu_reorder_button.visibility = View.GONE
                holder.view.menu_order_status_text.text = "Accepted"
                holder.view.menu_order_status_view.background = holder.view.context.resources.getDrawable(R.drawable.background_time_green)
                holder.view.menu_order_status_image.setImageDrawable(holder.view.context.resources.getDrawable(R.drawable.ic_check_white_48dp))
            }
            if (order.isDeclined) {
                holder.view.menu_order_status_text.text = "Declined"
                holder.view.menu_order_status_view.background = holder.view.context.resources.getDrawable(R.drawable.background_time_red)
                holder.view.menu_order_status_image.setImageDrawable(holder.view.context.resources.getDrawable(R.drawable.ic_close_white_24dp))
                holder.view.menu_reorder_button.setOnClickListener {
                    val orderFormDialog = OrderFormDialog()
                    val bundle = Bundle()
                    bundle.putString("quantity", order.quantity.toString())
                    bundle.putString("conditions", order.conditions)
                    orderFormDialog.arguments = bundle
                    orderFormDialog.show(fragmentManager, orderFormDialog.tag)
                    orderFormDialog.onSubmitOrder(object : SubmitOrderListener {
                        override fun onSubmitOrder(quantity: String, conditions: String) {
                            ordersMenuEventsListener.onReorder(order.id, if(quantity != ""){ quantity.toInt() } else { 1 }, conditions.replace("\\s".toRegex(), ""))
                            orderFormDialog.dismiss()
                        }
                    })
                }
            }
        }

        if (order.hasPromotion) {
            holder.view.menu_order_has_promotion_text.text = "YES"
            holder.view.menu_separator_third.visibility = View.VISIBLE
            holder.view.menu_promotions_title.visibility = View.VISIBLE

            if(order.promotion?.reduction != null) {
                holder.view.menu_promotion_reduction.visibility = View.VISIBLE
                holder.view.menu_promotion_reduction_icon.visibility = View.VISIBLE
                holder.view.menu_promotion_reduction.text = order.promotion.reduction
            }

            if(order.promotion?.supplement != null) {
                holder.view.menu_promotion_supplement.visibility = View.VISIBLE
                holder.view.menu_promotion_supplement_icon.visibility = View.VISIBLE
                holder.view.menu_promotion_supplement.text = order.promotion.supplement
            }
        } else { holder.view.menu_order_has_promotion_text.text = "NO" }
    }

    public fun addItems(ordersOthers : MutableList<Order>) {
        val lastPosition = orders.size
        orders.addAll(ordersOthers)
        notifyItemRangeInserted(lastPosition, ordersOthers.size)
    }
}


class PlacementOrdersMenuViewHolder(val view: View, var order: Order? = null) : RecyclerView.ViewHolder(view){}