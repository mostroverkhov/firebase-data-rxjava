package com.github.mostroverkhov.datawindowsource;

import com.github.mostroverkhov.datawindowsource.callbacks.NotificationCallback;
import com.github.mostroverkhov.datawindowsource.model.WindowChangeEvent;
import com.github.mostroverkhov.datawindowsource.util.KeyAndKind;
import com.github.mostroverkhov.datawindowsource.util.KeyValue;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Author: mostroverkhov
 */
class DelegatingChildEventListener<T> implements ChildEventListener {

    private final LinkedHashMap<KeyAndKind, DataSnapshot> cachedEvents = new LinkedHashMap<>();
    private final Scheduler scheduler;
    private final NotificationCallback<T> notificationCallback;
    private final Class<T> itemType;
    private volatile boolean dispatchToCallback = false;
    private final Object lock = new Object();

    public DelegatingChildEventListener(Scheduler scheduler,
                                        NotificationCallback<T> notificationCallback,
                                        Class<T> itemType) {
        this.scheduler = scheduler;
        this.notificationCallback = notificationCallback;
        this.itemType = itemType;
    }

    @Override
    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
        processChildEvent(dataSnapshot, itemType, WindowChangeEvent.Kind.ADDED);
    }

    @Override
    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
        processChildEvent(dataSnapshot, itemType, WindowChangeEvent.Kind.CHANGED);
    }

    @Override
    public void onChildRemoved(DataSnapshot dataSnapshot) {
        processChildEvent(dataSnapshot, itemType, WindowChangeEvent.Kind.REMOVED);
    }

    @Override
    public void onChildMoved(DataSnapshot dataSnapshot, String s) {
        processChildEvent(dataSnapshot, itemType, WindowChangeEvent.Kind.MOVED);
    }

    private void processChildEvent(DataSnapshot dataSnapshot,
                                   Class<T> type,
                                   WindowChangeEvent.Kind kind) {

        T value = dataSnapshot.getValue(type);

        if (value != null) {
            WindowChangeEvent<T> event = new WindowChangeEvent<>(
                    value,
                    kind);

            synchronized (lock) {
                if (dispatchToCallback) {
                    dispatchDataNotificationEvent(event);
                } else {
                    cachedEvents.put(new KeyAndKind(dataSnapshot.getKey(), kind),
                            dataSnapshot);
                }
            }
        }
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
        dispatchDataNotificationError(databaseError);
    }


    public void removeEvents(List<KeyValue<T>> keyValues) {
        synchronized (lock) {
            for (KeyValue<T> keyValue : keyValues) {
                cachedEvents.remove(new KeyAndKind(
                        keyValue.getKey(),
                        WindowChangeEvent.Kind.ADDED));
            }
        }
    }

    public void dispatchChildEvents() {
        synchronized (lock) {
            for (Map.Entry<KeyAndKind, DataSnapshot> event : cachedEvents.entrySet()) {
                T value = event.getValue().getValue(itemType);

                if (value != null) {
                    dispatchDataNotificationEvent(new WindowChangeEvent<>(
                            value,
                            event.getKey().getKind()));
                }
            }
            cachedEvents.clear();
        }
        dispatchToCallback = true;
    }

    private void dispatchDataNotificationEvent(final WindowChangeEvent<T> event) {
        scheduler.execute(new Runnable() {
            @Override
            public void run() {
                notificationCallback.onChildChanged(event);
            }
        });
    }

    private void dispatchDataNotificationError(final DatabaseError databaseError) {
        scheduler.execute(new Runnable() {
            @Override
            public void run() {
                notificationCallback.onError(databaseError);
            }
        });
    }
}
