package eu.jonahbauer.android.preference.annotations.serializer;

/**
 * The default {@link PreferenceExportSerializer} for enum preferences, converting between the enum type and
 * {@code String} via {@link Enum#name()} and {@link Enum#valueOf(Class, String)}.
 */
public final class EnumPreferenceExportSerializer<T extends Enum<T>> implements PreferenceExportSerializer<T, String> {
    private final Class<T> clazz;

    public EnumPreferenceExportSerializer(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public String export(T value) {
        if (value == null) return null;
        return value.name();
    }

    @Override
    public T restore(String value) {
        if (value == null) return null;
        try {
            return Enum.valueOf(clazz, value);
        } catch (IllegalArgumentException e) {
            throw new PreferenceSerializationException(e);
        }
    }
}
