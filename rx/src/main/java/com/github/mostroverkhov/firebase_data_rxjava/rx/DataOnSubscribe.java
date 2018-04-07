package com.github.mostroverkhov.firebase_data_rxjava.rx;

import com.github.mostroverkhov.datawindowsource.DataWindowSource;
import com.github.mostroverkhov.datawindowsource.callbacks.DataCallback;
import com.github.mostroverkhov.datawindowsource.model.DataQuery;
import com.github.mostroverkhov.datawindowsource.model.DataWindow;
import com.github.mostroverkhov.firebase_data_rxjava.rx.model.Window;
import com.google.firebase.database.DatabaseError;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.observables.AsyncOnSubscribe;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Maksym Ostroverkhov on 20.07.2016.
 */
class DataOnSubscribe<T> extends AsyncOnSubscribe<State<T>, Window<T>> {
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
    protected State<T> generateState() {
        return new State<>(dataQuery);
    }

    @Override
    protected State<T> next(final State<T> state,
                            long requested,
                            Observer<Observable<? extends Window<T>>> observer) {
        DataWindowOnSubscribe<T> dataWindowOnSubscribe =
                new DataWindowOnSubscribe<>(dataWindowSource,
                        requested,
                        type,
                        state,
                        observer,
                        new WindowProcessedCallback(state));

        state.getSubscribeFuncs().offer(dataWindowOnSubscribe);
        if (state.observablesCount().getAndIncrement() == 0) {
            queryNextWindow(state);
        }
        observer.onNext(Observable.unsafeCreate(dataWindowOnSubscribe));

        return state;
    }

    private void queryNextWindow(final State<T> state) {

        DataWindowOnSubscribe<T> windowObservable = state.getSubscribeFuncs().poll();
        windowObservable.enable();
    }

    static class DataWindowOnSubscribe<T> implements Observable.OnSubscribe<Window<T>> {

        private final DataWindowSource dataWindowSource;
        private final long requested;
        private final Class<T> type;
        private final State state;
        private final Observer<Observable<? extends Window<T>>> observer;
        private final DataWindowProcessedCallback dataWindowProcessedCallback;
        private volatile boolean stopCurrentRound;
        private volatile boolean isDataAvailable = true;
        private volatile boolean enableCalled;
        private volatile boolean subscribeCalled;
        private volatile Subscriber<? super Window<T>> subscriber;

        public DataWindowOnSubscribe(DataWindowSource dataWindowSource,
                                     long requested,
                                     Class<T> type,
                                     State state,
                                     Observer<Observable<? extends Window<T>>> observer,
                                     DataWindowProcessedCallback dataWindowProcessedCallback) {
            this.dataWindowSource = dataWindowSource;
            this.requested = requested;
            this.type = type;
            this.state = state;
            this.observer = observer;
            this.dataWindowProcessedCallback = dataWindowProcessedCallback;
        }

        public void enable() {
            enableCalled = true;
            dispatchChange();
        }

        public void complete() {
            if (subscriber != null) {
                subscriber.onCompleted();
            }
        }

        @Override
        public void call(final Subscriber<? super Window<T>> subscriber) {
            this.subscriber = subscriber;
            this.subscribeCalled = true;
            dispatchChange();
        }

        private void dispatchChange() {
            if (subscribeCalled && enableCalled) {
                nextWindow(subscriber, observer, dataWindowSource, 0);
            }
        }

        private void nextWindow(final Subscriber<? super Window<T>> subscriber,
                                final Observer<Observable<? extends Window<T>>> observer,
                                final DataWindowSource dataWindowSource,
                                final long index) {
            if (subscriber.isUnsubscribed()) {
                return;
            }
            final DataQuery curQuery = state.getNext();
            dataWindowSource.next(curQuery, type,
                    new DataCallback<T, DataWindow<T>>() {

                        @Override
                        public void onError(DatabaseError e) {
                            FirebaseDataException ex = new FirebaseDataException(e);
                            subscriber.onError(ex);
                            observer.onError(ex);
                        }

                        @Override
                        public void onData(DataWindow<T> result) {
                            state.setNext(result.getNext());

                            List<T> data = result.getData();

                            if (subscriber.isUnsubscribed()) {
                                stopCurrentRound = true;
                            } else if (!data.isEmpty()) {
                                subscriber.onNext(new Window<>(data, curQuery));

                                if (isLast(index)) {
                                    subscriber.onCompleted();
                                    stopCurrentRound = true;
                                }
                            /*empty data - signal interrupt*/
                            } else {
                                subscriber.onCompleted();
                                observer.onCompleted();
                                stopCurrentRound = true;
                                isDataAvailable = false;
                            }
                            if (!stopCurrentRound) {
                                nextWindow(subscriber, observer, dataWindowSource, index + 1);
                            } else {
                                dataWindowProcessedCallback.requestProcessed(isDataAvailable);
                            }
                        }

                        private boolean isLast(long index) {
                            return index == requested - 1;
                        }
                    });
        }
    }

    interface DataWindowProcessedCallback {

        void requestProcessed(boolean dataAvailable);
    }

    private class WindowProcessedCallback implements DataWindowProcessedCallback {
        private final State<T> state;

        public WindowProcessedCallback(State<T> state) {
            this.state = state;
        }

        @Override
        public void requestProcessed(boolean dataAvailable) {
            AtomicInteger observablesCount = state.observablesCount();
            if (observablesCount.decrementAndGet() != 0) {
                if (dataAvailable) {
                    queryNextWindow(state);
                } else {
                    completeWindowObservables(observablesCount, state);
                }
            }
        }

        private void completeWindowObservables(AtomicInteger observablesCount, State<T> state) {
            observablesCount.set(0);
            for (DataWindowOnSubscribe<T> f : state.getSubscribeFuncs()) {
                f.complete();
            }
        }
    }
}
