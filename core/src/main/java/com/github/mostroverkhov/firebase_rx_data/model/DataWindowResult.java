package com.github.mostroverkhov.firebase_rx_data.model;

import com.github.mostroverkhov.firebase_rx_data.model.HasDataWindow;
import com.github.mostroverkhov.firebase_rx_data.model.NextQuery;
import com.github.mostroverkhov.firebase_rx_data.model.DataQuery;

import java.util.List;

/**
 * Created by Maksym Ostroverkhov on 19.07.2016.
 */

/**
 * Data window result
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
