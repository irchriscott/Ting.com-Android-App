package com.codepipes.ting.adapters.tableview.holders

import androidx.core.content.ContextCompat
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.codepipes.ting.R
import com.codepipes.ting.adapters.tableview.models.CellModel
import com.codepipes.ting.tableview.adapter.recyclerview.holder.AbstractViewHolder


class CellViewHolder(private val view: View) : AbstractViewHolder(view) {

    private var cellTextView: TextView = view.findViewById(R.id.cell_data)
    private var cellContainer: LinearLayout = view.findViewById(R.id.cell_container)

    fun setCellModel(cellModel: CellModel, columnPosition: Int) {
        cellTextView.gravity = Gravity.CENTER_VERTICAL
        cellTextView.text = cellModel.data
        cellContainer.layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT
        cellTextView.requestLayout()
    }

    override fun setSelected(selectionState: SelectionState) {
        super.setSelected(selectionState)
        if (selectionState == SelectionState.SELECTED) {
            cellTextView.setTextColor(
                ContextCompat.getColor(
                    cellTextView.context,
                    R.color.colorPrimaryDark
                )
            )
        } else {
            cellTextView.setTextColor(
                ContextCompat.getColor(
                    cellTextView.context,
                    R.color.colorGray
                )
            )
        }
    }

}