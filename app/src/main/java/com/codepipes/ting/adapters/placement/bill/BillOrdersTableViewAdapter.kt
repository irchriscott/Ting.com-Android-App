package com.codepipes.ting.adapters.placement.bill

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codepipes.ting.R
import com.codepipes.ting.adapters.tableview.holders.CellViewHolder
import com.codepipes.ting.adapters.tableview.holders.ColumnHeaderViewHolder
import com.codepipes.ting.adapters.tableview.holders.RowHeaderViewHolder
import com.codepipes.ting.adapters.tableview.models.CellModel
import com.codepipes.ting.adapters.tableview.models.ColumnHeaderModel
import com.codepipes.ting.adapters.tableview.models.RowHeaderModel
import com.codepipes.ting.models.OrderData
import com.codepipes.ting.tableview.adapter.AbstractTableAdapter
import com.codepipes.ting.tableview.adapter.recyclerview.holder.AbstractSorterViewHolder
import com.codepipes.ting.tableview.adapter.recyclerview.holder.AbstractViewHolder


class BillOrdersTableViewAdapter(private val context: Context?) :

    AbstractTableAdapter<ColumnHeaderModel?, RowHeaderModel?, CellModel?>() {

    private val ordersTableViewModel: BillOrdersTableViewModel = BillOrdersTableViewModel()

    override fun onCreateCellViewHolder(parent: ViewGroup, viewType: Int): AbstractViewHolder {
        val layout = LayoutInflater.from(context).inflate(R.layout.tableview_cell_view, parent, false)
        return CellViewHolder(layout)
    }

    override fun onCreateColumnHeaderViewHolder(parent: ViewGroup, viewType: Int): AbstractSorterViewHolder {
        val layout: View = LayoutInflater.from(context).inflate(R.layout.tableview_column_header_view, parent, false)
        return ColumnHeaderViewHolder(layout, tableView)
    }

    override fun onCreateRowHeaderViewHolder(parent: ViewGroup, viewType: Int): AbstractViewHolder {
        val layout: View = LayoutInflater.from(context).inflate(R.layout.tableview_row_header_view, parent, false)
        return RowHeaderViewHolder(layout)
    }

    override fun onBindCellViewHolder(
        holder: AbstractViewHolder,
        cellItemModel: CellModel?,
        columnPosition: Int,
        rowPosition: Int
    ) {
        val cell = cellItemModel as CellModel?
        if (holder is CellViewHolder) {
            holder.setCellModel(cell!!, columnPosition)
        }
    }

    override fun onBindColumnHeaderViewHolder(
        holder: AbstractViewHolder,
        columnHeaderItemModel: ColumnHeaderModel?,
        columnPosition: Int
    ) {
        val columnHeader = columnHeaderItemModel as ColumnHeaderModel?
        val columnHeaderViewHolder = holder as ColumnHeaderViewHolder
        columnHeaderViewHolder.setColumnHeaderModel(columnHeader!!, columnPosition)
    }

    override fun onBindRowHeaderViewHolder(
        holder: AbstractViewHolder,
        rowHeaderItemModel: RowHeaderModel?,
        rowPosition: Int
    ) {
        val rowHeaderModel = rowHeaderItemModel as RowHeaderModel
        val rowHeaderViewHolder = holder as RowHeaderViewHolder
        rowHeaderViewHolder.rowHeaderTextView.text = rowHeaderModel.data
    }

    override fun getCellItemViewType(position: Int): Int = 0

    @SuppressLint("InflateParams")
    override fun onCreateCornerView(parent: ViewGroup): View {
        return LayoutInflater.from(context).inflate(R.layout.tableview_corner_view, null, false);
    }

    override fun getColumnHeaderItemViewType(position: Int): Int = 0

    override fun getRowHeaderItemViewType(position: Int): Int = 0

    fun setOrdersList(orders: List<OrderData>) {
        ordersTableViewModel.generateListForTableView(orders)
        setAllItems(
            ordersTableViewModel.columnHeaderModeList, ordersTableViewModel
                .rowHeaderModelList, ordersTableViewModel.cellModelList
        )
    }
}