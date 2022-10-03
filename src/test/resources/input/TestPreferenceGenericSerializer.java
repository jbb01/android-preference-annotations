package eu.jonahbauer.android.preference.annotations.sources;

import eu.jonahbauer.android.preference.annotations.Preference;
import eu.jonahbauer.android.preference.annotations.PreferenceGroup;
import eu.jonahbauer.android.preference.annotations.Preferences;
import eu.jonahbauer.android.preference.annotations.R;
import eu.jonahbauer.android.preference.annotations.serializer.PreferenceSerializer;

@Preferences(name = "eu.jonahbauer.android.preference.annotations.generated.TestPreferences", r = R.class, makeFile = true, value = {
        @PreferenceGroup(name = "general", prefix = "preferences_general_", suffix = "_key", value = {
                @Preference(name = "int_pref", type = int.class, description = "an int preference", serializer = TestPreferenceGenericSerializer.NumberSerializer.class, defaultValue = "0"),
                @Preference(name = "float_pref", type = float.class, description = "an float preference", serializer = TestPreferenceGenericSerializer.NumberSerializer.class, defaultValue = "0"),
        })
})
public final class TestPreferenceGenericSerializer {
    private TestPreferenceGenericSerializer() {}

    public static class NumberSerializer<T extends Number> implements PreferenceSerializer<T, String> {
        private final Class<? extends T> clazz;

        public NumberSerializer(Class<? extends T> clazz) {
            this.clazz = clazz;
        }

        public String serialize(T value) {
            return value.toString();
        }

        public T deserialize(String value) {
            try {
                return (T) clazz.getMethod("valueOf", String.class).invoke(null, value);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
