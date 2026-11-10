package io.github.regalpine.agentforge.provider.spi.cancellation;

public final class OperationCancelledException
    extends RuntimeException {

    public OperationCancelledException(
        String message
    ) {
        super(message);
    }
}
