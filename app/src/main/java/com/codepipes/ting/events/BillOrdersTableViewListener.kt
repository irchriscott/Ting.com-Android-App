package com.codepipes.ting.events

import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView

import com.codepipes.ting.adapters.tableview.holders.ColumnHeaderViewHolder
import com.codepipes.ting.tableview.ITableView
import com.codepipes.ting.tableview.listener.ITableViewListener


class BillOrdersTableViewListener(private val mTableView: ITableView) : ITableViewListener {

    override fun onCellLongPressed(cellView: RecyclerView.ViewHolder, column: Int, row: Int) {}

    override fun onColumnHeaderLongPressed(columnHeaderView: RecyclerView.ViewHolder, column: Int) {}

    override fun onRowHeaderClicked(rowHeaderView: RecyclerView.ViewHolder, row: Int) {}

    override fun onColumnHeaderClicked(columnHeaderView: RecyclerView.ViewHolder, column: Int) {}

    override fun onCellClicked(cellView: RecyclerView.ViewHolder, column: Int, row: Int) {}

    override fun onColumnHeaderDoubleClicked(columnHeaderView: RecyclerView.ViewHolder, column: Int) {}

    override fun onCellDoubleClicked(cellView: RecyclerView.ViewHolder, column: Int, row: Int) {}

    override fun onRowHeaderLongPressed(rowHeaderView: RecyclerView.ViewHolder, row: Int) {}

    override fun onRowHeaderDoubleClicked(rowHeaderView: RecyclerView.ViewHolder, row: Int) {}
}