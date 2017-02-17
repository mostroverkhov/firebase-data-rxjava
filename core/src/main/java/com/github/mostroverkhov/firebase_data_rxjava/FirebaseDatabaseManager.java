package com.github.mostroverkhov.firebase_data_rxjava;

import com.github.mostroverkhov.firebase_data_rxjava.model.DataQuery;
import com.github.mostroverkhov.firebase_data_rxjava.model.Window;
import com.github.mostroverkhov.firebase_data_rxjava.model.WindowWithNotifications;
import com.github.mostroverkhov.firebase_data_rxjava.model.WriteResult;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import java.util.Map;

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
    private final DataNotificationsWatcher watcher = new DataNotificationsWatcher();

    public FirebaseDatabaseManager(DatabaseReference root) {
        this.root = root;
    }

    /**
     * @param dbRefFunc function to transform root reference
     * @return entry point for firebase database rx capabilities
     */
    public Data data(DbRefFunc dbRefFunc) {
        return new Data(dbRefFunc.map(root), watcher);
    }

    /**
     * @return entry point for firebase database
     */
    public Data data() {
        return new Data(root, watcher);
    }

    public static class Data {
        private final DatabaseReference dbRef;
        private final DataNotificationsWatcher watcher;

        Data(DatabaseReference dbRef, DataNotificationsWatcher watcher) {
            this.dbRef = dbRef;
            this.watcher = watcher;
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
            return Observable.create(new DataAndNotificationsOnSubscribe<>(dataQuery, itemType, watcher));
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
            return Observable.create(new DataAndNotificationsOnSubscribe<>(dataQuery, Object.class, watcher));
        }

        /**
         * Observable data window, items type of which is known and same. Returned data is
         * either Map<String,Object> or String
         *
         * @param dataQuery determines window for firebase database data
         * @return observable data for window
         */

        public <T> Observable<Window<T>> window(final DataQuery dataQuery, Class<T> type) {
            return Observable.create(new DataOnSubscribe<>(dataQuery, type));
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
            return Observable.create(new OnSubscribe<WriteResult>() {
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
            return Observable.create(new OnSubscribe<WriteResult>() {
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
            return Observable.create(new OnSubscribe<WriteResult>() {
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
