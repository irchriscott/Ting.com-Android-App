package com.codepipes.ting.adapters.cuisine

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codepipes.ting.R
import com.codepipes.ting.activities.restaurant.RestaurantProfile
import com.codepipes.ting.fragments.restaurants.RestaurantsMapFragment
import com.codepipes.ting.models.Branch
import com.codepipes.ting.utils.UtilsFunctions
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.row_cuisine_restaurant.view.*
import java.text.NumberFormat
import java.util.*


class CuisineRestaurantsAdapter (
    private val branches: MutableList<Branch>,
    private val fragmentManager: FragmentManager,
    private val statusWorkTimer: Timer
) : RecyclerView.Adapter<CuisineRestaurantsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): CuisineRestaurantsViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val row = layoutInflater.inflate(R.layout.row_cuisine_restaurant, parent, false)
        return CuisineRestaurantsViewHolder(row)
    }

    override fun getItemCount(): Int = branches.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: CuisineRestaurantsViewHolder, position: Int) {
        val branch = branches[position]

        val activity = holder.view.context as Activity
        val utilsFunctions = UtilsFunctions(holder.view.context)

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

        holder.view.restaurant_image.setOnClickListener {
            val intent = Intent(holder.view.context, RestaurantProfile::class.java)
            intent.putExtra("resto", branch.id)
            intent.putExtra("tab", 0)
            activity.startActivity(intent)
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

            statusWorkTimer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    activity.runOnUiThread {

                        val statusTimer = utilsFunctions.statusWorkTime(branch.restaurant.opening, branch.restaurant.closing)
                        holder.view.restaurant_time.text = statusTimer["msg"]

                        when (statusTimer["clr"]) {
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

            mapFragment.arguments = args
            mapFragment.setRestaurant(Gson().toJson(branch))
            mapFragment.show(fragmentManager, mapFragment.tag)
        }

        holder.view.restaurant_distance_view.setOnClickListener {
            val cx = (it.x + it.width / 2).toInt()
            val cy = (it.y + it.height).toInt()

            val mapFragment =  RestaurantsMapFragment()
            val args: Bundle = Bundle()

            args.putInt("cx", cx)
            args.putInt("cy", cy)

            mapFragment.arguments = args
            mapFragment.setRestaurant(Gson().toJson(branch))
            mapFragment.show(fragmentManager, mapFragment.tag)
        }
    }

    public fun addItems(restosOthers : MutableList<Branch>) {
        val lastPosition = branches.size
        branches.addAll(restosOthers)
        notifyItemRangeInserted(lastPosition, restosOthers.size)
    }
}

class CuisineRestaurantsViewHolder(val view: View, var branch: Branch? = null) : RecyclerView.ViewHolder(view) {}