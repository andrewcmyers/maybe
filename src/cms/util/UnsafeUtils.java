package cms.util;

/** Unsafe utilities not intended for general use. */
public class UnsafeUtils {
    /**
     * Throws a (potentially checked) {@link Throwable} without checking it.
     * <strong>This method subverts the Java type system and is therefore unsafe.
     * Do not use it except in extremely rare situations.</strong>
     * <p>
     * Implementation note:<br/>
     * This method works by unsafely casting the throwable provided to an unspecified generic type {@code <T>}.
     * This does not raise a {@link ClassCastException} as generic types
     * <a href="https://www.baeldung.com/java-type-erasure">are erased</a> at runtime.
     * The compiler <a href="https://stackoverflow.com/a/31316879/13160488">always infers</a> {@code <T>} to be a {@link RuntimeException},
     * which lets it be thrown unchecked.
     * <strong>Note that this does not actually convert the provided {@link Throwable} to a {@link RuntimeException}.</strong>
     * The method works by subverting the type system and convincing it that the thrown exception is a different type than it is.
     * This is what makes it unsafe!
     *
     * @param throwable the {@link Throwable} to throw
     * @throws T the parameter {@code throwable}, with the (incorrectly and unsafely) inferred type of {@link RuntimeException}
     */
    @SuppressWarnings("unchecked")
    public static <T extends Throwable> void unsafeThrowUnchecked(Throwable throwable) throws T
    {
        throw (T) throwable;
    }
}
