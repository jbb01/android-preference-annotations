package eu.jonahbauer.android.preference.annotations.serializer;

import eu.jonahbauer.android.preference.annotations.Preference;

/**
 * An interface for converting between the runtime type of a preference and a JSON representation
 * used by the generated {@code exportPreferences()} and {@code importPreferences(JSONObject)} methods.
 * <p>
 *     In contrast to a {@link PreferenceSerializer} (which converts to one of the types supported by
 *     {@code SharedPreferences}) an export serializer converts to a type that can be embedded in a
 *     {@code org.json.JSONObject} directly. This avoids exported files containing strings that are
 *     themselves serialized JSON.
 * </p>
 * @param <S> the runtime type.
 * @param <T> the JSON representation type. Must be one of {@code Boolean}, {@code Byte}, {@code Short},
 * {@code Integer}, {@code Long}, {@code Float}, {@code Double}, {@code String},
 * {@code org.json.JSONObject} or {@code org.json.JSONArray}.
 * @see Preference#exportSerializer()
 * @see Preference#export()
 */
public interface PreferenceExportSerializer<S, T> {
    /**
     * Convert from runtime type to JSON representation. If the JSON representation type is a primitive wrapper
     * the return value must not be {@code null}.
     * @param value an object of the runtime type
     * @return the JSON representation of the parameter
     * @throws PreferenceSerializationException if the object could not be serialized
     */
    T export(S value) throws PreferenceSerializationException;

    /**
     * Convert from JSON representation to runtime type. If the JSON representation type is a primitive wrapper
     * the parameter is guaranteed to not be {@code null}.
     * @param value the JSON representation
     * @return the runtime type value of the parameter
     * @throws PreferenceSerializationException if the object could not be deserialized
     */
    S restore(T value) throws PreferenceSerializationException;
}
