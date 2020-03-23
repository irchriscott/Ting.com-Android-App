package com.codepipes.ting.tableview.filter;

import androidx.annotation.NonNull;

public interface IFilterableModel {

    /**
     * Filterable query string for this object.
     *
     * @return query string for this object to be used in filtering.
     */
    @NonNull
    String getFilterableKeyword();
}
