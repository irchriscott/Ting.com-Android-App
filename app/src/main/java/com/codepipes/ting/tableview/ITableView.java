package com.codepipes.ting.tableview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.view.ViewGroup;

import com.codepipes.ting.tableview.adapter.AbstractTableAdapter;
import com.codepipes.ting.tableview.adapter.recyclerview.CellRecyclerView;
import com.codepipes.ting.tableview.filter.Filter;
import com.codepipes.ting.tableview.handler.ColumnSortHandler;
import com.codepipes.ting.tableview.handler.FilterHandler;
import com.codepipes.ting.tableview.handler.ScrollHandler;
import com.codepipes.ting.tableview.handler.SelectionHandler;
import com.codepipes.ting.tableview.handler.VisibilityHandler;
import com.codepipes.ting.tableview.layoutmanager.CellLayoutManager;
import com.codepipes.ting.tableview.layoutmanager.ColumnHeaderLayoutManager;
import com.codepipes.ting.tableview.listener.ITableViewListener;
import com.codepipes.ting.tableview.listener.scroll.HorizontalRecyclerViewListener;
import com.codepipes.ting.tableview.listener.scroll.VerticalRecyclerViewListener;
import com.codepipes.ting.tableview.sort.SortState;

public interface ITableView {

    void addView(View child, ViewGroup.LayoutParams params);

    boolean hasFixedWidth();

    boolean isIgnoreSelectionColors();

    boolean isShowHorizontalSeparators();

    boolean isShowVerticalSeparators();

    boolean isAllowClickInsideCell();

    boolean isSortable();

    @NonNull
    Context getContext();

    @NonNull
    CellRecyclerView getCellRecyclerView();

    @NonNull
    CellRecyclerView getColumnHeaderRecyclerView();

    @NonNull
    CellRecyclerView getRowHeaderRecyclerView();

    @NonNull
    ColumnHeaderLayoutManager getColumnHeaderLayoutManager();

    @NonNull
    CellLayoutManager getCellLayoutManager();

    @NonNull
    LinearLayoutManager getRowHeaderLayoutManager();

    @NonNull
    HorizontalRecyclerViewListener getHorizontalRecyclerViewListener();

    @NonNull
    VerticalRecyclerViewListener getVerticalRecyclerViewListener();

    @Nullable
    ITableViewListener getTableViewListener();

    @NonNull
    SelectionHandler getSelectionHandler();

    @Nullable
    ColumnSortHandler getColumnSortHandler();

    @NonNull
    VisibilityHandler getVisibilityHandler();

    @NonNull
    DividerItemDecoration getHorizontalItemDecoration();

    @NonNull
    DividerItemDecoration getVerticalItemDecoration();

    @NonNull
    SortState getSortingStatus(int column);

    @Nullable
    SortState getRowHeaderSortingStatus();

    void scrollToColumnPosition(int column);

    void scrollToColumnPosition(int column, int offset);

    void scrollToRowPosition(int row);

    void scrollToRowPosition(int row, int offset);

    void showRow(int row);

    void hideRow(int row);

    boolean isRowVisible(int row);

    void showAllHiddenRows();

    void clearHiddenRowList();

    void showColumn(int column);

    void hideColumn(int column);

    boolean isColumnVisible(int column);

    void showAllHiddenColumns();

    void clearHiddenColumnList();

    int getShadowColor();

    int getSelectedColor();

    int getUnSelectedColor();

    int getSeparatorColor();

    void sortColumn(int columnPosition, @NonNull SortState sortState);

    void sortRowHeader(@NonNull SortState sortState);

    void remeasureColumnWidth(int column);

    int getRowHeaderWidth();

    void setRowHeaderWidth(int rowHeaderWidth);

    @Nullable
    AbstractTableAdapter getAdapter();

    /**
     * Filters the whole table using the provided Filter object which supports multiple filters.
     *
     * @param filter The filter object.
     */
    void filter(@NonNull Filter filter);

    /**
     * Retrieves the FilterHandler of the TableView.
     *
     * @return The FilterHandler of the TableView.
     */
    @Nullable
    FilterHandler getFilterHandler();

    /**
     * Retrieves the ScrollHandler of the TableView.
     *
     * @return The ScrollHandler of the TableView.
     */
    @NonNull
    ScrollHandler getScrollHandler();
}
