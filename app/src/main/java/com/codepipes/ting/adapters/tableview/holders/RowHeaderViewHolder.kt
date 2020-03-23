package com.codepipes.ting.adapters.tableview.holders

import androidx.core.content.ContextCompat
import android.view.View
import android.widget.TextView
import com.codepipes.ting.R
import com.codepipes.ting.tableview.adapter.recyclerview.holder.AbstractViewHolder


class RowHeaderViewHolder(private val view: View) : AbstractViewHolder(view) {

    val rowHeaderTextView: TextView = view.findViewById(R.id.row_header_textview)

    override fun setSelected(selectionState: SelectionState) {
        super.setSelected(selectionState)
        val nBackgroundColorId: Int
        val nForegroundColorId: Int
        when (selectionState) {
            SelectionState.SELECTED -> {
                nBackgroundColorId = R.color.colorVeryLightGray
                nForegroundColorId = R.color.colorPrimaryDark
            }
            SelectionState.UNSELECTED -> {
                nBackgroundColorId = R.color.colorWhite
                nForegroundColorId = R.color.colorGray
            }
            else -> {
                nBackgroundColorId = R.color.colorWhite
                nForegroundColorId = R.color.colorGray
            }
        }
        itemView.setBackgroundColor(
            ContextCompat.getColor(
                itemView.context,
                nBackgroundColorId
            )
        )
        rowHeaderTextView.setTextColor(
            ContextCompat.getColor(
                rowHeaderTextView.context,
                nForegroundColorId
            )
        )
    }
}