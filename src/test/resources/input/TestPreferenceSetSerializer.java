package eu.jonahbauer.android.preference.annotations.sources;

import eu.jonahbauer.android.preference.annotations.Preference;
import eu.jonahbauer.android.preference.annotations.PreferenceGroup;
import eu.jonahbauer.android.preference.annotations.Preferences;
import eu.jonahbauer.android.preference.annotations.R;
import eu.jonahbauer.android.preference.annotations.serializer.Serializer;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Preferences(name = "eu.jonahbauer.android.preference.annotations.generated.TestPreferences", r = R.class, makeFile = true, value = {
        @PreferenceGroup(name = "general", prefix = "preferences_general_", suffix = "_key", value = {
                @Preference(name = "set_pref", type = Set.class, description = "an int set preference", serializer = TestPreferenceSetSerializer.IntSetSerializer.class),
        })
})
public final class TestPreferenceSetSerializer {
    private TestPreferenceSetSerializer() {}

    public static class IntSetSerializer implements Serializer<Set<Integer>, Set<String>> {

        public Set<String> serialize(Set<Integer> value) {
            if (value == null) {
                return null;
            } else {
                return value.stream().map(String::valueOf).collect(Collectors.toSet());
            }
        }

        public Set<Integer> deserialize(Set<String> value) {
            if (value == null) {
                return null;
            } else {
                return value.stream().map(Integer::valueOf).collect(Collectors.toSet());
            }
        }
    }
}
