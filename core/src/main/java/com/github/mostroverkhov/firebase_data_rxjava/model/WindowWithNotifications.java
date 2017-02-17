package com.github.mostroverkhov.firebase_data_rxjava.model;

import java.util.List;

import rx.Observable;

/**
 * Created by Maksym Ostroverkhov on 07.07.2016.
 */

/**
 * Data window for database, together with notifications observable for changes to this data
 *
 * @param <T> type of item for data in window
 */
public class WindowWithNotifications<T> {

    private final List<T> window;
    private final Observable<DataWindowChangeEvent> windowNotifications;
    private final DataQuery dataQuery;

    public WindowWithNotifications(List<T> window,
                                   Observable<DataWindowChangeEvent> windowNotifications,
                                   DataQuery dataQuery) {
        if (window == null || windowNotifications == null || dataQuery == null) {
            throw new IllegalArgumentException("Args should not be null");
        }
        this.window = window;
        this.windowNotifications = windowNotifications;
        this.dataQuery = dataQuery;
    }

    /**
     * @return list for items for data window
     */
    public List<T> dataWindow() {
        return window;
    }

    /**
     * @return data change notifications {@link Observable} for data window
     */
    public Observable<DataWindowChangeEvent> dataWindowNotifications() {
        return windowNotifications;
    }

    /**
     * @return data query associated with given data window
     */
    public DataQuery getDataQuery() {
        return dataQuery;
    }
}
