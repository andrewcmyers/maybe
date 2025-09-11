package cms.util.maybe;

import cms.util.UnsafeUtils;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** An object that may contain a value of type {@code T}. Similar to
 *  Java's Optional class but uses a fast checked exception instead of
 *  a slow unchecked exception.
 */
@SuppressWarnings ({
        "OptionalUsedAsFieldOrParameterType", // We want interop with Optionals
        "unused" // Unused methods may be useful in the future
})
public sealed interface Maybe<T> extends Set<T> permits Maybes.None, Maybes.Some {

    /** Returns whether a value is contained in this {@code Maybe}.
     *  @return whether a value is contained. */
    boolean isPresent();

    /// Returns the contained value, if present. Otherwise, throws the exception.
    ///
    /// This method is primarily useful for writing code that is supposed to
    /// perform side effects, e.g.:
    /// ```
    /// Maybe<T> m = ...
    /// try {
    ///     ... m.get() ...
    /// } catch (NoMaybeValue e) {
    ///     ...
    /// }
    /// ```
    ///
    /// @return the contained value
    /// @throws NoMaybeValue if no value is contained in this Maybe.
    T get() throws NoMaybeValue;

    /// If a value `v` is present, returns `f(v)`. Otherwise, returns an empty `Maybe`.
    /// (This is a monadic bind.)
    ///
    /// This method is useful for chaining together a series of `Maybe` accesses. For
    /// example, supposing `T.foo()` returns a Maybe:
    /// ```
    /// Maybe<T> mt = ...
    /// mt.thenMaybe(t -> t.foo().then(f -> ...))
    ///     .orElse(...)
    /// ```
    ///
    /// @param f The function to be applied to the contained value, if any.
    /// @param <U> The type of the value that may be returned by the function `f`.
    /// @return the `Maybe` returned by `f` if a value is contained in this `Maybe`.
    ///         Otherwise, an empty `Maybe`.
    <U> Maybe<U> thenMaybe(Function<? super T, ? extends Maybe<? extends U>> f);

    /// If a value `v` is present, returns a `Maybe` containing `f(v)`, which must be non-null.
    ///  Otherwise, returns an empty `Maybe`. (This is a monadic bind composed with a monadic unit.)
    ///
    ///  This method can be used conveniently along with orElse to handle both
    ///  maybe cases, e.g.:
    /// ```
    /// Maybe<T> mt = ...
    /// mt.then(t -> ...)
    ///   .orElse(...)
    /// ```
    ///
    /// @param f The function to be applied to the contained value, if any.
    /// @param <U> The type of the value that may be returned by the function `f`.
    /// @return a `Maybe` containing the value `f`, if a value is contained in this
    ///         `Maybe`. Otherwise, an empty `Maybe`.
    <U> Maybe<U> then(Function<? super T, ? extends U> f);

    /// If a value `v` is present and `v` satisfies `condition`,
    /// returns a `Maybe` containing `v`.
    /// If not, returns `Maybe.none()`.
    ///
    /// Example:
    /// ```
    /// var maybeX = some(10);
    /// var maybeY = some(9);
    /// maybeX.onlyIf(x -> x % 2 == 0); // returns some(10)
    /// maybeY.onlyIf(y -> y % 2 == 0); // returns none
    /// ```
    ///
    /// @param condition the condition to check on the value
    /// @return a `Maybe` containing the value contained in this `Maybe`,
    ///         if one exists and meets `condition`.
    Maybe<T> onlyIf(Predicate<? super T> condition);

    /** Returns the contained value, if any; otherwise, returns {@code other}.
     *  Note: since orElse is an ordinary method call, its argument is always computed,
     *  unlike a Java {@code else} statement. If the argument is
     *  expensive to compute or has side effects, {@code orElseGet()} should
     *  be used instead.
     *
     *  @param other The value to be returned if no value is in this {@code Maybe}.
     *  @return The contained value, or an empty {@code Maybe}.
     */
    T orElse(T other);

    /** Returns the contained value, if any; otherwise, returns {@code other.get()}.
     *  @param other The function to use when this {@code Maybe} is empty.
     *  @return the contained value or {@code other.get()}.
     */
    T orElseGet(Supplier<? extends T> other);

    /** Returns the contained value, if any; otherwise, throw the supplied exception. */
    <E extends Exception> T orElseThrow(Supplier<E> throwable) throws E;

    /** Returns this if a value is contained; otherwise, returns {@code other.get()}.
     *  @param other The function to use when this {@code Maybe} is empty.
     *  @return this or {@code other.get()}.
     */
    Maybe<T> orElseMaybe(Supplier<? extends Maybe<? extends T>> other);

    /** Call {@code cons} on the contained value, if any.
     *  @param cons The function to send the contained value to.
     */
    void thenDo(Consumer<? super T> cons);

    /** If a value is contained, run consThen on the value; otherwise run procElse */
    void thenElse(Consumer<? super T> consThen, Runnable procElse);

    /// Provide an iterator that yields either one `T` or none, depending.
    @SuppressWarnings ("NullableProblems") // IntelliJ complains about lack of `@NotNull`,
                                           // but we don't use IntelliJ annotations
    @Override
    Iterator<T> iterator();

    @Override
    default boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) return false;
        }
        return true;
    }
    //----- static methods -----

    /** Create a Maybe from a possibly null value v. The
     *  Maybe will contain a value if v is non-null.
     *  @param v The value to put into a {@code Maybe}, or null.
     *  @param <T> The type of {@code Maybe} to be created.
     */
    static <T> Maybe<T> from(T v) {
        return (v == null)
               ? none()
               : some(v);
    }

    /**
     * Create a {@code Maybe} from an {@code Optional} value.
     * @param optional The {@code Optional} value.
     * @param <T> The type of the {@code Maybe} to be created.
     * @return If the {@code Optional} contains a value, then Some of that value, otherwise None.
     */
    static <T> Maybe<T> fromOptional(Optional<? extends T> optional) {
        return Maybe.cast(optional.map(Maybe::some).orElseGet(Maybe::none));
    }
    /**
     * Create an {@code Optional} from a {@code Maybe}
     */
    default Optional<T> toOptional() {
        return then(Optional::of).orElse(Optional.empty());
    }

    /** Get the value in an {@code Optional}, if present; otherwise
     *  throw the checked exception {@code NoMaybeValue}. This
     *  method allows {@code Optional}s to be used as if they were Maybes.
     *  @return The value in the optional, if any
     *  @throws NoMaybeValue if no value is present.
     */
    static <T> T getOptional(Optional<? extends T> optional) throws NoMaybeValue {
        if (optional.isPresent()) return optional.get();
        else throw NoMaybeValue.theException;
    }

    /**
     * Create a {@link Maybe} from the result of a computation,
     * returning {@code Maybes.None} if the computation returns null
     * or throws a particular exception.
     * Note that this method only catches the specified exception.
     * If the computation throws any other exception, it is propagated.
     * <p>
     * As an example, the following code:<br>
     * <pre>
     * {@code
     * Maybe<Float> oldScoreClient;
     * try {
     *     oldScoreClient = Maybe.some(Float.parseFloat(vals[5]));
     * } catch (NumberFormatException e) {
     *     oldScoreClient = Maybe.none();
     * }}
     * </pre>
     * could be rewritten as:<br>
     * <pre>
     * {@code final Maybe<Float> oldScoreClient = Maybe.fromCatching(() -> Float.parseFloat(vals[5]), NumberFormatException.class)}
     * </pre>
     * @param valueOrThrow a computation, represented by a {@link ThrowingSupplier} that either
     *                     returns a value to store in this maybe or throws an exception.
     * @param exnClass the exception that {@code valueOrThrow} might throw.
     * @return a {@code Maybes.Some} instance containing the result of {@code valueOrThrow},
     *         or {@code Maybes.None} if the {@code valueOrThrow} throws.
     * @param <T> the type parameter of the resulting {@link Maybe}
     * @param <E> the type of the exception being caught
     */
    static <T, E extends Throwable> Maybe<T> fromCatching(ThrowingSupplier<T, E> valueOrThrow, Class<? super E> exnClass) {
        try {
            return from(valueOrThrow.get());
        } catch (Throwable e) {
            if (exnClass.isInstance(e)) {
                return none();
            } else {
                UnsafeUtils.unsafeThrowUnchecked(e); // Propagate the exception.
                                                     // It's not of type E, so it wasn't checked before,
                                                     // and we don't want to make it checked now.
                                                     // Must use this unsafe method because the compiler
                                                     // doesn't know that e is not of type E (which we checked above).
                throw new IllegalStateException("Impossible to reach this point in the code; unsafeThrowUnchecked always throws");
            }
        }
    }

    /** Returns an empty {@code Maybe}. */
    static <T> Maybe<T> none() {
        return Maybes.none();
    }

    /** Creates a {@code Maybe} from a non-null argument.
     * @param v must be non-null
     * @throws IllegalArgumentException if a null value is passed to it.
     */
    static <T> Maybe<T> some(T v) {
        return Maybes.some(v);
    }

    /** Convert a {@code Maybe<U>} to a {@code Maybe<T>}, when {@code T}
     *  is a supertype of {@code U}. This covariant subtyping is safe because
     *  {@code Maybe}s are immutable.
     */
    @SuppressWarnings("unchecked")
    static <T, U extends T> Maybe<T> cast(Maybe<U> in) {
        return (Maybe<T>) in;
    }
}
