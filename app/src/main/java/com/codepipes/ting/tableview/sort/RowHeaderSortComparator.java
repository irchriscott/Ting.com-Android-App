package com.codepipes.ting.tableview.sort;

import android.support.annotation.NonNull;

import java.util.Comparator;


public class RowHeaderSortComparator extends AbstractSortComparator implements Comparator<ISortableModel> {

    public RowHeaderSortComparator(@NonNull SortState sortState) {
        this.mSortState = sortState;
    }

    @Override
    public int compare(ISortableModel o1, ISortableModel o2) {
        if (mSortState == SortState.DESCENDING) {
            return compareContent(o2.getContent(), o1.getContent());
        } else {
            return compareContent(o1.getContent(), o2.getContent());
        }
    }
}
