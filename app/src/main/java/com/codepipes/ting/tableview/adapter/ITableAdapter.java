package com.codepipes.ting.tableview.adapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import com.codepipes.ting.tableview.ITableView;
import com.codepipes.ting.tableview.adapter.recyclerview.holder.AbstractViewHolder;


public interface ITableAdapter<CH, RH, C> {

    int getColumnHeaderItemViewType(int position);

    int getRowHeaderItemViewType(int position);

    int getCellItemViewType(int position);

    View getCornerView();

    @NonNull
    AbstractViewHolder onCreateCellViewHolder(@NonNull ViewGroup parent, int viewType);

    void onBindCellViewHolder(@NonNull AbstractViewHolder holder, @Nullable C cellItemModel, int columnPosition, int rowPosition);

    @NonNull
    AbstractViewHolder onCreateColumnHeaderViewHolder(@NonNull ViewGroup parent, int viewType);

    void onBindColumnHeaderViewHolder(@NonNull AbstractViewHolder holder, @Nullable CH columnHeaderItemModel, int columnPosition);

    @NonNull
    AbstractViewHolder onCreateRowHeaderViewHolder(@NonNull ViewGroup parent, int viewType);

    void onBindRowHeaderViewHolder(@NonNull AbstractViewHolder holder, @Nullable RH rowHeaderItemModel, int rowPosition);

    @NonNull
    View onCreateCornerView(@NonNull ViewGroup parent);

    ITableView getTableView();

    /**
     * Sets the listener for changes of data set on the TableView.
     *
     * @param listener The AdapterDataSetChangedListener listener.
     */
    void addAdapterDataSetChangedListener(@NonNull AdapterDataSetChangedListener<CH, RH, C> listener);
}
