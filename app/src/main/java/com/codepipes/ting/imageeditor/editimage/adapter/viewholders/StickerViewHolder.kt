package com.codepipes.ting.imageeditor.editimage.adapter.viewholders

import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import com.codepipes.ting.R

class StickerViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
    @JvmField
    var image: ImageView = itemView.findViewById(R.id.img)
}