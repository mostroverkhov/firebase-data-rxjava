package com.github.mostroverkhov.datawindowsource;

import java.util.concurrent.Executor;

/**
 * Created by Maksym Ostroverkhov on 18.02.2017.
 */
public class ExecutorScheduler implements Scheduler {
    private final Executor executor;

    public ExecutorScheduler(Executor executor) {
        this.executor = executor;
    }

    @Override
    public void execute(Runnable action) {
        executor.execute(action);
    }
}
