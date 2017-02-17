package com.github.mostroverkhov.firebase_data_rxjava.model;

import java.util.List;

/**
 * Created by Maksym Ostroverkhov on 19.07.2016.
 */
public interface HasDataWindow<T> {

    List<T> getData();
}
