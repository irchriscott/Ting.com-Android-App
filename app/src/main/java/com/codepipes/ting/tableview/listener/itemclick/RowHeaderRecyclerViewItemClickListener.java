package com.codepipes.ting.tableview.listener.itemclick;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;

import com.codepipes.ting.tableview.ITableView;
import com.codepipes.ting.tableview.adapter.recyclerview.CellRecyclerView;
import com.codepipes.ting.tableview.adapter.recyclerview.holder.AbstractViewHolder;

public class RowHeaderRecyclerViewItemClickListener extends AbstractItemClickListener {

    public RowHeaderRecyclerViewItemClickListener(@NonNull CellRecyclerView recyclerView, @NonNull ITableView
            tableView) {
        super(recyclerView, tableView);
    }

    @Override
    protected boolean clickAction(@NonNull RecyclerView view, @NonNull MotionEvent e) {
        // Get interacted view from x,y coordinate.
        View childView = view.findChildViewUnder(e.getX(), e.getY());

        if (childView != null) {
            // Find the view holder
            AbstractViewHolder holder = (AbstractViewHolder) mRecyclerView.getChildViewHolder
                    (childView);

            int row = holder.getAdapterPosition();

            // Control to ignore selection color
            if (!mTableView.isIgnoreSelectionColors()) {
                mSelectionHandler.setSelectedRowPosition(holder, row);
            }

            // Call ITableView listener for item click
            getTableViewListener().onRowHeaderClicked(holder, row);
            return true;
        }
        return false;
    }

    protected void longPressAction(@NonNull MotionEvent e) {
        // Consume the action for the time when the recyclerView is scrolling.
        if (mRecyclerView.getScrollState() != RecyclerView.SCROLL_STATE_IDLE) {
            return;
        }

        // Get interacted view from x,y coordinate.
        View child = mRecyclerView.findChildViewUnder(e.getX(), e.getY());

        if (child != null) {
            // Find the view holder
            RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(child);

            // Call ITableView listener for long click
            getTableViewListener().onRowHeaderLongPressed(holder, holder.getAdapterPosition());
        }
    }

    @Override
    protected boolean doubleClickAction(MotionEvent e) {
        // Get interacted view from x,y coordinate.
        View childView = mRecyclerView.findChildViewUnder(e.getX(), e.getY());

        if (childView != null) {
            try {
                // Find the view holder
                AbstractViewHolder holder = (AbstractViewHolder) mRecyclerView.getChildViewHolder
                        (childView);

                int row = holder.getAdapterPosition();

                // Control to ignore selection color
                if (!mTableView.isIgnoreSelectionColors()) {
                    mSelectionHandler.setSelectedRowPosition(holder, row);
                }

                // Call ITableView listener for item click
                getTableViewListener().onRowHeaderDoubleClicked(holder, row);
                return true;
            } catch (Exception ex) { return false; }
        }
        return false;
    }
}
