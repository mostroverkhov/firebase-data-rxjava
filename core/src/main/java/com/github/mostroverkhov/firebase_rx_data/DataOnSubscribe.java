package com.github.mostroverkhov.firebase_rx_data;

import com.github.mostroverkhov.firebase_rx_data.lib.DataCallback;
import com.github.mostroverkhov.firebase_rx_data.lib.DataWindowSource;
import com.github.mostroverkhov.firebase_rx_data.model.DataWindowResult;
import com.github.mostroverkhov.firebase_rx_data.model.DataQuery;
import com.github.mostroverkhov.firebase_rx_data.model.Window;
import com.google.firebase.database.DatabaseError;

import java.util.List;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.observables.AsyncOnSubscribe;

/**
 * Created by Maksym Ostroverkhov on 20.07.2016.
 */
class DataOnSubscribe<T> extends AsyncOnSubscribe<State, Window<T>> {
    private final DataQuery dataQuery;
    private final Class<T> type;

    public DataOnSubscribe(DataQuery dataQuery, Class<T> type) {
        this.dataQuery = dataQuery;
        this.type = type;
    }

    @Override
    protected State generateState() {
        return new State(dataQuery);
    }

    @Override
    protected State next(State state, long requested, Observer<Observable<? extends Window<T>>> observer) {
        observer.onNext(Observable.create(new DataWindowOnSubscribe<>(requested, type, state, observer))
                .onBackpressureBuffer());

        return state;
    }

    private static class DataWindowOnSubscribe<T> implements Observable.OnSubscribe<Window<T>> {

        private final long requested;
        private final Class<T> type;
        private final State state;
        private final Observer<Observable<? extends Window<T>>> observer;
        private volatile boolean isInterrupted;

        public DataWindowOnSubscribe(long requested,
                                     Class<T> type,
                                     State state,
                                     Observer<Observable<? extends Window<T>>> observer) {
            this.requested = requested;
            this.type = type;
            this.state = state;
            this.observer = observer;
        }

        @Override
        public void call(final Subscriber<? super Window<T>> subscriber) {
            DataWindowSource<T> dataWindowSource = new DataWindowSource<>();
            nextWindow(subscriber, dataWindowSource, 0);
        }


        private void nextWindow(final Subscriber<? super Window<T>> subscriber,
                                final DataWindowSource<T> dataWindowSource,
                                final long index) {

            final DataQuery curQuery = state.getNext();

            dataWindowSource.next(curQuery, type,
                    new DataCallback<T, DataWindowResult<T>>() {

                        @Override
                        public void onError(DatabaseError e) {
                            FirebaseDataException ex = new FirebaseDataException(e);
                            subscriber.onError(ex);
                            observer.onError(ex);
                        }

                        @Override
                        public void onData(DataWindowResult<T> result) {
                            state.setNext(result.getNext());

                            List<T> data = result.getData();

                            if (subscriber.isUnsubscribed()) {
                                isInterrupted = true;
                            } else if (!data.isEmpty()) {
                                subscriber.onNext(new Window<>(data, curQuery));

                                if (isLast(index)) {
                                    subscriber.onCompleted();
                                    isInterrupted = true;
                                }
                            /*empty data - signal interrupt*/
                            } else {
                                subscriber.onCompleted();
                                observer.onCompleted();
                                isInterrupted = true;
                            }
                            if (!isInterrupted) {
                                nextWindow(subscriber, dataWindowSource, index + 1);
                            }
                        }

                        private boolean isLast(long index) {
                            return index == requested - 1;
                        }
                    });
        }
    }
}
