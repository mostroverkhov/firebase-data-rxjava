package com.github.mostroverkhov.firebase_rx_data.model;

import com.github.mostroverkhov.firebase_rx_data.model.NextQuery;
import com.github.mostroverkhov.firebase_rx_data.model.DataQuery;

/**
 * Created by Maksym Ostroverkhov on 19.07.2016.
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
