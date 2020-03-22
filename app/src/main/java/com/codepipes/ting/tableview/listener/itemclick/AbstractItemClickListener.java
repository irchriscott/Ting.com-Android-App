package com.codepipes.ting.tableview.listener.itemclick;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.codepipes.ting.tableview.ITableView;
import com.codepipes.ting.tableview.adapter.recyclerview.CellRecyclerView;
import com.codepipes.ting.tableview.handler.SelectionHandler;
import com.codepipes.ting.tableview.listener.ITableViewListener;


public abstract class AbstractItemClickListener implements RecyclerView.OnItemTouchListener {

    private ITableViewListener mListener;
    @NonNull
    protected GestureDetector mGestureDetector;
    @NonNull
    protected CellRecyclerView mRecyclerView;
    @NonNull
    protected SelectionHandler mSelectionHandler;
    @NonNull
    protected ITableView mTableView;

    public AbstractItemClickListener(@NonNull CellRecyclerView recyclerView, @NonNull ITableView tableView) {
        this.mRecyclerView = recyclerView;
        this.mTableView = tableView;
        this.mSelectionHandler = tableView.getSelectionHandler();

        mGestureDetector = new GestureDetector(mRecyclerView.getContext(), new GestureDetector
                .SimpleOnGestureListener() {

            @Nullable
            MotionEvent start;

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                return clickAction(mRecyclerView, e);
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                return doubleClickAction(e);
            }

            @Override
            public boolean onDown(MotionEvent e) {
                start = e;
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                // Check distance to prevent scroll to trigger the event
                if (start != null && Math.abs(start.getRawX() - e.getRawX()) < 20 && Math.abs
                        (start.getRawY() - e.getRawY()) < 20) {
                    longPressAction(e);
                }
            }
        });
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView view, @NonNull MotionEvent e) {
        mGestureDetector.onTouchEvent(e);
        // Return false intentionally
        return false;
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView view, @NonNull MotionEvent motionEvent) {
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }

    @NonNull
    protected ITableViewListener getTableViewListener() {
        if (mListener == null) {
            mListener = mTableView.getTableViewListener();
        }
        return mListener;
    }

    abstract protected boolean clickAction(@NonNull RecyclerView view, @NonNull MotionEvent e);

    abstract protected void longPressAction(@NonNull MotionEvent e);

    abstract protected boolean doubleClickAction(MotionEvent e);
}
