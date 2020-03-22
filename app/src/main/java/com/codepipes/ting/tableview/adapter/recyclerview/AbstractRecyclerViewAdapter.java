package com.codepipes.ting.tableview.adapter.recyclerview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;

import com.codepipes.ting.tableview.adapter.recyclerview.holder.AbstractViewHolder;

import java.util.ArrayList;
import java.util.List;


public abstract class AbstractRecyclerViewAdapter<T> extends RecyclerView.Adapter<AbstractViewHolder> {

    @NonNull
    protected List<T> mItemList;

    @NonNull
    protected Context mContext;

    public AbstractRecyclerViewAdapter(@NonNull Context context) {
        this(context, null);
    }

    public AbstractRecyclerViewAdapter(@NonNull Context context, @Nullable List<T> itemList) {
        mContext = context;

        if (itemList == null) {
            mItemList = new ArrayList<>();
        } else {
            setItems(itemList);
        }
    }

    @Override
    public int getItemCount() {
        return mItemList.size();
    }

    @NonNull
    public List<T> getItems() {
        return mItemList;
    }

    public void setItems(@NonNull List<T> itemList) {
        mItemList = new ArrayList<>(itemList);

        this.notifyDataSetChanged();
    }

    public void setItems(@NonNull List<T> itemList, boolean notifyDataSet) {
        mItemList = new ArrayList<>(itemList);

        if (notifyDataSet) {
            this.notifyDataSetChanged();
        }
    }

    @Nullable
    public T getItem(int position) {
        if (mItemList.isEmpty() || position < 0 || position >= mItemList.size()) {
            return null;
        }
        return mItemList.get(position);
    }

    public void deleteItem(int position) {
        if (position != RecyclerView.NO_POSITION) {
            mItemList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void deleteItemRange(int positionStart, int itemCount) {
        for (int i = positionStart + itemCount - 1; i >= positionStart; i--) {
            if (i != RecyclerView.NO_POSITION) {
                mItemList.remove(i);
            }
        }

        notifyItemRangeRemoved(positionStart, itemCount);
    }

    public void addItem(int position, @Nullable T item) {
        if (position != RecyclerView.NO_POSITION && item != null) {
            mItemList.add(position, item);
            notifyItemInserted(position);
        }
    }

    public void addItemRange(int positionStart, @Nullable List<T> items) {
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                mItemList.add((i + positionStart), items.get(i));
            }

            notifyItemRangeInserted(positionStart, items.size());
        }
    }

    public void changeItem(int position, @Nullable T item) {
        if (position != RecyclerView.NO_POSITION && item != null) {
            mItemList.set(position, item);
            notifyItemChanged(position);
        }
    }

    public void changeItemRange(int positionStart, @Nullable List<T> items) {
        if (items != null && mItemList.size() > positionStart + items.size()) {
            for (int i = 0; i < items.size(); i++) {
                mItemList.set(i + positionStart, items.get(i));
            }
            notifyItemRangeChanged(positionStart, items.size());
        }
    }

    @Override
    public int getItemViewType(int position) {
        return 1;
    }
}
