package com.github.mostroverkhov.firebase_data_rxjava.rx;

import com.github.mostroverkhov.datawindowsource.DataWindowSource;
import com.github.mostroverkhov.datawindowsource.callbacks.NextWindowCallback;
import com.github.mostroverkhov.datawindowsource.callbacks.NotificationCallback;
import com.github.mostroverkhov.datawindowsource.callbacks.QueryHandle;
import com.github.mostroverkhov.datawindowsource.model.DataQuery;
import com.github.mostroverkhov.datawindowsource.model.WindowChangeEvent;
import com.github.mostroverkhov.datawindowsource.model.NextQueryCurrentCount;
import com.google.firebase.database.DatabaseError;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

/**
 * Created with IntelliJ IDEA.
 * Author: mostroverkhov
 */
class DataNotificationsOnSubscribe<T> implements Observable.OnSubscribe<WindowChangeEvent<T>> {

    private static final NextWindowCallback NOOP_NEXT_QUERY_CALLBACK = new NoopNextQueryCallback();

    private DataQuery dataQuery;
    private Class<T> itemType;
    private final DataWindowSource<T> dataWindowSource;

    public DataNotificationsOnSubscribe(DataQuery dataQuery,
                                        Class<T> itemType,
                                        DataWindowSource<T> dataWindowSource) {
        this.dataQuery = dataQuery;
        this.itemType = itemType;
        this.dataWindowSource = dataWindowSource;
    }

    @Override
    public void call(final Subscriber<? super WindowChangeEvent<T>> subscriber) {
        if (!subscriber.isUnsubscribed()) {
            final QueryHandle queryHandle = dataWindowSource.next(
                    dataQuery,
                    itemType,
                    NOOP_NEXT_QUERY_CALLBACK,
                    new NotificationCallback<T>() {
                        @Override
                        public void onChildChanged(WindowChangeEvent<T> event) {
                            if (!subscriber.isUnsubscribed()) {
                                subscriber.onNext(event);
                            }
                        }

                        @Override
                        public void onError(DatabaseError e) {
                            if (!subscriber.isUnsubscribed()) {
                                subscriber.onError(new FirebaseDataException(e));
                            }
                        }
                    });
            subscriber.add(Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    queryHandle.cancel();
                }
            }));
        }
    }

    private static class NoopNextQueryCallback implements NextWindowCallback {
        @Override
        public void onData(NextQueryCurrentCount nextQueryCurrentCount) {

        }

        @Override
        public void onError(DatabaseError e) {

        }
    }
}
