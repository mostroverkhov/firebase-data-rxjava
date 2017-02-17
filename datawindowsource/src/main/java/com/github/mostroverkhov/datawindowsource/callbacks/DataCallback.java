package com.github.mostroverkhov.datawindowsource.callbacks;


/**
 * Created by Maksym Ostroverkhov on 19.07.2016.
 */

import com.github.mostroverkhov.datawindowsource.model.HasDataWindow;

/**
 * Callback for data window
 */
public interface DataCallback<V, T extends HasDataWindow<V>> extends ErrorCallback {

    void onData(T result);
}
