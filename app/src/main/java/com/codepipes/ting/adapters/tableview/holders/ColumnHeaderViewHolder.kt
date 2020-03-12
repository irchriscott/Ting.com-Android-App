package com.codepipes.ting.adapters.tableview.holders

import android.support.v4.content.ContextCompat
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.codepipes.ting.R
import com.codepipes.ting.adapters.tableview.models.ColumnHeaderModel
import com.codepipes.ting.tableview.ITableView
import com.codepipes.ting.tableview.adapter.recyclerview.holder.AbstractSorterViewHolder


class ColumnHeaderViewHolder(private val view: View, val tableView: ITableView) : AbstractSorterViewHolder(view) {

    private var columnHeaderContainer: LinearLayout = view.findViewById(R.id.column_header_container)
    private var columnHeaderTextView: TextView = view.findViewById(R.id.column_header_textView)

    fun setColumnHeaderModel(columnHeaderModel: ColumnHeaderModel, columnPosition: Int) {
        columnHeaderTextView.gravity = Gravity.CENTER_VERTICAL
        columnHeaderTextView.text = columnHeaderModel.data
        columnHeaderContainer.layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT
        columnHeaderTextView.requestLayout()
    }

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
        columnHeaderContainer.setBackgroundColor(
            ContextCompat.getColor(
                columnHeaderContainer
                    .context, nBackgroundColorId
            )
        )
        columnHeaderTextView.setTextColor(
            ContextCompat.getColor(
                columnHeaderContainer
                    .context, nForegroundColorId
            )
        )
    }
}