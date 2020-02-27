package com.codepipes.ting.adapters.placement

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Paint
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.codepipes.ting.activities.placement.CurrentRestaurant
import com.codepipes.ting.R
import com.codepipes.ting.dialogs.messages.TingToast
import com.codepipes.ting.dialogs.messages.TingToastType
import com.codepipes.ting.dialogs.placement.OrderFormDialog
import com.codepipes.ting.fragments.menu.MenuBottomSheetFragment
import com.codepipes.ting.interfaces.SubmitOrderListener
import com.codepipes.ting.models.Placement
import com.codepipes.ting.models.RestaurantMenu
import com.codepipes.ting.models.ServerResponse
import com.codepipes.ting.models.User
import com.codepipes.ting.providers.UserAuthentication
import com.codepipes.ting.providers.UserPlacement
import com.codepipes.ting.utils.Routes
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.row_restaurant_menu_order.view.*
import okhttp3.*
import java.io.IOException
import java.lang.Exception
import java.text.NumberFormat
import java.util.concurrent.TimeUnit

class RestaurantMenusOrderAdapter (private val menus: MutableList<RestaurantMenu>, val fragmentManager: FragmentManager, val context: Context) : RecyclerView.Adapter<RestaurantMenusOrderViewHolder>(){

    private lateinit var userAuthentication: UserAuthentication
    private lateinit var session: User

    private lateinit var placement: Placement

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): RestaurantMenusOrderViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val row = layoutInflater.inflate(R.layout.row_restaurant_menu_order, parent, false)
        userAuthentication = UserAuthentication(context)
        session = userAuthentication.get()!!
        val userPlacement = UserPlacement(context)
        placement = userPlacement.get()!!
        return RestaurantMenusOrderViewHolder(row)
    }

    override fun getItemCount(): Int = menus.size

    @SuppressLint("SetTextI18n", "DefaultLocale")
    override fun onBindViewHolder(holder: RestaurantMenusOrderViewHolder, position: Int) {

        val menu = menus[position]
        holder.menu = menu

        val index = (0 until menu.menu.images.count - 1).random()
        val image = menu.menu.images.images[index]
        Picasso.get().load("${Routes().HOST_END_POINT}${image.image}").into(holder.view.menu_image)

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
                Picasso.get().load("${Routes().HOST_END_POINT}${menu.menu.category?.image}").into(holder.view.menu_category_image)
                holder.view.menu_cuisine_name.text = menu.menu.cuisine?.name
                Picasso.get().load("${Routes().HOST_END_POINT}${menu.menu.cuisine?.image}").into(holder.view.menu_cuisine_image)
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
                holder.view.menu_type_image.setImageDrawable(holder.view.context.resources.getDrawable(R.drawable.ic_clock_gray_24dp))
                holder.view.menu_category_name.text = menu.menu.category?.name
                Picasso.get().load("${Routes().HOST_END_POINT}${menu.menu.category?.image}").into(holder.view.menu_category_image)
                holder.view.menu_type_name.text = menu.menu.dishTime
                holder.view.menu_cuisine_name.text = menu.menu.cuisine?.name
                Picasso.get().load("${Routes().HOST_END_POINT}${menu.menu.cuisine?.image}").into(holder.view.menu_cuisine_image)
                if (menu.menu.isCountable) {
                    holder.view.menu_quantity.text = "${menu.menu.quantity} plates / packs"
                } else { holder.view.menu_quantity.visibility = View.GONE }
            }
        }

        holder.view.menu_order_button.setOnClickListener {
            val orderFormDialog = OrderFormDialog()
            orderFormDialog.show(fragmentManager, orderFormDialog.tag)
            orderFormDialog.onSubmitOrder(object : SubmitOrderListener {
                override fun onSubmitOrder(quantity: String, conditions: String) {
                    submitOrder(if(quantity != ""){ quantity.toInt() } else { 1 }, conditions, menu.id)
                    orderFormDialog.dismiss()
                }
            })
        }

        holder.view.setOnClickListener {
            val menuBottomSheet = MenuBottomSheetFragment()
            val bundle = Bundle()
            bundle.putString("menu", Gson().toJson(menu.menu))
            bundle.putBoolean(CurrentRestaurant.MENU_FROM_ORDER_KEY, true)
            menuBottomSheet.arguments = bundle
            menuBottomSheet.show(fragmentManager, menuBottomSheet.tag)
        }
    }

    private fun submitOrder(quantity: Int, conditions: String, menu: Int) {

        val url = Routes().placeOrderMenu
        val activity = context as Activity

        val client = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60 * 5, TimeUnit.SECONDS)
            .build()

        val form = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("token", placement.token)
            .addFormDataPart("quantity", quantity.toString())
            .addFormDataPart("conditions", conditions)
            .addFormDataPart("menu", menu.toString())
            .build()

        val request = Request.Builder()
            .header("Authorization", session.token!!)
            .url(url)
            .post(form)
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                activity.runOnUiThread { TingToast(
                    context,
                    e.localizedMessage,
                    TingToastType.ERROR
                ).showToast(Toast.LENGTH_LONG) }
            }

            override fun onResponse(call: Call, response: Response) {
                val dataString = response.body()!!.string()
                activity.runOnUiThread {
                    try {
                        val serverResponse = Gson().fromJson(dataString, ServerResponse::class.java)
                        if (serverResponse.type == "success") {
                            TingToast(
                                context,
                                serverResponse.message,
                                TingToastType.SUCCESS
                            ).showToast(Toast.LENGTH_LONG)
                        } else { TingToast(
                            context,
                            serverResponse.message,
                            TingToastType.ERROR
                        ).showToast(Toast.LENGTH_LONG) }
                    } catch (e: Exception) { TingToast(
                        context,
                        e.localizedMessage,
                        TingToastType.ERROR
                    ).showToast(Toast.LENGTH_LONG) }
                }
            }
        })
    }
}


class RestaurantMenusOrderViewHolder(val view: View, var menu: RestaurantMenu? = null) : RecyclerView.ViewHolder(view){}