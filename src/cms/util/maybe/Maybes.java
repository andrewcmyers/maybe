package cms.util.maybe;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Implementations of the interface {@code Maybe<T>}.
 */
@SuppressWarnings ("NullableProblems") // IntelliJ complains about lack of `@NotNull`,
                                       // but we don't use IntelliJ annotations
public class Maybes {

    @SuppressWarnings("rawtypes")
    private static final None theNone = new None();

    /** Representation of an empty Maybe. It should be created using the
     * method {@code Maybe.none()}.
     */
    public static final class None<T> implements Maybe<T> {
        private None() { // Use Maybe.none() instead
        }

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
            return Maybes.none();
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
            return Maybes.some(f.apply(value));
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
                return Maybes.none();
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

    @SuppressWarnings("unchecked")
    static <T> Maybe<T> none() {
        return (Maybe<T>) Maybes.theNone;
    }

    /** Creates a Maybe from a non-null argument.
     * @param v must be non-null
     * @throws IllegalArgumentException if a null value is passed to it.
     */
    static <T> Maybe<T> some(T v) {
        if (v == null) {
            throw new IllegalArgumentException("Maybe.some() requires a non-null argument");
        }
        return new Maybes.Some<>(v);
    }
}
