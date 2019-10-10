package com.codepipes.ting.adapters.restaurant

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codepipes.ting.R
import com.codepipes.ting.models.MenuReview
import com.codepipes.ting.models.RestaurantReview
import com.codepipes.ting.utils.UtilsFunctions
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.row_review.view.*

class RestaurantReviewsAdapter (private val reviews: MutableList<RestaurantReview>) : RecyclerView.Adapter<RestaurantReviewsViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): RestaurantReviewsViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val row = layoutInflater.inflate(R.layout.row_review, parent, false)
        return RestaurantReviewsViewHolder(row)
    }

    override fun getItemCount(): Int = reviews.size

    override fun onBindViewHolder(holder: RestaurantReviewsViewHolder, position: Int) {
        val review = reviews[position]
        Picasso.get().load(review.user?.imageURL()).into(holder.view.review_image)

        val utilsFunctions = UtilsFunctions(holder.view.context)

        holder.view.review_user_name.text = review.user?.name
        holder.view.review_rating.rating = review.review.toFloat()
        holder.view.review_comment.text = review.comment
        holder.view.review_date.text = utilsFunctions.timeAgo(review.updatedAt)
    }
}

class RestaurantReviewsViewHolder(val view: View, val review: RestaurantReview? = null) : RecyclerView.ViewHolder(view){}