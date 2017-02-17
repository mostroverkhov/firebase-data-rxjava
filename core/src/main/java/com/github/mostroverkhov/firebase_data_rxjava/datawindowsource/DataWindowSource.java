package com.github.mostroverkhov.firebase_data_rxjava.datawindowsource;

import com.github.mostroverkhov.firebase_data_rxjava.datawindowsource.callbacks.NotificationCallback;
import com.github.mostroverkhov.firebase_data_rxjava.datawindowsource.callbacks.QueryHandle;
import com.github.mostroverkhov.firebase_data_rxjava.datawindowsource.model.DataQuery;
import com.github.mostroverkhov.firebase_data_rxjava.datawindowsource.model.DataWindowChangeEvent;
import com.github.mostroverkhov.firebase_data_rxjava.datawindowsource.model.DataWindowResult;
import com.github.mostroverkhov.firebase_data_rxjava.datawindowsource.callbacks.DataCallback;
import com.github.mostroverkhov.firebase_data_rxjava.datawindowsource.callbacks.NextWindowCallback;
import com.github.mostroverkhov.firebase_data_rxjava.datawindowsource.model.DataWindowAndNotificationResult;
import com.github.mostroverkhov.firebase_data_rxjava.datawindowsource.model.NotificationResult;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Maksym Ostroverkhov on 09.07.2016.
 */

/**
 * Provides windows into data, notifications for changes in those windows
 *
 * @param <T> type of item in data window
 */
public class DataWindowSource<T> {

    private static final QueryHandle NOOP_DATA_HANDLE = new QueryHandle() {
        @Override
        public void cancel() {
            /*noop*/
        }
    };
    private final Object lock = new Object();

    public DataWindowSource() {
    }

    /**
     * Returns data change notifications to client provided callback.
     *
     * @param nextWindowCallback next data query callback
     * @param notificationCallback     data window change events callback
     * @return handle used to unsubscribe from child change notifications
     */
    public QueryHandle next(final DataQuery dataQuery,
                            final Class<T> itemType,
                            final NextWindowCallback nextWindowCallback,
                            final NotificationCallback<T> notificationCallback) {

        final int windowSize = dataQuery.getWindowSize();
        int windowSizeAndNextFirst = windowSize + 1;
        final DataQuery.OrderDirection orderDir = dataQuery.getOrderDir();
        DataQuery.OrderBy orderBy = dataQuery.getOrderBy();
        String orderByChildKey = dataQuery.orderByChildKey();

        final Query dataDbRef = buildQuery(dataQuery,
                windowSizeAndNextFirst,
                orderDir,
                orderBy,
                orderByChildKey);

        final ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                DataQuery next = orderDir == DataQuery.OrderDirection.ASC
                        ? nextAsc(dataQuery, dataSnapshot, windowSize)
                        : nextDesc(dataQuery, dataSnapshot, windowSize);

                nextWindowCallback.onData(new NotificationResult(next, dataSnapshot.getChildrenCount()));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                nextWindowCallback.onError(databaseError);
            }
        };
        dataDbRef.addListenerForSingleValueEvent(valueEventListener);

        final Query childDbRef = buildQuery(dataQuery, windowSize, orderDir, orderBy, orderByChildKey);
        final ChildEventListener childListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                onEvent(dataSnapshot,
                        s,
                        itemType,
                        notificationCallback,
                        DataWindowChangeEvent.Kind.ADDED);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                onEvent(dataSnapshot,
                        s,
                        itemType,
                        notificationCallback,
                        DataWindowChangeEvent.Kind.CHANGED);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                onEvent(dataSnapshot,
                        "",
                        itemType,
                        notificationCallback,
                        DataWindowChangeEvent.Kind.REMOVED);
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                onEvent(dataSnapshot,
                        s,
                        itemType,
                        notificationCallback,
                        DataWindowChangeEvent.Kind.MOVED);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                notificationCallback.onError(databaseError);
            }
        };
        childDbRef.addChildEventListener(childListener);

        return new QueryHandle() {
            @Override
            public void cancel() {
                childDbRef.removeEventListener(childListener);
                dataDbRef.removeEventListener(valueEventListener);
            }
        };
    }

    private void onEvent(DataSnapshot dataSnapshot,
                         String prevChildName,
                         Class<T> itemType, NotificationCallback<T> notificationCallback,
                         DataWindowChangeEvent.Kind kind) {

        T value = dataSnapshot.getValue(itemType);
        if (value != null) {
            notificationCallback.onChildChanged(new DataWindowChangeEvent<>(
                    value,
                    kind,
                    prevChildName));
        }
    }

    /**
     * @return data w/o change notifications to client provided callback
     */
    public QueryHandle next(final DataQuery dataQuery,
                            final Class<T> dataItemType,
                            final DataCallback<T, DataWindowResult<T>> dataCallback) {


        if (dataQuery.isLast()) {
            dataCallback.onData(new DataWindowResult<>(Collections.<T>emptyList(), dataQuery));
            return NOOP_DATA_HANDLE;
        }
        final int windowSize = dataQuery.getWindowSize();
        int windowSizeAndNextFirst = windowSize + 1;
        final DataQuery.OrderDirection orderDir = dataQuery.getOrderDir();
        DataQuery.OrderBy orderBy = dataQuery.getOrderBy();
        String orderByChildKey = dataQuery.orderByChildKey();

        final Query dataDbRef = buildQuery(dataQuery, windowSizeAndNextFirst, orderDir, orderBy, orderByChildKey);

        final ValueEventListener valueListener = new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Pair<List<KeyValue<T>>, DataQuery> pair = orderDir == DataQuery.OrderDirection.ASC
                        ? onDataChangeAsc(dataQuery, dataSnapshot, dataItemType, windowSize)
                        : onDataChangeDesc(dataQuery, dataSnapshot, dataItemType, windowSize);

                List<T> data = toItemsList(pair.getLeft());
                DataQuery nextQuery = pair.getRight();
                dataCallback.onData(new DataWindowResult<>(data, nextQuery));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                dataCallback.onError(databaseError);
            }
        };
        dataDbRef.addListenerForSingleValueEvent(valueListener);

        return new QueryHandle() {
            @Override
            public void cancel() {
                dataDbRef.removeEventListener(valueListener);
            }
        };
    }

    /**
     * Returns both data and data change notifications to client provided callback. Notifications are
     * controlled by dataCallback's NotificationsHandle.
     *
     * @param dataQuery            query for data window
     * @param dataItemType         type of item in data window
     * @param dataCallback         callback for data window itself and handle to data window change notifications
     * @param notificationCallback callback for data window notifications
     * @return token to cancel data query in progress
     */
    public QueryHandle next(final DataQuery dataQuery,
                            final Class<T> dataItemType,
                            final DataCallback<T, DataWindowAndNotificationResult<T>> dataCallback,
                            NotificationCallback<T> notificationCallback) {

        final NextSignals nextSignals = new NextSignals();

        if (dataQuery.isLast()) {
            dataCallback.onData(new DataWindowAndNotificationResult<>(Collections.<T>emptyList(), dataQuery));
            return NOOP_DATA_HANDLE;
        }
        final int windowSize = dataQuery.getWindowSize();
        /*take one additional item to have next window start key*/
        int windowSizeAndNextFirst = windowSize + 1;
        final DataQuery.OrderDirection orderDir = dataQuery.getOrderDir();
        DataQuery.OrderBy orderBy = dataQuery.getOrderBy();
        String orderByChildKey = dataQuery.orderByChildKey();

        final Query childDbRef = buildQuery(dataQuery, windowSize, orderDir, orderBy, orderByChildKey);
        final Query dataDbRef = buildQuery(dataQuery, windowSizeAndNextFirst, orderDir, orderBy, orderByChildKey);

        /*listens and caches child changed events, and once asked, dispatches those events to listener
        * Field is used to cancel notifications while data query is in progress, but once data is queried,
        * unsubscription from child change events should be performed by NotificationHandle*/
        final DelegatingChildEventListener<T> delegatingListener = new DelegatingChildEventListener<>(
                notificationCallback, dataItemType);
        /*used to listen for child data notifications*/
        final ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Pair<List<KeyValue<T>>, DataQuery> pair = orderDir == DataQuery.OrderDirection.ASC
                        ? onDataChangeAsc(dataQuery, dataSnapshot, dataItemType, windowSize)
                        : onDataChangeDesc(dataQuery, dataSnapshot, dataItemType, windowSize);

                List<KeyValue<T>> keyValues = pair.getLeft();
                /*remove items obtained from data query*/
                delegatingListener.removeEvents(keyValues);
                List<T> values = toItemsList(pair.getLeft());
                DataQuery query = pair.getRight();
                DataWindowAndNotificationResult<T> result = new DataWindowAndNotificationResult<>(
                        values,
                        query,
                        new NotificationsHandle() {

                            @Override
                            public void startListenToNotifications() {
                                /*dispatch current cached and future events to listener*/
                                delegatingListener.dispatchChildEvents();
                            }

                            @Override
                            public void stopListenToNotifications() {
                                /*stop listen for child events*/
                                childDbRef.removeEventListener(delegatingListener);
                            }
                        });
                /*deliver if was not cancelled while processing onDataChange*/
                synchronized (lock) {
                    if (!nextSignals.isCancelled()) {
                        dataCallback.onData(result);
                    }
                    /*clean up query state*/
                    nextSignals.setDataQueryInProgress(false);
                    nextSignals.setCancelled(false);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                dataCallback.onError(databaseError);
            }
        };

        synchronized (lock) {
            childDbRef.addChildEventListener(delegatingListener);
            dataDbRef.addListenerForSingleValueEvent(valueEventListener);
            nextSignals.setDataQueryInProgress(true);
        }

        return new QueryHandle() {
            @Override
            public void cancel() {
                synchronized (lock) {
                    /*unsubscribe only while data is in progress, otherwise clients should use NotificationHandle
                     from DataWindowAndNotificationResult*/
                    if (nextSignals.isDataQueryInProgress()) {
                        nextSignals.setCancelled(true);
                        nextSignals.setDataQueryInProgress(false);
                        childDbRef.removeEventListener(delegatingListener);
                        dataDbRef.removeEventListener(valueEventListener);
                    }
                }
            }
        };
    }

    private static <T> List<T> toItemsList(List<KeyValue<T>> items) {
        ArrayList<T> res = new ArrayList<>();
        for (KeyValue<T> item : items) {
            T val = item.getValue();
            res.add(val);
        }
        return res;
    }

    private Query buildQuery(DataQuery dataQuery,
                             int windowAndNextFirst,
                             DataQuery.OrderDirection orderDir,
                             DataQuery.OrderBy orderBy,
                             String orderByChildKey) {

        Query query = dataQuery.getDbRef();
        query = withOrderBy(orderBy, orderByChildKey, query);
        query = withLimitTo(query, windowAndNextFirst, orderDir);

        if (!dataQuery.isFirst()) {
            query = orderDir == DataQuery.OrderDirection.ASC
                    ? withStartAt(dataQuery, query)
                    : withEndAt(dataQuery, query);
        }
        return query;
    }

    private String nextWindowStart(DataQuery dataQuery,
                                   DataSnapshot dataSnapshot) {

        DataQuery.OrderBy orderBy = dataQuery.getOrderBy();
        switch (orderBy) {
            case CHILD:
                return dataSnapshot.child(dataQuery.orderByChildKey()).getValue(String.class);
            case KEY:
                return dataSnapshot.getKey();
            case VALUE:
                return dataSnapshot.getValue(String.class);
            default:
                throw new IllegalArgumentException("Unsupported orderBy: " + orderBy);
        }
    }

    private Query withStartAt(DataQuery dataQuery,
                              Query dbRef) {
        return dbRef.startAt(dataQuery.getWindowStartWith());
    }

    private Query withEndAt(DataQuery dataQuery,
                            Query dbRef) {
        return dbRef.endAt(dataQuery.getWindowStartWith());
    }

    private DataQuery nextAsc(DataQuery dataQuery,
                              DataSnapshot dataSnapshot,
                              int windowSize) {

        int index = 0;
        String nextWindowStart = null;

        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            if (index == windowSize) {
                nextWindowStart = nextWindowStart(dataQuery, snapshot);
            }
            index++;
        }

        return nextWindowStart == null
                ? DataQuery.last(dataQuery)
                : DataQuery.next(dataQuery, nextWindowStart);
    }

    private DataQuery nextDesc(DataQuery dataQuery,
                               DataSnapshot dataSnapshot,
                               int windowSize) {

        String nextStartAt = null;

        DataSnapshot firstSnapshot = null;
        int counter = 0;

        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            if (firstSnapshot == null) {
                firstSnapshot = snapshot;
            }
            counter++;
        }

        if (counter == windowSize + 1) {
            nextStartAt = nextWindowStart(dataQuery, firstSnapshot);
        }

        return nextStartAt == null
                ? DataQuery.last(dataQuery)
                : DataQuery.next(dataQuery, nextStartAt);

    }

    private Pair<List<KeyValue<T>>, DataQuery> onDataChangeAsc(DataQuery dataQuery,
                                                               DataSnapshot dataSnapshot,
                                                               Class<T> clazz,
                                                               int windowSize) {
        int index = 0;
        String nextWindowStart = null;

        List<KeyValue<T>> res = new ArrayList<>();

        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

            T val;
            try {
                val = snapshot.getValue(clazz);
            } catch (DatabaseException e) {
                val = null;
            }
            if (index < windowSize) {
                if (val != null) {
                    res.add(new KeyValue<>(snapshot.getKey(), val));
                }
            } else {
                nextWindowStart = nextWindowStart(dataQuery, snapshot);
            }
            index++;
        }


        DataQuery next = nextWindowStart == null
                ? DataQuery.last(dataQuery)
                : DataQuery.next(dataQuery, nextWindowStart);

        return new Pair<>(res, next);
    }

    private Pair<List<KeyValue<T>>, DataQuery> onDataChangeDesc(DataQuery dataQuery,
                                                                DataSnapshot dataSnapshot,
                                                                Class<T> clazz,
                                                                int windowSize) {
        List<KeyValue<T>> res = new ArrayList<>();
        String nextStartAt = null;

        DataSnapshot firstSnapshot = null;

        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            if (firstSnapshot == null) {
                firstSnapshot = snapshot;
            }
            T val = snapshot.getValue(clazz);
            res.add(0, new KeyValue<>(snapshot.getKey(), val));
        }

        if (res.size() == windowSize + 1) {
            res.remove(res.size() - 1);
            nextStartAt = nextWindowStart(dataQuery, firstSnapshot);
        }

        DataQuery next = nextStartAt == null
                ? DataQuery.last(dataQuery)
                : DataQuery.next(dataQuery, nextStartAt);

        return new Pair<>(res, next);
    }

    private Query withLimitTo(Query dbRef,
                              int windowAndNextFirst,
                              DataQuery.OrderDirection orderDir) {
        switch (orderDir) {
            case ASC:
                dbRef = dbRef.limitToFirst(windowAndNextFirst);
                break;
            case DESC:
                dbRef = dbRef.limitToLast(windowAndNextFirst);
                break;
            default:
                throw new IllegalArgumentException("Unsupported order direction: " + orderDir);
        }
        return dbRef;
    }

    private Query withOrderBy(DataQuery.OrderBy orderBy,
                              String key,
                              Query dbRef) {
        switch (orderBy) {
            case CHILD:
                dbRef = dbRef.orderByChild(key);
                break;
            case KEY:
                dbRef = dbRef.orderByKey();
                break;
            case VALUE:
                dbRef = dbRef.orderByValue();
                break;
            default:
                throw new IllegalArgumentException("Unsupported OrderBy: " + orderBy);
        }
        return dbRef;
    }

    private static class KeyValue<T> {
        private final String key;
        private final T value;

        public KeyValue(String key, T value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public T getValue() {
            return value;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("DataItem{");
            sb.append("key='").append(key).append('\'');
            sb.append(", value=").append(value);
            sb.append('}');
            return sb.toString();
        }
    }

    private static class DelegatingChildEventListener<T> implements ChildEventListener {

        private final LinkedHashMap<KeyAndKind, DataSnapshot> cachedEvents = new LinkedHashMap<>();
        private final NotificationCallback<T> notificationCallback;
        private final Class<T> itemType;
        private volatile boolean dispatchToCallback = false;
        private final Object lock = new Object();

        public DelegatingChildEventListener(NotificationCallback<T> notificationCallback,
                                            Class<T> itemType) {
            this.notificationCallback = notificationCallback;
            this.itemType = itemType;
        }

        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            processChildEvent(dataSnapshot, itemType, DataWindowChangeEvent.Kind.ADDED);
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            processChildEvent(dataSnapshot, itemType, DataWindowChangeEvent.Kind.CHANGED);
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            processChildEvent(dataSnapshot, itemType, DataWindowChangeEvent.Kind.REMOVED);
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            processChildEvent(dataSnapshot, itemType, DataWindowChangeEvent.Kind.MOVED);
        }

        private void processChildEvent(DataSnapshot dataSnapshot,
                                       Class<T> type,
                                       DataWindowChangeEvent.Kind kind) {

            T value = dataSnapshot.getValue(type);

            if (value != null) {
                DataWindowChangeEvent<T> event = new DataWindowChangeEvent<>(
                        value,
                        kind);

                synchronized (lock) {
                    if (dispatchToCallback) {
                        notificationCallback.onChildChanged(event);
                    } else {
                        cachedEvents.put(new KeyAndKind(dataSnapshot.getKey(), kind),
                                dataSnapshot);
                    }
                }
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            notificationCallback.onError(databaseError);
        }

        public void removeEvents(List<KeyValue<T>> keyValues) {
            synchronized (lock) {
                for (KeyValue<T> keyValue : keyValues) {
                    cachedEvents.remove(new KeyAndKind(
                            keyValue.getKey(),
                            DataWindowChangeEvent.Kind.ADDED));
                }
            }
        }

        public void dispatchChildEvents() {
            synchronized (lock) {
                for (Map.Entry<KeyAndKind, DataSnapshot> event : cachedEvents.entrySet()) {
                    T value = event.getValue().getValue(itemType);

                    if (value != null) {
                        notificationCallback.onChildChanged(
                                new DataWindowChangeEvent<>(
                                        value,
                                        event.getKey().getKind()));
                    }
                }
                cachedEvents.clear();
            }
            dispatchToCallback = true;
        }
    }

    private static class KeyAndKind {

        private final String key;
        private final DataWindowChangeEvent.Kind kind;

        public KeyAndKind(String key, DataWindowChangeEvent.Kind kind) {
            this.key = key;
            this.kind = kind;
        }

        public String getKey() {
            return key;
        }

        public DataWindowChangeEvent.Kind getKind() {
            return kind;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            KeyAndKind that = (KeyAndKind) o;

            if (!key.equals(that.key)) return false;
            return kind == that.kind;

        }

        @Override
        public int hashCode() {
            int result = key.hashCode();
            result = 31 * result + kind.hashCode();
            return result;
        }
    }

    private static class NextSignals {
        /*used to cancel data notification, if cancel was issued after native data callback, but before
        * delivering data to client*/
        private boolean isCancelled;
        /*used to cancel child event notifications (c.e.n.) ONLY for data query which is in progress. Once
        * data is delivered, client should use c.e.n handle to cancel c.e.n.*/
        private boolean isDataQueryInProgress;

        public boolean isCancelled() {
            return isCancelled;
        }

        public void setCancelled(boolean cancelled) {
            isCancelled = cancelled;
        }

        public boolean isDataQueryInProgress() {
            return isDataQueryInProgress;
        }

        public void setDataQueryInProgress(boolean dataQueryInProgress) {
            isDataQueryInProgress = dataQueryInProgress;
        }
    }

    static class Pair<L, R> {

        private final L left;
        private final R right;

        public Pair(L left, R right) {
            this.left = left;
            this.right = right;
        }

        public L getLeft() {
            return left;
        }

        public R getRight() {
            return right;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Pair{");
            sb.append("left=").append(left);
            sb.append(", right=").append(right);
            sb.append('}');
            return sb.toString();
        }
    }
}
