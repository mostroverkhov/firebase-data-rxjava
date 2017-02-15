package com.github.mostroverkhov.firebase_rx_data.model;

/**
 * Created by Maksym Ostroverkhov on 19.07.2016.
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
