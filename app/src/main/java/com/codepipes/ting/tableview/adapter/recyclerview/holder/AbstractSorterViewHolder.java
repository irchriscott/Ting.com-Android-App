package com.codepipes.ting.tableview.adapter.recyclerview.holder;

import android.support.annotation.NonNull;
import android.view.View;

import com.codepipes.ting.tableview.sort.SortState;

public class AbstractSorterViewHolder extends AbstractViewHolder {
    @NonNull
    private SortState mSortState = SortState.UNSORTED;

    public AbstractSorterViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public void onSortingStatusChanged(@NonNull SortState pSortState) {
        this.mSortState = pSortState;
    }

    @NonNull
    public SortState getSortState() {
        return mSortState;
    }
}
