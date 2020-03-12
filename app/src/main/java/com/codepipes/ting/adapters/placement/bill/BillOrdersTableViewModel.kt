package com.codepipes.ting.adapters.placement.bill

import android.annotation.SuppressLint
import android.view.Gravity
import com.codepipes.ting.adapters.tableview.models.CellModel
import com.codepipes.ting.adapters.tableview.models.ColumnHeaderModel
import com.codepipes.ting.adapters.tableview.models.RowHeaderModel
import com.codepipes.ting.models.Bill
import com.codepipes.ting.models.OrderData
import java.text.NumberFormat


class BillOrdersTableViewModel {

    var columnHeaderModeList: List<ColumnHeaderModel>? = null
        private set
    var rowHeaderModelList: List<RowHeaderModel>? = null
        private set
    var cellModelList: List<List<CellModel>>? = null
        private set

    private fun createColumnHeaderModelList(): List<ColumnHeaderModel> {

        val list: MutableList<ColumnHeaderModel> = ArrayList()

        list.add(ColumnHeaderModel("ID"))
        list.add(ColumnHeaderModel("Menu"))
        list.add(ColumnHeaderModel("Price"))
        list.add(ColumnHeaderModel("Quantity"))
        list.add(ColumnHeaderModel("Total"))
        list.add(ColumnHeaderModel("Has Promotion"))

        return list
    }

    @SuppressLint("DefaultLocale")
    private fun createCellModelList(orders: List<OrderData>): List<List<CellModel>> {

        val lists: MutableList<List<CellModel>> = ArrayList()

        for (i in orders.indices) {
            val order = orders[i]
            val list: MutableList<CellModel> = ArrayList()

            list.add(CellModel("1-order-$i", "${i + 1}"))
            list.add(CellModel("2-order-$i", order.menu))
            list.add(CellModel("3-order-$i", "${order.currency} ${NumberFormat.getNumberInstance().format(order.price)}".toUpperCase()))
            list.add(CellModel("4-order-$i", order.quantity.toString()))
            list.add(CellModel("5-order-$i", "${order.currency} ${NumberFormat.getNumberInstance().format(order.total)}".toUpperCase()))
            list.add(CellModel("6-order-$i", if(order.hasPromotion){ "YES" } else { "NO" }))

            lists.add(list)
        }
        return lists
    }

    private fun createRowHeaderList(size: Int): List<RowHeaderModel> {
        val list: MutableList<RowHeaderModel> = ArrayList()
        for (i in 0 until size) { list.add(RowHeaderModel((i + 1).toString())) }
        return list
    }

    fun generateListForTableView(orders: List<OrderData>) {
        columnHeaderModeList = createColumnHeaderModelList()
        cellModelList = createCellModelList(orders)
        rowHeaderModelList = createRowHeaderList(orders.size)
    }
}