package com.github.mostroverkhov.firebase_data_rxjava.datawindowsource.model;

import com.google.firebase.database.DatabaseReference;

/**
 * Created by Maksym Ostroverkhov on 07.07.2016.
 */

/**
 * Data query which determines window into firebase database data. To create one clients should
 * use {@link DataQuery.Builder}
 */
public class DataQuery {

    private final DatabaseReference dbRef;
    private final int windowSize;
    private final OrderDirection orderDir;
    private final OrderBy orderBy;
    private final String orderByChildKey;
    private final String windowStartWith;

    DataQuery(DatabaseReference dbRef,
              int windowSize,
              OrderDirection orderDir,
              OrderBy orderBy,
              String orderByChildKey,
              String windowStartWith) {

        this.dbRef = dbRef;
        this.windowSize = windowSize;
        this.orderDir = orderDir;
        this.orderBy = orderBy;
        this.orderByChildKey = orderByChildKey;
        this.windowStartWith = windowStartWith;
    }

    public DatabaseReference getDbRef() {
        return dbRef;
    }

    public int getWindowSize() {
        return windowSize;
    }

    public OrderDirection getOrderDir() {
        return orderDir;
    }

    public OrderBy getOrderBy() {
        return orderBy;
    }

    public String orderByChildKey() {
        return orderByChildKey;
    }

    public String getWindowStartWith() {
        return windowStartWith;
    }

    public boolean isFirst() {
        return windowStartWith == null;
    }

    public boolean isLast() {
        return "".equals(windowStartWith);
    }

    /**
     * @param dataQuery       data query for current window
     * @param windowStartWith starting value for next window. Actual value is determined by {@link OrderBy}
     * @return {@link DataQuery} for next window
     */
    public static DataQuery next(DataQuery dataQuery, String windowStartWith) {

        return new DataQuery(dataQuery.getDbRef(),
                dataQuery.getWindowSize(),
                dataQuery.getOrderDir(),
                dataQuery.getOrderBy(),
                dataQuery.orderByChildKey(),
                windowStartWith);
    }

    /**
     * @param dataQuery current data query
     * @return {@link DataQuery} for last window
     */
    public static DataQuery last(DataQuery dataQuery) {

        return new DataQuery(dataQuery.getDbRef(),
                dataQuery.getWindowSize(),
                dataQuery.getOrderDir(),
                dataQuery.getOrderBy(),
                dataQuery.orderByChildKey(),
                "");
    }

    /**
     * @param dbRef      database reference
     * @param windowSize size of data window
     * @param orderDir   order direction
     * @param orderBy    order by value
     * @param key        only used for {@link OrderBy#CHILD}
     * @return {@link DataQuery} for first window
     */
    public static DataQuery first(DatabaseReference dbRef,
                                  int windowSize,
                                  OrderDirection orderDir,
                                  OrderBy orderBy,
                                  String key) {

        return new DataQuery(dbRef,
                windowSize,
                orderDir,
                orderBy,
                key,
                null);
    }

    public static class Builder {

        private final DatabaseReference dbRef;
        private int windowSize = 25;
        private OrderDirection orderDir = OrderDirection.ASC;
        private OrderBy orderBy = OrderBy.KEY;
        private String key;

        public Builder(DatabaseReference dbRef) {
            this.dbRef = dbRef;
        }

        /**
         * @param size window size. Should be positive value
         */
        public Builder windowWithSize(int size) {
            if (size < 1) {
                throw new IllegalArgumentException("Size should be positive");
            }
            this.windowSize = size;
            return this;
        }

        /**
         * @param key child property name used for ordering
         */
        public Builder orderByChild(String key) {
            if (key == null) {
                throw new IllegalArgumentException("key should not be null");
            }
            this.key = key;
            orderBy = OrderBy.CHILD;
            return this;
        }

        /**
         * order by child key
         */
        public Builder orderByKey() {
            orderBy = OrderBy.KEY;
            this.key = null;
            return this;
        }

        /**
         * order by child value
         */
        public Builder orderByValue() {
            orderBy = OrderBy.VALUE;
            this.key = null;
            return this;
        }

        /**
         * ascending order for data windows
         */
        public Builder asc() {
            orderDir = OrderDirection.ASC;
            return this;
        }

        /**
         * descending order fo data windows
         */
        public Builder desc() {
            orderDir = OrderDirection.DESC;
            return this;
        }

        /**
         * @return new {@link DataQuery}
         */
        public DataQuery build() {
            return new DataQuery(dbRef, windowSize, orderDir, orderBy, key, null);
        }
    }

    public enum OrderBy {
        CHILD,
        KEY,
        VALUE
    }

    public enum OrderDirection {
        ASC,
        DESC
    }
}
