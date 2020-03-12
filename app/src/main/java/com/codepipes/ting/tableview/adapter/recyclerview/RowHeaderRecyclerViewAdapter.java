package com.codepipes.ting.tableview.adapter.recyclerview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.ViewGroup;

import com.codepipes.ting.tableview.ITableView;
import com.codepipes.ting.tableview.adapter.ITableAdapter;
import com.codepipes.ting.tableview.adapter.recyclerview.holder.AbstractViewHolder;
import com.codepipes.ting.tableview.adapter.recyclerview.holder.AbstractViewHolder.SelectionState;
import com.codepipes.ting.tableview.sort.RowHeaderSortHelper;

import java.util.List;

public class RowHeaderRecyclerViewAdapter<RH> extends AbstractRecyclerViewAdapter<RH> {

    @NonNull
    private ITableAdapter mTableAdapter;
    private ITableView mTableView;
    private RowHeaderSortHelper mRowHeaderSortHelper;

    public RowHeaderRecyclerViewAdapter(@NonNull Context context, @Nullable List<RH> itemList, @NonNull ITableAdapter
            tableAdapter) {
        super(context, itemList);
        this.mTableAdapter = tableAdapter;
        this.mTableView = tableAdapter.getTableView();
    }

    @NonNull
    @Override
    public AbstractViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return mTableAdapter.onCreateRowHeaderViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull AbstractViewHolder holder, int position) {
        mTableAdapter.onBindRowHeaderViewHolder(holder, getItem(position), position);
    }

    @Override
    public int getItemViewType(int position) {
        return mTableAdapter.getRowHeaderItemViewType(position);
    }

    @Override
    public void onViewAttachedToWindow(@NonNull AbstractViewHolder viewHolder) {
        super.onViewAttachedToWindow(viewHolder);

        SelectionState selectionState = mTableView.getSelectionHandler().getRowSelectionState
                (viewHolder.getAdapterPosition());

        // Control to ignore selection color
        if (!mTableView.isIgnoreSelectionColors()) {

            // Change background color of the view considering it's selected state
            mTableView.getSelectionHandler().changeRowBackgroundColorBySelectionStatus
                    (viewHolder, selectionState);
        }

        // Change selection status
        viewHolder.setSelected(selectionState);
    }

    @NonNull
    public RowHeaderSortHelper getRowHeaderSortHelper() {
        if (mRowHeaderSortHelper == null) {
            // It helps to store sorting state of row headers
            this.mRowHeaderSortHelper = new RowHeaderSortHelper();
        }
        return mRowHeaderSortHelper;
    }
}
