package com.codepipes.ting.adapters.restaurant

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.app.FragmentManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codepipes.ting.R
import com.codepipes.ting.fragments.restaurants.RestaurantsMapFragment
import com.codepipes.ting.models.Branch
import com.codepipes.ting.utils.UtilsFunctions
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.row_restaurant.view.*
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

        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                activity.runOnUiThread {
                    val status = utilsFunctions.statusWorkTime(branch.restaurant?.opening!!, branch.restaurant.closing)
                    holder.view.restaurant_time.text = status?.get("msg")

                    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
                    when(status?.get("clr")){
                        "green" -> {
                            holder.view.restaurant_work_status.background = holder.view.context.getDrawable(R.drawable.background_time_green)
                        }
                        "orange" -> {
                            holder.view.restaurant_work_status.background = holder.view.context.getDrawable(R.drawable.background_time_orange)
                        }
                        "red" -> {
                            holder.view.restaurant_work_status.background = holder.view.context.getDrawable(R.drawable.background_time_red)
                        }
                    }
                }
            }
        }, 0, 10000)

        holder.view.restaurant_address.setOnClickListener {
            val cx = (it.x + it.width / 2).toInt()
            val cy = (it.y + it.height + 56).toInt()

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