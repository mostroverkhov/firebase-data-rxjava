package com.github.mostroverkhov.firebase_rx_data.model;

import java.util.List;

/**
 * Created by Maksym Ostroverkhov on 19.07.2016.
 */
public class Window<T> {

    private final List<T> dataWindow;
    private final DataQuery dataQuery;

    public Window(List<T> dataWindow, DataQuery dataQuery) {
        this.dataWindow = dataWindow;
        this.dataQuery = dataQuery;
    }

    public List<T> dataWindow() {
        return dataWindow;
    }

    public DataQuery getDataQuery() {
        return dataQuery;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Window<?> window = (Window<?>) o;

        if (!dataWindow.equals(window.dataWindow)) return false;
        return dataQuery.equals(window.dataQuery);

    }

    @Override
    public int hashCode() {
        int result = dataWindow.hashCode();
        result = 31 * result + dataQuery.hashCode();
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Window{");
        sb.append("dataWindow=").append(dataWindow);
        sb.append(", dataQuery=").append(dataQuery);
        sb.append('}');
        return sb.toString();
    }
}
