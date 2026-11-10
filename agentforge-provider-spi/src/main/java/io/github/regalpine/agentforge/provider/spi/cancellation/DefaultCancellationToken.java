package io.github.regalpine.agentforge.provider.spi.cancellation;

import java.util.concurrent.atomic.AtomicBoolean;

public final class DefaultCancellationToken
    implements CancellationToken {

    private final AtomicBoolean cancelled =
        new AtomicBoolean(false);

    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }

    @Override
    public void throwIfCancelled() {
        if (cancelled.get()) {
            throw new OperationCancelledException(
                "Operation was cancelled"
            );
        }
    }

    public void cancel() {
        cancelled.set(true);
    }
}
