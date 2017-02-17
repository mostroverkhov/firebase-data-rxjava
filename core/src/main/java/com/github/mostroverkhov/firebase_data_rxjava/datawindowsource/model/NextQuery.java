package com.github.mostroverkhov.firebase_data_rxjava.datawindowsource.model;

/**
 * Created by Maksym Ostroverkhov on 19.07.2016.
 */

import com.github.mostroverkhov.firebase_data_rxjava.datawindowsource.model.DataQuery;

/**
 * Represents query fir next data window
 */
public class NextQuery {

    private final DataQuery next;

    public NextQuery(DataQuery next) {
        this.next = next;
    }

    public DataQuery getNext() {
        return next;
    }
}
