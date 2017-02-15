package com.github.mostroverkhov.firebase_rx_data.lib;

import com.github.mostroverkhov.firebase_rx_data.model.HasDataWindow;

/**
 * Created by Maksym Ostroverkhov on 19.07.2016.
 */

/**
 * Callback window data
 */
public interface DataCallback<V, T extends HasDataWindow<V>> extends ErrorCallback {

    void onData(T result);
}
