package com.codepipes.ting.tableview.sort;

import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;

import java.util.List;


public class RowHeaderSortCallback extends DiffUtil.Callback {

    @NonNull
    private List<ISortableModel> mOldCellItems;
    @NonNull
    private List<ISortableModel> mNewCellItems;

    public RowHeaderSortCallback(@NonNull List<ISortableModel> oldCellItems, @NonNull List<ISortableModel>
            newCellItems) {
        this.mOldCellItems = oldCellItems;
        this.mNewCellItems = newCellItems;
    }

    @Override
    public int getOldListSize() {
        return mOldCellItems.size();
    }

    @Override
    public int getNewListSize() {
        return mNewCellItems.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        // Control for precaution from IndexOutOfBoundsException
        if (mOldCellItems.size() > oldItemPosition && mNewCellItems.size() > newItemPosition) {
            // Compare ids
            String oldId = mOldCellItems.get(oldItemPosition).getId();
            String newId = mNewCellItems.get(newItemPosition).getId();
            return oldId.equals(newId);
        }
        return false;
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        // Control for precaution from IndexOutOfBoundsException
        if (mOldCellItems.size() > oldItemPosition && mNewCellItems.size() > newItemPosition) {
            // Compare contents
            Object oldContent = mOldCellItems.get(oldItemPosition)
                    .getContent();
            Object newContent = mNewCellItems.get(newItemPosition)
                    .getContent();
            return oldContent.equals(newContent);
        }

        return false;
    }

}
