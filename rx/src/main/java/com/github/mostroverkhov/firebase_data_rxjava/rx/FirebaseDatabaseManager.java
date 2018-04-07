package com.github.mostroverkhov.firebase_data_rxjava.rx;

import com.github.mostroverkhov.datawindowsource.CurrentThreadScheduler;
import com.github.mostroverkhov.datawindowsource.DataWindowSource;
import com.github.mostroverkhov.datawindowsource.Scheduler;
import com.github.mostroverkhov.datawindowsource.model.DataItem;
import com.github.mostroverkhov.datawindowsource.model.DataQuery;
import com.github.mostroverkhov.datawindowsource.model.WindowChangeEvent;
import com.github.mostroverkhov.firebase_data_rxjava.rx.model.Window;
import com.github.mostroverkhov.firebase_data_rxjava.rx.model.WindowWithNotifications;
import com.github.mostroverkhov.firebase_data_rxjava.rx.model.WriteResult;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;

/**
 * Created by Maksym Ostroverkhov on 07.07.2016.
 */

/**
 * Entry point for firebase database rx capabilities
 */
public class FirebaseDatabaseManager {

    private final DatabaseReference root;
    private final DataNotificationsWatcher watcher;
    private DataWindowSource dataWindowSource;

    /**
     * @param root      firebase root path
     * @param scheduler determines context for firebase events delivery
     */
    public FirebaseDatabaseManager(DatabaseReference root, Scheduler scheduler) {
        this.root = root;
        this.dataWindowSource = new DataWindowSource(scheduler);
        this.watcher = new DataNotificationsWatcher();
    }

    public FirebaseDatabaseManager(DatabaseReference root) {
        this(root, CurrentThreadScheduler.getInstance());
    }

    /**
     * @param dbRefFunc allows to change root reference
     * @return entry point for firebase database rx capabilities
     */
    public Data data(DbRefFunc dbRefFunc) {
        return new Data(dataWindowSource, dbRefFunc.map(root), watcher);
    }

    /**
     * @return entry point for firebase database
     */
    public Data data() {
        return new Data(dataWindowSource, root, watcher);
    }

    public static class Data {
        private final DatabaseReference dbRef;
        private final DataNotificationsWatcher watcher;
        private final DataWindowSource dataWindowSource;

        Data(DataWindowSource dataWindowSource,
             DatabaseReference dbRef,
             DataNotificationsWatcher watcher) {
            this.dbRef = dbRef;
            this.watcher = watcher;
            this.dataWindowSource = dataWindowSource;
        }

        /**
         * Provides data window as observable of WindowChangeEvent for each individual item,
         * type of which is known and same, interleaved with DataQuery for next window
         *
         * @param dataQuery determines window into firebase database data
         * @param itemType  type of item for window data
         * @return observable of child items events. Buffering is used for backpressure
         */
        public <T> Observable<DataItem> notifications(DataQuery dataQuery,
                                                      Class<T> itemType) {
            return Observable
                    .unsafeCreate(new NotificationsOnSubscribe<T>(dataQuery, itemType, dataWindowSource))
                    .serialize()
                    .onBackpressureBuffer();
        }


        /**
         * Provides data window as observable of WindowChangeEvent for each individual item,
         * interleaved with DataQuery for next window
         *
         * @param dataQuery determines window into firebase database data
         * @return observable of child items events. Buffering is used for backpressure
         */
        public Observable<DataItem> notifications(DataQuery dataQuery) {
            return notifications(dataQuery, Object.class);
        }

        /**
         * Observable data window, items type of which is known and same.
         *
         * @param dataQuery determines window into firebase database data
         * @param itemType  of item for window data
         * @param <T>       type of item for window data
         * @return observable data for window, together with notifications observable for
         * changes to this window data
         */
        public <T> Observable<WindowWithNotifications<T>> windowWithNotifications(DataQuery dataQuery,
                                                                                  Class<T> itemType) {
            return Observable.create(new DataAndNotificationsOnSubscribe<>(dataWindowSource, dataQuery, itemType, watcher));
        }

        /**
         * Observable data window, items type of which is either unknown or changing. Returned data is
         * either Map<String,Object> or String.
         *
         * @param dataQuery determines window into firebase database data
         * @return observable data for window, together with notifications observable for
         * changes to this window data
         */
        public Observable<WindowWithNotifications<Object>> windowWithNotifications(DataQuery dataQuery) {
            return Observable.create(new DataAndNotificationsOnSubscribe<>(dataWindowSource, dataQuery, Object.class, watcher));
        }

        /**
         * Observable data window, items type of which is known and same. Returned data is
         * either Map<String,Object> or String
         *
         * @param dataQuery determines window for firebase database data
         * @return observable data for window
         */

        public <T> Observable<Window<T>> window(final DataQuery dataQuery, Class<T> type) {
            return Observable.create(new DataOnSubscribe<>(dataWindowSource, dataQuery, type));
        }

        /**
         * Observable data window, items type of which is either unknown or changing. Returned data is
         * either Map<String,Object> or String
         *
         * @param dataQuery determines window for firebase database data
         * @return observable data for window
         */
        public Observable<Window<Object>> window(DataQuery dataQuery) {
            return window(dataQuery, Object.class);
        }

        /**
         * Update children of provided database reference with given child updates
         *
         * @return write completion notification observable
         */
        public Observable<WriteResult> updateChildren(final Map<String, Object> childUpdates) {
            return Observable.unsafeCreate(new OnSubscribe<WriteResult>() {
                @Override
                public void call(final Subscriber<? super WriteResult> subscriber) {
                    dbRef.updateChildren(childUpdates, completionListener(subscriber));
                }
            });
        }

        /**
         * Set value of provided database reference to given object
         *
         * @return write completion notification observable
         */
        public Observable<WriteResult> setValue(final Object object) {
            return Observable.unsafeCreate(new OnSubscribe<WriteResult>() {
                @Override
                public void call(final Subscriber<? super WriteResult> subscriber) {
                    dbRef.setValue(object, completionListener(subscriber));
                }
            });
        }

        /**
         * remove value associated with current database reference
         *
         * @return write completion notification observable
         */
        public Observable<WriteResult> removeValue() {
            return Observable.unsafeCreate(new OnSubscribe<WriteResult>() {
                @Override
                public void call(Subscriber<? super WriteResult> subscriber) {
                    dbRef.removeValue(completionListener(subscriber));
                }
            });
        }

        /**
         * SHOULD be called if client is not interested (not subscribing) to data update notifications
         * for {@link #windowWithNotifications(DataQuery)}
         */
        public void releaseNotifications(WindowWithNotifications<?> windowWithNotifications) {
            watcher.releaseNotifications(windowWithNotifications);
        }

        private static DatabaseReference.CompletionListener completionListener(
                final Subscriber<? super WriteResult> subscriber) {

            return new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError,
                                       DatabaseReference databaseReference) {

                    if (!subscriber.isUnsubscribed()) {
                        if (databaseError == null) {
                            subscriber.onNext(new WriteResult(databaseReference));
                            subscriber.onCompleted();
                        } else {
                            subscriber.onError(new FirebaseDataException(databaseError));
                        }
                    }
                }
            };
        }

    }

    public interface DbRefFunc {
        DatabaseReference map(DatabaseReference ref);
    }

}
