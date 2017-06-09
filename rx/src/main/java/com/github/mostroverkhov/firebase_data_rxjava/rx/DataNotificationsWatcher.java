package com.github.mostroverkhov.firebase_data_rxjava.rx;


import com.github.mostroverkhov.datawindowsource.NotificationsHandle;
import com.github.mostroverkhov.datawindowsource.callbacks.QueryHandle;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by Maksym Ostroverkhov on 17.07.2016.
 */

/**
 * Utility class for releasing internal subscriptions for firebase database in case client decided
 * not to subscribe to data window notifications
 */
class DataNotificationsWatcher {

    private final ConcurrentMap<Object, HandleAction> readResults = new ConcurrentHashMap<>();

    public void releaseNotifications(Object item) {
        HandleAction action = readResults.remove(item);
        if (action != null) {
            action.perform();
        }
    }

    public void addReadResult(Object item, NotificationsHandle handle) {
        readResults.put(item, new NotificationsHandleAction(handle));
    }

    public void addReadResult(Object item, QueryHandle handle) {
        readResults.put(item, new QueryHandleAction(handle));
    }

    private interface HandleAction {

        void perform();
    }

    private static class NotificationsHandleAction implements HandleAction {

        private final NotificationsHandle handle;

        public NotificationsHandleAction(NotificationsHandle handle) {
            this.handle = handle;
        }

        @Override
        public void perform() {
            handle.stopListenToNotifications();
        }
    }

    private static class QueryHandleAction implements HandleAction {

        private final QueryHandle handle;

        public QueryHandleAction(QueryHandle handle) {
            this.handle = handle;
        }

        @Override
        public void perform() {
            handle.cancel();
        }
    }

}
