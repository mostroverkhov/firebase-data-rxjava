package com.github.mostroverkhov.datawindowsource.util;

import com.github.mostroverkhov.datawindowsource.DataWindowSource;
import com.github.mostroverkhov.datawindowsource.model.WindowChangeEvent;

/**
 * Created with IntelliJ IDEA.
 * Author: mostroverkhov
 */
public class KeyAndKind {

    private final String key;
    private final WindowChangeEvent.Kind kind;

    public KeyAndKind(String key, WindowChangeEvent.Kind kind) {
        this.key = key;
        this.kind = kind;
    }

    public String getKey() {
        return key;
    }

    public WindowChangeEvent.Kind getKind() {
        return kind;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KeyAndKind that = (KeyAndKind) o;

        if (!key.equals(that.key)) return false;
        return kind == that.kind;

    }

    @Override
    public int hashCode() {
        int result = key.hashCode();
        result = 31 * result + kind.hashCode();
        return result;
    }
}
