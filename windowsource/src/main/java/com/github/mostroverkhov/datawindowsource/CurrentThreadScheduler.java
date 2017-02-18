package com.github.mostroverkhov.datawindowsource;

/**
 * Created by Maksym Ostroverkhov on 18.02.2017.
 */
public final class CurrentThreadScheduler implements Scheduler {

    private static volatile CurrentThreadScheduler instance;

    private CurrentThreadScheduler() {
    }

    public static CurrentThreadScheduler getInstance() {
        if (instance == null) {
            synchronized (CurrentThreadScheduler.class) {
                if (instance == null) {
                    instance = new CurrentThreadScheduler();
                }
            }
        }
        return instance;
    }

    @Override
    public void execute(Runnable action) {
        action.run();
    }
}
