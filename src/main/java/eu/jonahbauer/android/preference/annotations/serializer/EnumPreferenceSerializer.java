package eu.jonahbauer.android.preference.annotations.serializer;

public final class EnumPreferenceSerializer<T extends Enum<T>> implements PreferenceSerializer<T, String> {
    private final Class<T> clazz;

    public EnumPreferenceSerializer(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public String serialize(T value) {
        if (value == null) return null;
        return value.name();
    }

    @Override
    public T deserialize(String value) {
        if (value == null) return null;
        try {
            return Enum.valueOf(clazz, value);
        } catch (IllegalArgumentException e) {
            throw new PreferenceSerializationException(e);
        }
    }
}
