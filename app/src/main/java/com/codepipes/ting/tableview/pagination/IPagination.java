package com.codepipes.ting.tableview.pagination;

import androidx.annotation.Nullable;

public interface IPagination {

    /**
     * Loads the next page of the data set to the table view.
     */
    void nextPage();

    /**
     * Loads the previous page of the data set to the table view.
     */
    void previousPage();

    /**
     * Loads the data set of the specified page to the table view.
     *
     * @param page The page to be loaded.
     */
    void goToPage(int page);

    /**
     * Sets the number of items (rows) per page to be displayed in the table view.
     *
     * @param numItems The number of items per page.
     */
    void setItemsPerPage(int numItems);

    /**
     * Sets the OnTableViewPageTurnedListener for this Pagination.
     *
     * @param onTableViewPageTurnedListener The OnTableViewPageTurnedListener.
     */
    void setOnTableViewPageTurnedListener(@Nullable Pagination.OnTableViewPageTurnedListener onTableViewPageTurnedListener);

    /**
     * Removes the OnTableViewPageTurnedListener for this Pagination.
     */
    void removeOnTableViewPageTurnedListener();

    /**
     * @return The current page loaded to the table view.
     */
    int getCurrentPage();

    /**
     * @return The number of items per page loaded to the table view.
     */
    int getItemsPerPage();

    /**
     * @return The number of pages in the pagination.
     */
    int getPageCount();

    /**
     * @return Current pagination state of the table view.
     */
    boolean isPaginated();

}
