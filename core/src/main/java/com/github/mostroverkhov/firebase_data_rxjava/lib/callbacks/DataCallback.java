package com.github.mostroverkhov.firebase_data_rxjava.lib.callbacks;

import com.github.mostroverkhov.firebase_data_rxjava.model.HasDataWindow;

/**
 * Created by Maksym Ostroverkhov on 19.07.2016.
 */

/**
 * Callback for data window
 */
public interface DataCallback<V, T extends HasDataWindow<V>> extends ErrorCallback {

    void onData(T result);
}
