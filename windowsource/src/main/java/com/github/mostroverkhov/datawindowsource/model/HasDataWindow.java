package com.github.mostroverkhov.datawindowsource.model;

import java.util.List;

/**
 * Created by Maksym Ostroverkhov on 19.07.2016.
 */
public interface HasDataWindow<T> {

    List<T> getData();
}
