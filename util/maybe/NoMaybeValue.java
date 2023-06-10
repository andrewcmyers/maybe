package cms.util.maybe;

import cms.util.FastException;

/**
 * A checked exception that forces the programmer to handle the possibility
 * of empty maybes. This exception is designed to be efficient and to be used
 * intentionally by programmers in non-error situations.
 */
public class NoMaybeValue extends FastException {
    // There is only one exception object because there is no real value in
    // building a stack trace for checked exceptions.
    public static NoMaybeValue theException = new NoMaybeValue();

    @Override
    protected FastException create() {
        return new NoMaybeValue();
    }
}
