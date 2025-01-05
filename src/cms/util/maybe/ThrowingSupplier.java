package cms.util.maybe;

/**
 * A function that takes no arguments and either returns a value of type {@code <T>}
 * or throws an exception of type {@code <E>}
 */
@FunctionalInterface
public interface ThrowingSupplier<T, E extends Throwable> {
    T get() throws E;
}
