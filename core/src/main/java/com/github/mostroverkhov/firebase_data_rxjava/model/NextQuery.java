package com.github.mostroverkhov.firebase_data_rxjava.model;

/**
 * Created by Maksym Ostroverkhov on 19.07.2016.
 */

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
