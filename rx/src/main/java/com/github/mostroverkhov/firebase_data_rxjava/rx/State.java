package com.github.mostroverkhov.firebase_data_rxjava.rx;


import com.github.mostroverkhov.datawindowsource.callbacks.QueryHandle;
import com.github.mostroverkhov.datawindowsource.model.DataQuery;
import com.github.mostroverkhov.firebase_data_rxjava.rx.DataOnSubscribe.DataWindowOnSubscribe;

import java.util.Collections;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Maksym Ostroverkhov on 20.07.2016.
 */
class State<T> {
    private final AtomicInteger actionsCounter = new AtomicInteger();
    private final Queue<Runnable> actions = new ConcurrentLinkedQueue<>();

    private volatile DataQuery dataQuery;
    private final Set<QueryHandle> queryHandles = Collections.newSetFromMap(
            new ConcurrentHashMap<QueryHandle, Boolean>());
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

    public Queue<DataWindowOnSubscribe<T>> getSubscribeFuncs() {
        return subscribeFuncs;
    }

    public AtomicInteger observablesCount() {
        return observablesCount;
    }

    public void cancelDataHandles() {
        actions.offer(new ClearQueries());
        drain();
    }

    public void addQueryHandle(final QueryHandle queryHandle) {
        actions.offer(new AddQuery(queryHandle));
        drain();
    }

    public void removeQueryHandle(final QueryHandle queryHandle) {
        actions.offer(new RemoveQuery(queryHandle));
        drain();
    }

    private void drain() {
        if (actionsCounter.getAndIncrement() == 0) {
            do {
                Runnable action = actions.poll();
                action.run();
            } while (actionsCounter.decrementAndGet() != 0);
        }
    }

    private class RemoveQuery implements Runnable {

        private final QueryHandle queryHandle;

        public RemoveQuery(QueryHandle queryHandle) {
            this.queryHandle = queryHandle;
        }

        @Override
        public void run() {
            if (queryHandle != null) {
                queryHandles.remove(queryHandle);
            }
        }
    }

    private class AddQuery implements Runnable {
        private final QueryHandle queryHandle;

        public AddQuery(QueryHandle queryHandle) {
            this.queryHandle = queryHandle;
        }

        @Override
        public void run() {
            queryHandles.add(queryHandle);
        }
    }

    private class ClearQueries implements Runnable {
        @Override
        public void run() {
            for (QueryHandle queryHandle : queryHandles) {
                queryHandle.cancel();
            }
            queryHandles.clear();
        }
    }
}
