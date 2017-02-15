package com.github.mostroverkhov.firebase_rx_data;

import com.github.mostroverkhov.firebase_rx_data.lib.QueryHandle;
import com.github.mostroverkhov.firebase_rx_data.model.DataQuery;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Maksym Ostroverkhov on 20.07.2016.
 */
class State {
    private volatile DataQuery dataQuery;
    private final Set<QueryHandle> queryHandles = new HashSet<>();
    private final Object lock = new Object();

    public State(DataQuery dataQuery) {
        this.dataQuery = dataQuery;
    }

    public DataQuery getNext() {
        return dataQuery;
    }

    public void setNext(DataQuery dataQuery) {
        this.dataQuery = dataQuery;
    }


    public void cancelDataHandles() {
        synchronized (lock) {
            for (QueryHandle queryHandle : queryHandles) {
                queryHandle.cancel();
            }
            queryHandles.clear();
        }
    }

    public boolean hasDataHandles() {
        synchronized (lock) {
            return !queryHandles.isEmpty();
        }
    }

    public void addQueryHandle(QueryHandle queryHandle) {
        synchronized (lock) {
            queryHandles.add(queryHandle);
        }
    }

    public void removeQueryHandle(QueryHandle queryHandle) {
        synchronized (lock) {
            if (queryHandle != null) {
                queryHandles.remove(queryHandle);
            }
        }
    }

}
