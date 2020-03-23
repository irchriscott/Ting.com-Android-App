package com.codepipes.ting.imageeditor.editimage.adapter.viewholders

import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.codepipes.ting.R

class FilterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    @JvmField
    var icon: ImageView = itemView.findViewById<View>(R.id.filter_image) as ImageView
    @JvmField
    var text: TextView = itemView.findViewById<View>(R.id.filter_name) as TextView
}