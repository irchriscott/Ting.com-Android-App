package com.codepipes.ting.tableview.filter;

import android.support.annotation.NonNull;

public class FilterItem {

    @NonNull
    private FilterType filterType;
    @NonNull
    private String filter;
    private int column;

    public FilterItem(@NonNull FilterType type, int column, @NonNull String filter) {
        this.filterType = type;
        this.column = column;
        this.filter = filter;
    }

    @NonNull
    public FilterType getFilterType() {
        return filterType;
    }

    @NonNull
    public String getFilter() {
        return filter;
    }

    public int getColumn() {
        return column;
    }
}