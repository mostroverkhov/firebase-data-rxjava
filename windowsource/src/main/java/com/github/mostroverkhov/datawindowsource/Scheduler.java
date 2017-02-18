package com.github.mostroverkhov.datawindowsource;

/**
 * Created by Maksym Ostroverkhov on 18.02.2017.
 */
public interface Scheduler {

    void execute(Runnable action);
}
