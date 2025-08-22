package cms.util.maybe;

import cms.util.Monad;

import java.util.function.Function;

/** Exposing Maybe as a monad. */
public class MaybeMonad<T> implements Monad<Maybe<T>, T> {
    @Override
    public Maybe<T> unit(T value) {
        return Maybe.some(value);
    }

    @Override
    public Function<Maybe<T>, Maybe<T>> bind(Function<T, Maybe<T>> rest) {
        return m -> m.thenMaybe(rest);
    }
}