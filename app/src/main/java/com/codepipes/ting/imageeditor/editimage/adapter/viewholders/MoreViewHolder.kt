package com.codepipes.ting.imageeditor.editimage.adapter.viewholders

import android.support.v7.widget.RecyclerView
import android.view.View
import com.codepipes.ting.R

class MoreViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
    @JvmField var moreButton: View = itemView.findViewById(R.id.color_panel_more)
}