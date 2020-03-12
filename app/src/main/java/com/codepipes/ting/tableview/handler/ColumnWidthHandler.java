package com.codepipes.ting.tableview.handler;

import androidx.annotation.NonNull;

import com.codepipes.ting.tableview.ITableView;


public class ColumnWidthHandler {

    @NonNull
    private ITableView mTableView;

    public ColumnWidthHandler(@NonNull ITableView tableView) {
        mTableView = tableView;
    }

    public void setColumnWidth(int columnPosition, int width) {

        // Firstly set the column header cache map
        mTableView.getColumnHeaderLayoutManager().setCacheWidth(columnPosition, width);

        // Set each of cell items that is located on the column position
        mTableView.getCellLayoutManager().setCacheWidth(columnPosition, width);
    }

}
