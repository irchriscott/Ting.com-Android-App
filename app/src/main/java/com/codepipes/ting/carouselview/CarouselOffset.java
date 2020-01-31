package com.codepipes.ting.carouselview;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewTreeObserver;

class CarouselOffset {

    void init(final RecyclerView recyclerView, final View view, final int spacing, final boolean isOffsetStart) {
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (recyclerView.getItemDecorationCount() > 0) {
                    recyclerView.removeItemDecorationAt(0);
                }
                if (isOffsetStart) {
                    recyclerView.addItemDecoration(new CarouselItemDecoration(view.getWidth(), spacing), 0);
                } else {
                    recyclerView.addItemDecoration(new CarouselItemDecoration(0, spacing), 0);
                }
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }
}
