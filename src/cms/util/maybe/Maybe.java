package cms.util.maybe;

import cms.util.UnsafeUtils;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** An object that may contain a value of type {@code T}. Similar to
 *  Java's Optional class but uses a fast checked exception instead of
 *  a slow unchecked exception.
 */
public abstract class Maybe<T> implements Set<T> {

    /** Returns whether a value is contained in this {@code Maybe}.
     *  @return whether a value is contained. */
    public abstract boolean isPresent();

    /** Returns the contained value, if present. Otherwise, throws the exception.
     * @return the contained value
     * @throws NoMaybeValue if no value is contained in this Maybe. This method
     * is primarily useful for writing code that is supposed to perform side
     * effects, e.g.:
     * <pre>
     * Maybe{@code<T>} m = ...
     * try {
     *    ... m.get() ...
     * } catch (NoMaybeValue e) {
     *    ...
     * }
     * </pre>
     *    
     */
    public abstract T get() throws NoMaybeValue;

    /** If a value {@code v} is present, returns {@code f(v)}. Otherwise, returns an empty {@code Maybe}.
     *  (This is a monadic bind.)
     *  
     *  This method is useful for chaining together a series of {@code Maybe} accesses. For
     *  example, supposing {@code T.foo()} returns a Maybe:
     * <pre>
     * {@code
     * Maybe<T> mt = ...
     * mt.thenMaybe(t -> t.foo().then(f -> ...))
     *   .orElse(...)
     * }</pre>
     *
     * @param f The function to be applied to the contained value, if any.
     * @param <U> The type of the value that may be returned by the function {@code f}.
     * @return the {@code Maybe} returned by {@code f}, if a value is contained in this
     *         {@code Maybe}. Otherwise, an empty {@code Maybe}.
     */
    public abstract <U> Maybe<U> thenMaybe(Function<? super T, ? extends Maybe<? extends U>> f);

    /** If a value {@code v} is present, returns a {@code Maybe} containing {@code f(v)}, which must be non-null.
     *  Otherwise, returns an empty {@code Maybe}. (This is a monadic bind composed with a monadic unit.)
     *  This method can be used conveniently along with orElse to handle both
     *  maybe cases, e.g.:
     * 
     * <pre>
     * {@code
     * Maybe<T> mt = ...
     * mt.then(t -> ...)
     *   .orElse(...)
     * }</pre>
     *
     * @param f The function to be applied to the contained value, if any.
     * @param <U> The type of the value that may be returned by the function {@code f}.
     * @return a {@code Maybe} containing the value {@code f}, if a value is contained in this
     *         {@code Maybe}. Otherwise, an empty {@code Maybe}.
     */
    public abstract <U> Maybe<U> then(Function<? super T, ? extends U> f);

    /** Returns the contained value, if any; otherwise, returns {@code other}.
     *  Note: since orElse is an ordinary method call, its argument is always computed,
     *  unlike a Java {@code else} statement. If the argument is
     *  expensive to compute or has side effects, {@code orElseGet()} should
     *  be used instead.
     *  
     *  @param other The value to be returned if no value is in this {@code Maybe}.
     *  @return The contained value, or an empty {@code Maybe}.
     */
    public abstract T orElse(T other);

    /** Returns the contained value, if any; otherwise, returns {@code other.get()}.
     *  @param other The function to use when this {@code Maybe} is empty.
     *  @return the contained value or {@code other.get()}.
     */
    public abstract T orElseGet(Supplier<? extends T> other);

    /** Returns the contained value, if any; otherwise, throws the supplied exception. */
    public abstract <E extends Throwable> T orElseThrow(Supplier<E> throwable) throws E;
    
    /** Returns this if a value is contained; otherwise, returns {@code other.get()}.
     *  @param other The function to use when this {@code Maybe} is empty.
     *  @return this or {@code other.get()}.
     */
    public abstract Maybe<T> orElseMaybe(Supplier<? extends Maybe<? extends T>> other);

    /** Call {@code cons} on the contained value, if any.
     *  @param cons The function to send the contained value to.
     */
    public abstract void thenDo(Consumer<? super T> cons);

    /** If a value is contained, run consThen on the value; otherwise run procElse */
    public abstract void thenElse(Consumer<? super T> consThen, Runnable procElse);

    /** Provide an iterator that yields either one {@code T} or none, depending. */
    public abstract Iterator<T> iterator();

    @Override
    public boolean containsAll(Collection<?> c) {
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
    public static <T> Maybe<T> from(T v) {
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
    public static <T> Maybe<T> fromOptional(Optional<? extends T> optional) {
        return Maybe.cast(optional.map(Maybe::some).orElseGet(Maybe::none));
    }
    /**
     * Create an {@code Optional} from a {@code Maybe}
     */
    public Optional<T> toOptional() {
        return then(Optional::of).orElse(Optional.empty());
    }

    /** Get the value in an {@code Optional}, if present; otherwise
     *  throw the checked exception {@code NoMaybeValue}. This
     *  method allows {@code Optional}s to be used as if they were Maybes.
     *  @return The value in the optional, if any
     *  @throws NoMaybeValue if no value is present.
     */
    public static <T> T getOptional(Optional<? extends T> optional) throws NoMaybeValue {
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
    public static <T, E extends Throwable> Maybe<T> fromCatching(ThrowingSupplier<T, E> valueOrThrow, Class<? super E> exnClass) {
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
    public static <T> Maybe<T> none() {
        return Maybes.none();
    }

    /** Creates a {@code Maybe} from a non-null argument.
     * @param v must be non-null
     * @throws IllegalArgumentException if a null value is passed to it.
     */
    public static <T> Maybe<T> some(T v) {
        return Maybes.some(v);
    }
    
    /** Convert a {@code Maybe<U>} to a {@code Maybe<T>}, when {@code T}
     *  is a supertype of {@code U}. This covariant subtyping is safe because
     *  {@code Maybe}s are immutable.
     */
    @SuppressWarnings("unchecked")
    public static <T,U extends T> Maybe<T> cast(Maybe<U> in) {
      return (Maybe<T>)in;
    }
}
