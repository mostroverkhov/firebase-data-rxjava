package com.github.mostroverkhov.datawindowsource;

import com.github.mostroverkhov.datawindowsource.callbacks.NotificationCallback;
import com.github.mostroverkhov.datawindowsource.model.WindowChangeEvent;
import com.github.mostroverkhov.datawindowsource.util.KeyAndKind;
import com.github.mostroverkhov.datawindowsource.util.KeyValue;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 * Author: mostroverkhov
 */
class DelegatingChildEventListener<T> implements ChildEventListener {

    private final ConcurrentMap<KeyAndKind, DataSnapshot> cachedEvents = new ConcurrentHashMap<>();
    private final Scheduler scheduler;
    private final NotificationCallback<T> notificationCallback;
    private final Class<T> itemType;
    private volatile boolean dispatchToCallback = false;
    private final AtomicInteger actionsCounter = new AtomicInteger();
    private final Queue<Runnable> actions = new ConcurrentLinkedQueue<>();

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

    private void processChildEvent(final DataSnapshot dataSnapshot,
                                   final Class<T> type,
                                   final WindowChangeEvent.Kind kind) {
        actions.offer(new Runnable() {
            @Override
            public void run() {
                T value = dataSnapshot.getValue(type);
                if (value != null) {
                    WindowChangeEvent<T> event = new WindowChangeEvent<>(
                            value,
                            kind);
                    if (dispatchToCallback) {
                        dispatchDataNotificationEvent(event);
                    } else {
                        cachedEvents.put(
                                new KeyAndKind(dataSnapshot.getKey(), kind),
                                dataSnapshot);
                    }
                }
            }
        });
        drain();
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
        dispatchDataNotificationError(databaseError);
    }


    public void removeEvents(final List<KeyValue<T>> keyValues) {
        actions.offer(new Runnable() {
            @Override
            public void run() {
                for (KeyValue<T> keyValue : keyValues) {
                    cachedEvents.remove(new KeyAndKind(
                            keyValue.getKey(),
                            WindowChangeEvent.Kind.ADDED));
                }
            }
        });
        drain();
    }

    public void dispatchChildEvents() {
        actions.offer(new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<KeyAndKind, DataSnapshot> event : cachedEvents.entrySet()) {
                    T value = event.getValue().getValue(itemType);

                    if (value != null) {
                        dispatchDataNotificationEvent(new WindowChangeEvent<>(
                                value,
                                event.getKey().getKind()));
                    }
                }
                cachedEvents.clear();
                dispatchToCallback = true;
            }
        });
        drain();
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

    private void drain() {
        if (actionsCounter.getAndIncrement() == 0) {
            do {
                Runnable action = actions.poll();
                action.run();
            } while (actionsCounter.decrementAndGet() != 0);
        }
    }
}
