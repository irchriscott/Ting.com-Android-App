package com.codepipes.ting.tableview.sort;

import android.support.annotation.NonNull;

import java.util.Comparator;
import java.util.List;

public class ColumnForRowHeaderSortComparator implements Comparator<ISortableModel> {
    @NonNull
    private List<ISortableModel> mRowHeaderList;
    @NonNull
    private List<List<ISortableModel>> mReferenceList;
    private int column;
    @NonNull
    private SortState mSortState;
    @NonNull
    private ColumnSortComparator mColumnSortComparator;

    public ColumnForRowHeaderSortComparator(@NonNull List<ISortableModel> rowHeader,
                                            @NonNull List<List<ISortableModel>> referenceList,
                                            int column,
                                            @NonNull SortState sortState) {
        this.mRowHeaderList = rowHeader;
        this.mReferenceList = referenceList;
        this.column = column;
        this.mSortState = sortState;
        this.mColumnSortComparator = new ColumnSortComparator(column, sortState);
    }

    @Override
    public int compare(ISortableModel o, ISortableModel t1) {
        Object o1 = mReferenceList.get(this.mRowHeaderList.indexOf(o)).get(column).getContent();
        Object o2 = mReferenceList.get(this.mRowHeaderList.indexOf(t1)).get(column).getContent();
        if (mSortState == SortState.DESCENDING) {
            return mColumnSortComparator.compareContent(o2, o1);
        } else {
            return mColumnSortComparator.compareContent(o1, o2);
        }
    }
}
