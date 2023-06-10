package cms.util;

/**
 * Exceptions that extend this class will be logged differently to regular exceptions.
 */
public abstract class FastException extends Exception {
    protected FastException() {}

    protected abstract FastException create();

    public FastException withStackTrace() {
        FastException e = create();
        return (FastException) e.customFillInStackTrace();
    }

    /**
     * There is no meaningful stack trace for this exception because it is supposed to always be handled.
     */
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

    public synchronized Throwable customFillInStackTrace() {
        return super.fillInStackTrace();
    }
}
