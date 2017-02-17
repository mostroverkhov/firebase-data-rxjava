package com.github.mostroverkhov.firebase_data_rxjava.datawindowsource.model;

import com.github.mostroverkhov.firebase_data_rxjava.datawindowsource.DataWindowSource;

import java.util.List;

/**
 * Created by Maksym Ostroverkhov on 19.07.2016.
 */

/**
 * Window into data for given data query, represented as list of typed elements
 * Used by {@link DataWindowSource}
 */
public class DataWindowResult<T> extends NextQuery implements HasDataWindow<T> {

    private final List<T> data;

    public DataWindowResult(List<T> data, DataQuery next) {
        super(next);
        this.data = data;
    }

    @Override
    public List<T> getData() {
        return data;
    }


}
