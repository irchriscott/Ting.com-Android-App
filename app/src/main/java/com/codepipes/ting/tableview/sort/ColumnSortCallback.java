package com.codepipes.ting.tableview.sort;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;
import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

public class ColumnSortCallback extends DiffUtil.Callback {
    @NonNull
    private List<List<ISortableModel>> mOldCellItems;
    @NonNull
    private List<List<ISortableModel>> mNewCellItems;
    private int mColumnPosition;

    public ColumnSortCallback(@NonNull List<List<ISortableModel>> oldCellItems, @NonNull List<List<ISortableModel>>
            newCellItems, int column) {
        this.mOldCellItems = oldCellItems;
        this.mNewCellItems = newCellItems;
        this.mColumnPosition = column;
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
            if (mOldCellItems.get(oldItemPosition).size() > mColumnPosition && mNewCellItems.get
                    (newItemPosition).size() > mColumnPosition) {
                // Compare ids
                String oldId = mOldCellItems.get(oldItemPosition).get(mColumnPosition).getId();
                String newId = mNewCellItems.get(newItemPosition).get(mColumnPosition).getId();
                return oldId.equals(newId);
            }
        }
        return false;
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        // Control for precaution from IndexOutOfBoundsException
        if (mOldCellItems.size() > oldItemPosition && mNewCellItems.size() > newItemPosition) {
            if (mOldCellItems.get(oldItemPosition).size() > mColumnPosition && mNewCellItems.get
                    (newItemPosition).size() > mColumnPosition) {
                // Compare contents
                Object oldContent = mOldCellItems.get(oldItemPosition).get(mColumnPosition)
                        .getContent();
                Object newContent = mNewCellItems.get(newItemPosition).get(mColumnPosition)
                        .getContent();
                return ObjectsCompat.equals(oldContent, newContent);
            }
        }
        return false;
    }
}
