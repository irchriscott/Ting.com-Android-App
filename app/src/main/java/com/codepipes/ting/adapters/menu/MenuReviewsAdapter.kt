package com.codepipes.ting.adapters.menu

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codepipes.ting.R
import com.codepipes.ting.models.MenuReview
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.row_review.view.*

class MenuReviewsAdapter (private val reviews: MutableList<MenuReview>) : RecyclerView.Adapter<MenuReviewsViewHolder>(){
    override fun onCreateViewHolder(parent: ViewGroup, type: Int): MenuReviewsViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val row = layoutInflater.inflate(R.layout.row_review, parent, false)
        return MenuReviewsViewHolder(row)
    }

    override fun getItemCount(): Int = reviews.size

    override fun onBindViewHolder(holder: MenuReviewsViewHolder, position: Int) {
        val review = reviews[position]
        Picasso.get().load(review.user.imageURL()).into(holder.view.review_image)
        holder.view.review_user_name.text = review.user.name
        holder.view.review_rating.rating = review.review.toFloat()
        holder.view.review_comment.text = review.comment
        holder.view.review_date.text = review.createdAt
    }
}

class MenuReviewsViewHolder(val view: View, val review: MenuReview? = null) : RecyclerView.ViewHolder(view){}