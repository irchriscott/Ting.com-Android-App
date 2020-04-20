package com.codepipes.ting.adapters.others

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.codepipes.ting.R
import com.codepipes.ting.activities.menu.RestaurantMenu
import com.codepipes.ting.activities.restaurant.RestaurantProfile
import com.codepipes.ting.models.SearchResult
import com.codepipes.ting.utils.Routes
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.row_search_result.view.*

class SearchResultAdapter (val results: MutableList<SearchResult>) : RecyclerView.Adapter<SearchResultViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val row = layoutInflater.inflate(R.layout.row_search_result, parent, false)
        return SearchResultViewHolder(row)
    }

    override fun getItemCount(): Int = results.size

    @SuppressLint("DefaultLocale")
    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        val result = results[position]
        holder.view.search_name.text = result.name
        holder.view.search_description.text = result.description
        holder.view.search_text.text = if(result.type == 1) { result.text } else { result.text.toUpperCase() }
        Picasso.get().load("${Routes.HOST_END_POINT}${result.image}").into(holder.view.search_image)
        if(result.type == 2) { holder.view.search_text.textSize = 15.0f }

        val activity = holder.view.context as Activity

        holder.view.setOnClickListener {
            when(result.type) {
                1 -> {
                    val intent = Intent(holder.view.context, RestaurantProfile::class.java)
                    intent.putExtra("resto", result.id)
                    intent.putExtra("url", result.apiGet)
                    activity.startActivity(intent)
                }
                2 -> {
                    val intent = Intent(holder.view.context, RestaurantMenu::class.java)
                    intent.putExtra("menu", result.id)
                    intent.putExtra("url", result.apiGet)
                    activity.startActivity(intent)
                }
                else -> {}
            }
        }
    }
}

class SearchResultViewHolder(val view: View) : RecyclerView.ViewHolder(view){}