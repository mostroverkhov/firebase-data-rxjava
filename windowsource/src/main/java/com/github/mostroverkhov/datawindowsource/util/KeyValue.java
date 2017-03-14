package com.github.mostroverkhov.datawindowsource.util;

/**
 * Created with IntelliJ IDEA.
 * Author: mostroverkhov
 */
public class KeyValue<T> {
    private final String key;
    private final T value;

    public KeyValue(String key, T value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public T getValue() {
        return value;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DataItem{");
        sb.append("key='").append(key).append('\'');
        sb.append(", value=").append(value);
        sb.append('}');
        return sb.toString();
    }
}
