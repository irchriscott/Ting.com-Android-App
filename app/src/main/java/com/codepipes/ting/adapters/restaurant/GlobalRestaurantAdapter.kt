package com.codepipes.ting.adapters.restaurant

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.app.FragmentManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codepipes.ting.R
import com.codepipes.ting.RestaurantProfile
import com.codepipes.ting.fragments.restaurants.RestaurantsMapFragment
import com.codepipes.ting.models.Branch
import com.codepipes.ting.models.RestaurantMenu
import com.codepipes.ting.utils.UtilsFunctions
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.row_restaurant.view.*
import java.text.NumberFormat
import java.util.*

@SuppressLint("SetTextI18n")
class GlobalRestaurantAdapter (private val restaurants: MutableList<Branch>, private val fragmentManager: FragmentManager) : RecyclerView.Adapter<RestaurantViewHolder>() {

    private lateinit var utilsFunctions: UtilsFunctions

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): RestaurantViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val row = layoutInflater.inflate(R.layout.row_restaurant, parent, false)
        return RestaurantViewHolder(row)
    }

    override fun getItemCount(): Int = restaurants.size


    override fun onBindViewHolder(holder: RestaurantViewHolder, position: Int) {
        val branch = restaurants[position]
        holder.branch = branch

        val activity = holder.view.context as Activity
        utilsFunctions = UtilsFunctions(holder.view.context)

        Picasso.get().load(branch.restaurant?.logoURL()).into(holder.view.restaurant_image)
        holder.view.restaurant_name.text = "${branch.restaurant?.name}, ${branch.name}"
        holder.view.restaurant_rating.rating = branch.reviews?.average!!.toFloat()
        holder.view.restaurant_address.text = branch.address
        holder.view.restaurant_distance.text = "${branch.dist} km"
        holder.view.restaurant_likes.text = NumberFormat.getNumberInstance().format(branch.likes?.count)
        holder.view.restaurant_reviews.text = NumberFormat.getNumberInstance().format(branch.reviews.count)
        holder.view.restaurant_specials.text = branch.specials.size.toString()

        holder.view.restaurant_name.setOnClickListener {
            val intent = Intent(holder.view.context, RestaurantProfile::class.java)
            intent.putExtra("resto",branch.id)
            intent.putExtra("tab", 0)
            activity.startActivity(intent)
        }

        holder.view.setOnClickListener {
            val intent = Intent(holder.view.context, RestaurantProfile::class.java)
            intent.putExtra("resto", branch.id)
            intent.putExtra("tab", 0)
            activity.startActivity(intent)
        }

        if(!branch.menus.menus.isNullOrEmpty()) {
            val layoutManager = LinearLayoutManager(holder.view.context)
            layoutManager.orientation = LinearLayoutManager.HORIZONTAL

            holder.view.restaurant_menus.layoutManager = layoutManager
            holder.view.restaurant_menus.adapter =
                RestaurantListMenuAdapter(branch.menus.menus.shuffled() as MutableList<RestaurantMenu>, fragmentManager)
        } else { holder.view.restaurant_menus.visibility = View.GONE }

        if (branch.categories.count > 0) {
            holder.view.restaurant_cuisines.text = branch.categories.categories.shuffled().joinToString(", ") { it.name }
        } else {
            holder.view.restaurant_cuisines_pin.visibility = View.GONE
            holder.view.restaurant_cuisines.visibility = View.GONE
        }

        if (!branch.restaurant?.foodCategories?.categories.isNullOrEmpty()) {
            holder.view.restaurant_categories.text = branch.restaurant!!.foodCategories.categories.shuffled().joinToString(", ") { it.name }
        } else {
            holder.view.restaurant_categories.visibility = View.GONE
            holder.view.restaurant_categories_pin.visibility = View.GONE
        }

        if(branch.isAvailable) {

            val status = utilsFunctions.statusWorkTime(branch.restaurant?.opening!!, branch.restaurant.closing)
            holder.view.restaurant_time.text = status["msg"]

            when (status["clr"]) {
                "green" -> {
                    holder.view.restaurant_work_status.background =
                        holder.view.context.resources.getDrawable(R.drawable.background_time_green)
                }
                "orange" -> {
                    holder.view.restaurant_work_status.background =
                        holder.view.context.resources.getDrawable(R.drawable.background_time_orange)
                }
                "red" -> {
                    holder.view.restaurant_work_status.background =
                        holder.view.context.resources.getDrawable(R.drawable.background_time_red)
                }
            }

            Timer().scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    activity.runOnUiThread {

                        val statusTimer = utilsFunctions.statusWorkTime(branch.restaurant.opening, branch.restaurant.closing)
                        holder.view.restaurant_time.text = statusTimer?.get("msg")

                        when (statusTimer?.get("clr")) {
                            "green" -> {
                                holder.view.restaurant_work_status.background =
                                    holder.view.context.resources.getDrawable(R.drawable.background_time_green)
                            }
                            "orange" -> {
                                holder.view.restaurant_work_status.background =
                                    holder.view.context.resources.getDrawable(R.drawable.background_time_orange)
                            }
                            "red" -> {
                                holder.view.restaurant_work_status.background =
                                    holder.view.context.resources.getDrawable(R.drawable.background_time_red)
                            }
                        }
                    }
                }
            }, 0, 10000)
        } else {
            holder.view.restaurant_work_status.background = holder.view.context.resources.getDrawable(R.drawable.background_time_red)
            holder.view.restaurant_work_status_icon.setImageDrawable(holder.view.context.resources.getDrawable(R.drawable.ic_close_white_24dp))
            holder.view.restaurant_time.text = "Not Available"
        }

        holder.view.restaurant_address.setOnClickListener {
            val cx = (it.x + it.width / 2).toInt()
            val cy = (it.y + it.height).toInt()

            val mapFragment =  RestaurantsMapFragment()
            val args: Bundle = Bundle()

            args.putInt("cx", cx)
            args.putInt("cy", cy)
            args.putString("resto", Gson().toJson(branch))

            mapFragment.arguments = args
            mapFragment.show(fragmentManager, mapFragment.tag)
        }

        holder.view.restaurant_distance_view.setOnClickListener {
            val cx = (it.x + it.width / 2).toInt()
            val cy = (it.y + it.height).toInt()

            val mapFragment =  RestaurantsMapFragment()
            val args: Bundle = Bundle()

            args.putInt("cx", cx)
            args.putInt("cy", cy)
            args.putString("resto", Gson().toJson(branch))

            mapFragment.arguments = args
            mapFragment.show(fragmentManager, mapFragment.tag)
        }
    }
}

class RestaurantViewHolder(val view: View, var branch: Branch? = null) : RecyclerView.ViewHolder(view){}