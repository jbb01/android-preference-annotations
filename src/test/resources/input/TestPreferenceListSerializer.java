package eu.jonahbauer.android.preference.annotations.sources;

import eu.jonahbauer.android.preference.annotations.Preference;
import eu.jonahbauer.android.preference.annotations.PreferenceGroup;
import eu.jonahbauer.android.preference.annotations.Preferences;
import eu.jonahbauer.android.preference.annotations.R;
import eu.jonahbauer.android.preference.annotations.serializer.Serializer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Preferences(name = "eu.jonahbauer.android.preference.annotations.generated.TestPreferences", r = R.class, makeFile = true, value = {
        @PreferenceGroup(name = "general", prefix = "preferences_general_", suffix = "_key", value = {
                @Preference(name = "list_pref", type = Integer.class, description = "an int list preference", serializer = TestPreferenceListSerializer.NumberListSerializer.class),
        })
})
public final class TestPreferenceListSerializer {
    private TestPreferenceListSerializer() {}

    public static class NumberListSerializer<T extends Number> implements Serializer<List<T>, String> {
        private final Class<? extends T> clazz;

        public NumberListSerializer(Class<? extends T> clazz) {
            this.clazz = clazz;
        }

        public String serialize(List<T> value) {
            if (value == null) {
                return null;
            } else {
                return value.stream().map(String::valueOf).collect(Collectors.joining(" "));
            }
        }

        public List<T> deserialize(String value) {
            if (value == null) {
                return null;
            } else try {
                var method = clazz.getMethod("valueOf", String.class);
                return Arrays.stream(value.split(" "))
                        .map(str -> {
                            try {
                                return (T) method.invoke(null, str);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .collect(Collectors.toList());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
