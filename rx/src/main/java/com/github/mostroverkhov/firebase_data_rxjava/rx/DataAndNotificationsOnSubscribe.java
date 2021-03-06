package com.github.mostroverkhov.firebase_data_rxjava.rx;

import com.github.mostroverkhov.datawindowsource.DataWindowSource;
import com.github.mostroverkhov.datawindowsource.NotificationsHandle;
import com.github.mostroverkhov.datawindowsource.callbacks.DataCallback;
import com.github.mostroverkhov.datawindowsource.callbacks.NotificationCallback;
import com.github.mostroverkhov.datawindowsource.callbacks.QueryHandle;
import com.github.mostroverkhov.datawindowsource.model.DataQuery;
import com.github.mostroverkhov.datawindowsource.model.DataWindowNotifications;
import com.github.mostroverkhov.datawindowsource.model.WindowChangeEvent;
import com.github.mostroverkhov.firebase_data_rxjava.rx.model.WindowWithNotifications;
import com.google.firebase.database.DatabaseError;

import java.util.List;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Action0;
import rx.observables.AsyncOnSubscribe;
import rx.subjects.Subject;
import rx.subjects.UnicastSubject;

/**
 * Created by Maksym Ostroverkhov on 20.07.2016.
 */
class DataAndNotificationsOnSubscribe<T> extends AsyncOnSubscribe<State, WindowWithNotifications<T>> {

    private final DataWindowSource dataWindowSource;
    private final DataQuery dataQuery;
    private final Class<T> dataItemType;
    private final DataNotificationsWatcher watcher;

    public DataAndNotificationsOnSubscribe(DataWindowSource dataWindowSource,
                                           DataQuery dataQuery,
                                           Class<T> dataItemType,
                                           DataNotificationsWatcher watcher) {
        this.dataWindowSource = dataWindowSource;
        this.dataQuery = dataQuery;
        this.dataItemType = dataItemType;
        this.watcher = watcher;
    }

    @Override
    protected State generateState() {
        return new State(dataQuery);
    }

    @Override
    protected State next(final State state,
                         long requested,
                         final Observer<Observable<? extends WindowWithNotifications<T>>> observer) {

        final Observable<WindowWithNotifications<T>> readResultObservable = Observable
                .unsafeCreate(new WindowWithNotificationsOnSubscribe<>(dataWindowSource,
                        state,
                        requested,
                        dataItemType,
                        observer,
                        watcher));
        observer.onNext(readResultObservable.onBackpressureBuffer());

        return state;
    }

    @Override
    protected void onUnsubscribe(State state) {
        state.cancelDataHandles();
    }

    private static class WindowWithNotificationsOnSubscribe<T> implements Observable.OnSubscribe<WindowWithNotifications<T>> {

        private final DataWindowSource dataWindowSource;
        private final State state;
        private final long requested;
        private final Class<T> dataItemType;
        private final Observer<Observable<? extends WindowWithNotifications<T>>> observer;
        private final DataNotificationsWatcher watcher;
        private volatile boolean isInterrupted;

        public WindowWithNotificationsOnSubscribe(DataWindowSource dataWindowSource,
                                                  State state,
                                                  long requested,
                                                  Class<T> dataItemType,
                                                  Observer<Observable<? extends WindowWithNotifications<T>>> observer,
                                                  DataNotificationsWatcher watcher) {
            this.dataWindowSource = dataWindowSource;
            this.state = state;
            this.requested = requested;
            this.dataItemType = dataItemType;
            this.observer = observer;
            this.watcher = watcher;
        }

        @Override
        public void call(final Subscriber<? super WindowWithNotifications<T>> subscriber) {

            long index = 0;
            windowByIndex(dataWindowSource, subscriber, index);
        }

        private void windowByIndex(DataWindowSource dataWindowSource,
                                   Subscriber<? super WindowWithNotifications<T>> subscriber,
                                   long index) {
            if (index >= requested || isInterrupted) {
                return;
            }
            final Subject<WindowChangeEvent, WindowChangeEvent> childChangeSubject
                    = UnicastSubject.<WindowChangeEvent>create().toSerialized();

            DataQuery query = state.getNext();

            RxDataCallback dataCallback = new RxDataCallback(
                    childChangeSubject,
                    subscriber,
                    state,
                    dataWindowSource,
                    watcher,
                    index);

            RxNotificationCallback notificationCallback =
                    new RxNotificationCallback(childChangeSubject);

            QueryHandle queryHandle = dataWindowSource.next(query,
                    dataItemType,
                    dataCallback,
                    notificationCallback);

            dataCallback.setHandle(queryHandle);
            state.addQueryHandle(queryHandle);
        }

        private static class RxNotificationCallback implements NotificationCallback {
            private final Subject<WindowChangeEvent, WindowChangeEvent> childChangeSubject;

            public RxNotificationCallback(Subject<WindowChangeEvent, WindowChangeEvent> childChangeSubject) {
                this.childChangeSubject = childChangeSubject;
            }

            @Override
            public void onChildChanged(WindowChangeEvent event) {
                childChangeSubject.onNext(event);
            }

            @Override
            public void onError(DatabaseError e) {
                childChangeSubject.onError(new FirebaseDataException(e));
            }
        }

        private class RxDataCallback implements DataCallback<T, DataWindowNotifications<T>> {
            private final Subject<WindowChangeEvent, WindowChangeEvent> childChangeSubject;
            private final Subscriber<? super WindowWithNotifications<T>> readResultSubscriber;
            private final State state;
            private final DataWindowSource dataWindowSource;
            private final DataNotificationsWatcher watcher;
            private final long index;
            private volatile QueryHandle queryHandle;

            public RxDataCallback(Subject<WindowChangeEvent,
                    WindowChangeEvent> childChangeSubject,
                                  Subscriber<? super WindowWithNotifications<T>> readResultSubscriber,
                                  State state,
                                  DataWindowSource dataWindowSource,
                                  DataNotificationsWatcher watcher,
                                  long index) {

                this.childChangeSubject = childChangeSubject;
                this.readResultSubscriber = readResultSubscriber;
                this.state = state;
                this.dataWindowSource = dataWindowSource;
                this.watcher = watcher;
                this.index = index;
            }

            /*used to remove handle once data is ready*/
            public void setHandle(QueryHandle queryHandle) {
                this.queryHandle = queryHandle;
            }

            @Override
            public void onData(DataWindowNotifications<T> result) {
                DataQuery curQuery = state.getNext();
                DataQuery nextQuery = result.getNext();
                state.setNext(nextQuery);
                List<T> data = result.getData();

                if (readResultSubscriber.isUnsubscribed()) {
                    isInterrupted = true;

                } else if (!data.isEmpty()) {
                    final NotificationsHandle notificationsHandle = result
                            .getNotificationsHandle();
                    notificationsHandle.startListenToNotifications();
                    final WindowWithNotifications<T> res = new WindowWithNotifications<>(data,
                            childChangeSubject
                                    .doOnUnsubscribe(new Action0() {
                                        @Override
                                        public void call() {
                                            notificationsHandle.stopListenToNotifications();
                                        }
                                    })
                                    .onBackpressureBuffer(),
                            curQuery);

                    watcher.addReadResult(res, notificationsHandle);
                    readResultSubscriber.onNext(res);

                    if (isLastIndex(index)) {
                        readResultSubscriber.onCompleted();
                    }
                } else {
                    isInterrupted = true;
                    readResultSubscriber.onCompleted();
                    observer.onCompleted();
                }
                state.removeQueryHandle(queryHandle);

                windowByIndex(dataWindowSource, readResultSubscriber, index + 1);
            }

            private boolean isLastIndex(long index) {
                return index == requested - 1;
            }

            @Override
            public void onError(DatabaseError e) {
                readResultSubscriber.onError(new FirebaseDataException(e));
                observer.onError(new FirebaseDataException(e));
            }
        }
    }
}
