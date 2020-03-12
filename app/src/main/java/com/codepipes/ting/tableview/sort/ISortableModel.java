package com.codepipes.ting.tableview.sort;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public interface ISortableModel {
    @NonNull
    String getId();

    @Nullable
    Object getContent();
}
