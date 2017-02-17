package com.github.mostroverkhov.firebase_data_rxjava.datawindowsource.model;

/**
 * Created by Maksym Ostroverkhov on 19.07.2016.
 */

/**
 * Contains query for next data window, together with item count for current one
 */
public class NotificationResult extends NextQuery {

    private final long itemsCount;

    public NotificationResult(DataQuery nextQuery, long itemsCount) {
        super(nextQuery);
        this.itemsCount = itemsCount;
    }

    public long getItemsCount() {
        return itemsCount;
    }
}
