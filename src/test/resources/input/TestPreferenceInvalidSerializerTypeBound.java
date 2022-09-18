package eu.jonahbauer.android.preference.annotations.sources;

import eu.jonahbauer.android.preference.annotations.Preference;
import eu.jonahbauer.android.preference.annotations.PreferenceGroup;
import eu.jonahbauer.android.preference.annotations.Preferences;
import eu.jonahbauer.android.preference.annotations.R;
import eu.jonahbauer.android.preference.annotations.serializer.Serializer;

@Preferences(name = "eu.jonahbauer.android.preference.annotations.generated.TestPreferences", r = R.class, makeFile = true, value = {
        @PreferenceGroup(name = "general", prefix = "preferences_general_", suffix = "_key", value = {
                @Preference(name = "string_pref", type = String.class, description = "a string preference", serializer = TestPreferenceInvalidSerializerTypeBound.NumberSerializer.class),
        })
})
public final class TestPreferenceInvalidSerializerTypeBound {
    private TestPreferenceInvalidSerializerTypeBound() {}

    public static class NumberSerializer<T extends Number> implements Serializer<T, String> {
        public String serialize(T value) {
            return null;
        }

        public T deserialize(String value) {
            return null;
        }
    }
}
