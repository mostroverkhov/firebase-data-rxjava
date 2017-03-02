package com.github.mostroverkhov.firebase_data_rxjava.rx;


import com.github.mostroverkhov.datawindowsource.callbacks.QueryHandle;
import com.github.mostroverkhov.datawindowsource.model.DataQuery;
import com.github.mostroverkhov.firebase_data_rxjava.rx.DataOnSubscribe.DataWindowOnSubscribe;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Maksym Ostroverkhov on 20.07.2016.
 */
class State<T> {
    private volatile DataQuery dataQuery;
    private final Set<QueryHandle> queryHandles = new HashSet<>();
    private final Object lock = new Object();
    private final Queue<DataWindowOnSubscribe<T>> subscribeFuncs =
            new ConcurrentLinkedQueue<>();
    private final AtomicInteger observablesCount = new AtomicInteger();

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

    public Queue<DataWindowOnSubscribe<T>> getSubscribeFuncs() {
        return subscribeFuncs;
    }

    public AtomicInteger observablesCount() {
        return observablesCount;
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
