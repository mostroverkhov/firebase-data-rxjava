package com.github.mostroverkhov.firebase_rx_data.model;

/**
 * Event for data window change notifications
 */
public class DataWindowChangeEvent<T> {

    private final String prevChildName;
    private final Kind kind;
    private final T item;

    public DataWindowChangeEvent(T item, Kind kind, String prevChildName) {

        if (item == null || kind == null || prevChildName == null) {
            throw new IllegalArgumentException("Args should not be null");
        }

        this.item = item;
        this.kind = kind;
        this.prevChildName = prevChildName;
    }

    public DataWindowChangeEvent(T item, Kind kind) {
        this(item, kind, "");
    }

    public boolean hasPrevChildName() {
        return !"".equals(prevChildName);
    }

    public T getItem() {
        return item;
    }

    public String getPrevChildName() {
        return prevChildName;
    }

    public Kind getKind() {
        return kind;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DataWindowChangeEvent{");
        sb.append("prevChildName='").append(prevChildName).append('\'');
        sb.append(", kind=").append(kind);
        sb.append(", item=").append(item);
        sb.append('}');
        return sb.toString();
    }

    public enum Kind {
        ADDED,
        CHANGED,
        MOVED,
        REMOVED
    }
}
