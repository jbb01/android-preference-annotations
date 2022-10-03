package eu.jonahbauer.android.preference.annotations.serializer;

import eu.jonahbauer.android.preference.annotations.Preference;

/**
 * An interface for converting between two types. See {@link Preference#serializer()} for more information.
 * @param <S> the runtime type.
 * @param <T> the persistent type. must be one of {@code boolean}, {@code byte}, {@code short}, {@code char},
 * {@code int}, {@code long}, {@code float}, {@code double} or {@code String}.
 * @see Preference#serializer()
 */
@SuppressWarnings("unused")
public interface PreferenceSerializer<S, T> {
    /**
     * Convert from runtime type to persistent type. If the persistent type is a primitive (wrapper) the return value
     * must not be {@code null}.
     * @param value an object of the runtime type
     * @return the persistent type representation of the parameter
     * @throws PreferenceSerializationException if the object could not be serialized
     */
    T serialize(S value) throws PreferenceSerializationException;

    /**
     * Convert from persistent type to runtime type. If the persistent type is a primitive (wrapper) the parameter is
     * guaranteed to not be {@code null}
     * @param value an object of the persistent type
     * @return the runtime type value of the parameter
     * @throws PreferenceSerializationException if the object could not be deserialized
     */
    S deserialize(T value) throws PreferenceSerializationException;
}
