package io.github.regalpine.agentforge.runtime;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LocalScheduler
    implements Scheduler, AutoCloseable {

    private final ExecutorService executor;

    public LocalScheduler() {
        this.executor =
            Executors.newFixedThreadPool(4);
    }

    @Override
    public void submit(
        ExecutionTask task
    ) {
        executor.submit(
            () -> {
                // M2: delegate to runtime task handler
            }
        );
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}
