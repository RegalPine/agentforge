package io.github.regalpine.agentforge.provider.spi.cancellation;

public interface CancellationToken {

    boolean isCancelled();

    void throwIfCancelled();
}
