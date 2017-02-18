package com.github.mostroverkhov.firebase_data_rxjava.rx;

import com.github.mostroverkhov.datawindowsource.DataWindowSource;
import com.github.mostroverkhov.datawindowsource.callbacks.DataCallback;
import com.github.mostroverkhov.datawindowsource.model.DataQuery;
import com.github.mostroverkhov.datawindowsource.model.DataWindowResult;
import com.github.mostroverkhov.firebase_data_rxjava.rx.model.Window;
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
    private final DataWindowSource dataWindowSource;
    private final DataQuery dataQuery;
    private final Class<T> type;

    public DataOnSubscribe(DataWindowSource dataWindowSource,
                           DataQuery dataQuery,
                           Class<T> type) {
        this.dataWindowSource = dataWindowSource;
        this.dataQuery = dataQuery;
        this.type = type;
    }

    @Override
    protected State generateState() {
        return new State(dataQuery);
    }

    @Override
    protected State next(State state, long requested, Observer<Observable<? extends Window<T>>> observer) {
        observer.onNext(Observable.create(new DataWindowOnSubscribe<>(dataWindowSource, requested, type, state, observer))
                .onBackpressureBuffer());

        return state;
    }

    private static class DataWindowOnSubscribe<T> implements Observable.OnSubscribe<Window<T>> {

        private final DataWindowSource dataWindowSource;
        private final long requested;
        private final Class<T> type;
        private final State state;
        private final Observer<Observable<? extends Window<T>>> observer;
        private volatile boolean isInterrupted;

        public DataWindowOnSubscribe(DataWindowSource dataWindowSource,
                                     long requested,
                                     Class<T> type,
                                     State state,
                                     Observer<Observable<? extends Window<T>>> observer) {
            this.dataWindowSource = dataWindowSource;
            this.requested = requested;
            this.type = type;
            this.state = state;
            this.observer = observer;
        }

        @Override
        public void call(final Subscriber<? super Window<T>> subscriber) {
            nextWindow(subscriber, dataWindowSource, 0);
        }


        private void nextWindow(final Subscriber<? super Window<T>> subscriber,
                                final DataWindowSource dataWindowSource,
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
