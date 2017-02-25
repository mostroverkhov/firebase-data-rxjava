package com.github.mostroverkhov.datawindowsource.model;

/**
 * Created by Maksym Ostroverkhov on 19.07.2016.
 */

/**
 * Represents query for next data window
 */
public class NextQuery extends DataItem {

    private final DataQuery next;

    public NextQuery(DataQuery next) {
        this.next = next;
    }

    public DataQuery getNext() {
        return next;
    }
}
