package com.github.mostroverkhov.firebase_data_rxjava.rx;

import com.github.mostroverkhov.datawindowsource.DataWindowSource;
import com.github.mostroverkhov.datawindowsource.callbacks.NextWindowCallback;
import com.github.mostroverkhov.datawindowsource.callbacks.NotificationCallback;
import com.github.mostroverkhov.datawindowsource.callbacks.QueryHandle;
import com.github.mostroverkhov.datawindowsource.model.*;
import com.google.firebase.database.DatabaseError;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created with IntelliJ IDEA.
 * Author: mostroverkhov
 */
class NotificationsOnSubscribe<T> implements Observable.OnSubscribe<DataItem> {

    private DataQuery dataQuery;
    private Class<T> itemType;
    private final DataWindowSource dataWindowSource;

    public NotificationsOnSubscribe(DataQuery dataQuery,
                                    Class<T> itemType,
                                    DataWindowSource dataWindowSource) {
        this.dataQuery = dataQuery;
        this.itemType = itemType;
        this.dataWindowSource = dataWindowSource;
    }

    @Override
    public void call(final Subscriber<? super DataItem> subscriber) {
        if (!subscriber.isUnsubscribed()) {
            final QueryHandle queryHandle = dataWindowSource.next(
                    dataQuery,
                    itemType,
                    new SubscriberNextWindowCallback(
                            new OnErrorContractEnforcer(subscriber)),
                    new SubscriberNotificationCallback<T>(
                            new OnErrorContractEnforcer(subscriber)));
            subscriber.add(Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    queryHandle.cancel();
                }
            }));
        }
    }

    private static class SubscriberNotificationCallback<T> implements NotificationCallback<T> {
        private final OnErrorContractEnforcer contractEnforcer;

        public SubscriberNotificationCallback(OnErrorContractEnforcer contractEnforcer) {
            this.contractEnforcer = contractEnforcer;
        }

        @Override
        public void onChildChanged(WindowChangeEvent<T> event) {
            contractEnforcer.onNext(event);
        }

        @Override
        public void onError(DatabaseError e) {
            contractEnforcer.onError(new FirebaseDataException(e));
        }
    }

    private static class SubscriberNextWindowCallback implements NextWindowCallback {
        private OnErrorContractEnforcer contractEnforcer;

        public SubscriberNextWindowCallback(OnErrorContractEnforcer contractEnforcer) {
            this.contractEnforcer = contractEnforcer;
        }

        @Override
        public void onData(NextQueryCurrentCount nextQueryCurrentCount) {
            contractEnforcer.onNext(new NextQuery(nextQueryCurrentCount.getNext()));
        }

        @Override
        public void onError(DatabaseError e) {
            contractEnforcer.onError(new FirebaseDataException(e));
        }
    }

    private static class OnErrorContractEnforcer {
        private final Subscriber<? super DataItem> subscriber;
        private final AtomicBoolean errorSignal = new AtomicBoolean();

        public OnErrorContractEnforcer(Subscriber<? super DataItem> subscriber) {
            this.subscriber = subscriber;
        }

        public void onNext(DataItem dataItem) {
            if (!subscriber.isUnsubscribed()) {
                subscriber.onNext(dataItem);
            }
        }

        public void onError(Exception e) {
            boolean firstOnError = errorSignal.compareAndSet(false, true);
            if (firstOnError && !subscriber.isUnsubscribed()) {
                subscriber.onError(e);
            }
        }
    }
}
