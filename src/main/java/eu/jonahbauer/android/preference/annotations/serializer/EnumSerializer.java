package eu.jonahbauer.android.preference.annotations.serializer;

public final class EnumSerializer<T extends Enum<T>> implements Serializer<T, String> {
    private final Class<T> clazz;

    public EnumSerializer(Class<T> clazz) {
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
        return Enum.valueOf(clazz, value);
    }
}
