package eu.jonahbauer.android.preference.annotations.serializer;

import eu.jonahbauer.android.preference.annotations.Preference;

/**
 * An interface for converting between two types. See {@link Preference#serializer()} for more information.
 * @param <S> the source type.
 * @param <T> the target type. must be one of {@code boolean}, {@code byte}, {@code short}, {@code char}, {@code int}, {@code long}, {@code float},
 * {@code double} or {@code String}.
 * @see Preference#serializer()
 */
@SuppressWarnings("unused")
public interface Serializer<S, T> {
    T serialize(S value);

    S deserialize(T value);
}
