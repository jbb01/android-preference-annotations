package eu.jonahbauer.android.preference.annotations.serializer;

/**
 * An interface for converting between two types.
 * @param <S> the source type.
 * @param <T> the target type. must be one of {@code boolean}, {@code byte}, {@code short}, {@code char}, {@code int}, {@code long}, {@code float},
 * {@code double} or {@code String}.
 */
public interface Serializer<S, T> {
    T serialize(S value);

    S deserialize(T value);
}
