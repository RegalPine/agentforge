package io.github.regalpine.agentforge.distributed.worker;

import io.github.regalpine.agentforge.runtime.ExecutionTask;

public interface Worker {

    WorkerId id();

    void start();

    void stop();

    void execute(
        ExecutionTask task
    );
}
