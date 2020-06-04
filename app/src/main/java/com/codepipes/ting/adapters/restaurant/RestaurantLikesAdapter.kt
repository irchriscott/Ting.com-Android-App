package com.codepipes.ting.adapters.restaurant

import android.annotation.SuppressLint
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codepipes.ting.R
import com.codepipes.ting.models.RestaurantReview
import com.codepipes.ting.models.UserRestaurant
import com.codepipes.ting.utils.UtilsFunctions
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.row_like.view.*


class RestaurantLikesAdapter (private val likes: MutableList<UserRestaurant>) : RecyclerView.Adapter<RestaurantLikesViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): RestaurantLikesViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val row = layoutInflater.inflate(R.layout.row_like, parent, false)
        return RestaurantLikesViewHolder(row)
    }

    override fun getItemCount(): Int = likes.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RestaurantLikesViewHolder, position: Int) {
        val like = likes[position]
        Picasso.get().load(like.user.imageURL()).fit().into(holder.view.like_image)

        val utilsFunctions = UtilsFunctions(holder.view.context)

        holder.view.like_name.text = like.user.name
        holder.view.like_address.text = "${like.user.town}, ${like.user.country}"
        holder.view.like_date.text = utilsFunctions.timeAgo(like.createdAt)
    }

    public fun addItems(likesOthers : MutableList<UserRestaurant>) {
        val lastPosition = likes.size
        likes.addAll(likesOthers)
        notifyItemRangeInserted(lastPosition, likesOthers.size)
    }
}

class RestaurantLikesViewHolder(val view: View, val like: UserRestaurant? = null) : RecyclerView.ViewHolder(view){}