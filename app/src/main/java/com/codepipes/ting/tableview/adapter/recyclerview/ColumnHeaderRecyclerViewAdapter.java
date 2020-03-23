package com.codepipes.ting.tableview.adapter.recyclerview;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.ViewGroup;

import com.codepipes.ting.tableview.ITableView;
import com.codepipes.ting.tableview.adapter.ITableAdapter;
import com.codepipes.ting.tableview.adapter.recyclerview.holder.AbstractSorterViewHolder;
import com.codepipes.ting.tableview.adapter.recyclerview.holder.AbstractViewHolder;
import com.codepipes.ting.tableview.adapter.recyclerview.holder.AbstractViewHolder.SelectionState;
import com.codepipes.ting.tableview.sort.ColumnSortHelper;
import com.codepipes.ting.tableview.sort.SortState;

import java.util.List;

public class ColumnHeaderRecyclerViewAdapter<CH> extends AbstractRecyclerViewAdapter<CH> {

    @NonNull
    private ITableAdapter mTableAdapter;
    private ITableView mTableView;
    private ColumnSortHelper mColumnSortHelper;

    public ColumnHeaderRecyclerViewAdapter(@NonNull Context context, @Nullable List<CH> itemList, @NonNull ITableAdapter
            tableAdapter) {
        super(context, itemList);
        this.mTableAdapter = tableAdapter;
        this.mTableView = tableAdapter.getTableView();
    }

    @NonNull
    @Override
    public AbstractViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return mTableAdapter.onCreateColumnHeaderViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull AbstractViewHolder holder, int position) {
        mTableAdapter.onBindColumnHeaderViewHolder(holder, getItem(position), position);
    }

    @Override
    public int getItemViewType(int position) {
        return mTableAdapter.getColumnHeaderItemViewType(position);
    }

    @Override
    public void onViewAttachedToWindow(@NonNull AbstractViewHolder viewHolder) {
        super.onViewAttachedToWindow(viewHolder);

        SelectionState selectionState = mTableView.getSelectionHandler().getColumnSelectionState
                (viewHolder.getAdapterPosition());

        // Control to ignore selection color
        if (!mTableView.isIgnoreSelectionColors()) {

            // Change background color of the view considering it's selected state
            mTableView.getSelectionHandler().changeColumnBackgroundColorBySelectionStatus
                    (viewHolder, selectionState);
        }

        // Change selection status
        viewHolder.setSelected(selectionState);

        // Control whether the TableView is sortable or not.
        if (mTableView.isSortable()) {
            if (viewHolder instanceof AbstractSorterViewHolder) {
                // Get its sorting state
                SortState state = getColumnSortHelper().getSortingStatus(viewHolder
                        .getAdapterPosition());
                // Fire onSortingStatusChanged
                ((AbstractSorterViewHolder) viewHolder).onSortingStatusChanged(state);
            }
        }
    }

    @NonNull
    public ColumnSortHelper getColumnSortHelper() {
        if (mColumnSortHelper == null) {
            // It helps to store sorting state of column headers
            this.mColumnSortHelper = new ColumnSortHelper(mTableView.getColumnHeaderLayoutManager
                    ());
        }
        return mColumnSortHelper;
    }
}
