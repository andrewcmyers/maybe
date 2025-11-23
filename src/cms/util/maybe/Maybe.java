package cms.util.maybe;

import cms.util.UnsafeUtils;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.NoSuchElementException;

/// An object that may contain a value of type {@code T}. Similar to
/// Java's Optional class but uses a fast checked exception instead of
/// a slow unchecked exception. It can also act like a {@code Set<T>}
/// containing at most one element. It has exactly two implementations,
/// {@code None} and {@code Some}. There are several ways to condition behavior
/// on which kind of Maybe one has, including:
///    - Call `get()` and handling the fast `NoMaybeValue` exception.
///    - The monadic methods `then`/`thenMaybe`/`else`/`elseGet` avoid exceptions.
///    - An enhanced `for`-loop over the Maybe, which does nothing on empty Maybe.
///    - Pattern-matching against {@code Some(T value)}.
@SuppressWarnings ({
        "OptionalUsedAsFieldOrParameterType", // We want interop with Optionals
        "NullableProblems",                   // IntelliJ complains about lack of `@NotNull`,
                                              // but we don't use IntelliJ annotations
        "unused" // Unused methods may be useful in the future
})
public sealed interface Maybe<T> extends Set<T> permits Maybe.None, Maybe.Some {

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
     * Create an {@code Optional} from a {@code Maybe}, which is useful
     * when a stream is needed.
     */
    default Optional<T> toOptional() {
        return then(Optional::of).orElse(Optional.empty());
    }

    /** Get the value in an {@code Optional}, if present; otherwise
     *  throw the checked exception {@code NoMaybeValue}. This
     *  method allows {@code Optional}s to be used as if they were Maybe.
     *  @return The value in the optional, if any
     *  @throws NoMaybeValue if no value is present.
     */
    static <T> T getOptional(Optional<? extends T> optional) throws NoMaybeValue {
        if (optional.isPresent()) return optional.get();
        else throw NoMaybeValue.theException;
    }

    /**
     * Create a {@link Maybe} from the result of a computation,
     * returning {@code Maybe.None} if the computation returns null
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
     * @return a {@code Maybe.Some} instance containing the result of {@code valueOrThrow},
     *         or {@code Maybe.None} if the {@code valueOrThrow} throws.
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
    @SuppressWarnings ("unchecked")
    static <T> Maybe<T> none() {
        return (Maybe<T>) None.theNone;
    }

    /** Convert a {@code Maybe<U>} to a {@code Maybe<T>}, when {@code T}
     *  is a supertype of {@code U}. This covariant subtyping is safe because
     *  {@code Maybe}s are immutable.
     */
    @SuppressWarnings("unchecked")
    static <T, U extends T> Maybe<T> cast(Maybe<U> in) {
        return (Maybe<T>) in;
    }

    /** Representation of an empty Maybe. It should be created using the
     * method {@code Maybe.none()}.
     */
    public static final class None<T> implements Maybe<T> {
        private None() { // Use Maybe.none() instead
        }

        @SuppressWarnings("rawtypes")
        private static final None theNone = new None();

        @Override
        public boolean isPresent() {
            return false;
        }

        @Override
        public T get() throws NoMaybeValue {
            throw NoMaybeValue.theException;
        }

        @Override
        public T orElse(T other) {
            return other;
        }
        
        @Override
        public T orElseGet(Supplier<? extends T> other) {
            return other.get();
        }

        @Override
        public <E extends Exception> T orElseThrow(Supplier<E> throwable) throws E {
            throw throwable.get();
        }

        @Override
        public <U> Maybe<U> thenMaybe(Function<? super T, ? extends Maybe<? extends U>> f) {
            return Maybe.none();
        }

        @Override
        public <U> Maybe<U> then(Function<? super T, ? extends U> f) {
            return Maybe.none();
        }

        @Override
        public void thenDo(Consumer<? super T> cons) {
        }

        @Override
        public void thenElse(Consumer<? super T> consThen, Runnable procElse) {
            procElse.run();
        }

        @Override
        public Maybe<T> onlyIf(Predicate<? super T> condition) {
            return Maybe.none();
        }

        @Override
        public Iterator<T> iterator() {
            return new Iterator<>() {
                public T next() { throw new NoSuchElementException(); }
                public boolean hasNext() { return false; }
            };
        }

        @Override
        public Object[] toArray() {
            return new Object[0];
        }

        @Override
        public <T1> T1[] toArray(T1[] a) {
            Arrays.fill(a, null); // See spec of toArray in Set
            return a;
        }

        @Override
        public boolean add(T t) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean addAll(Collection<? extends T> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return false;
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean contains(Object elem) {
            return false;
        }

        @Override
        public Maybe<T> orElseMaybe(Supplier<? extends Maybe<? extends T>> other) {
            return Maybe.cast(other.get());
        }
        
        @Override
        public String toString() {
            return "none";
        }
        
        @Override
        public int hashCode() {
            return 0;
        }
    }

    /** Representation of a Maybe containing a value of type {@code T}.
     * <p>
     * It can conveniently used for pattern-matching discrimination,
     * e.g., {@code if (m instanceof Some(T value)) { ... }}
     */
    public record Some<T>(T value) implements Maybe<T> {
        @Override
        public boolean isPresent() {
            return true;
        }

        @Override
        public T get() {
            return value;
        }
        
        @Override
        public T orElse(T other) {
            return value;
        }
        
        @Override
        public T orElseGet(Supplier<? extends T> other) {
            return value;
        }

        @Override
        public <E extends Exception> T orElseThrow(Supplier<E> throwable) {
            return value;
        }

        @Override
        public <U> Maybe<U> thenMaybe(Function<? super T, ? extends Maybe<? extends U>> f) {
            return Maybe.cast(f.apply(value));
        }

        @Override
        public <U> Maybe<U> then(Function<? super T, ? extends U> f) {
            return Maybe.some(f.apply(value));
        }

        @Override
        public void thenDo(Consumer<? super T> cons) {
            cons.accept(value);
        }

        @Override
        public void thenElse(Consumer<? super T> consThen, Runnable procElse) {
            consThen.accept(value);
        }

        @Override
        public Maybe<T> onlyIf(Predicate<? super T> condition) {
            if (condition.test(value)) {
                return this;
            } else {
                return Maybe.none();
            }
        }

        @Override
        public Iterator<T> iterator() {
            return new Iterator<>() {
                boolean yielded = false;
                public T next() {
                    if (yielded) throw new NoSuchElementException();
                    yielded = true;
                    return value;
                }
                public boolean hasNext() {
                    return !yielded;
                }
            };
        }

        @Override
        public Object[] toArray() {
            return new Object[0];
        }

        @Override
        public <T1> T1[] toArray(T1[] a) {
            List<T> list = new ArrayList<>(1);
            list.add(value);
            return list.toArray(a);
        }

        @Override
        public boolean add(T t) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            for (Object o : c) {
                if (!contains(o)) return false;
            }
            return true;
        }

        @Override
        public boolean addAll(Collection<? extends T> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean contains(Object elem) {
            return value.equals(elem);
        }

        @Override
        public Maybe<T> orElseMaybe(Supplier<? extends Maybe<? extends T>> other) {
            return this;
        }
        
        @Override
        public String toString() {
            return value.toString();
        }
    }

    /** Creates a Maybe from a non-null argument.
     * @param v must be non-null
     * @throws IllegalArgumentException if a null value is passed to it.
     */
    static <T> Maybe<T> some(T v) {
        if (v == null) {
            throw new IllegalArgumentException("Maybe.some() requires a non-null argument");
        }
        return new Maybe.Some<>(v);
    }
}
