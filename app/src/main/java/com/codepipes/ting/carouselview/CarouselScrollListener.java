package com.codepipes.ting.carouselview;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

public interface CarouselScrollListener {

    void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState, int position);

    void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy);
}
