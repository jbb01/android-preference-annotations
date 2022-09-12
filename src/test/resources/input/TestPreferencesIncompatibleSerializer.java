package eu.jonahbauer.android.preference.annotations.sources;

import eu.jonahbauer.android.preference.annotations.Preference;
import eu.jonahbauer.android.preference.annotations.PreferenceGroup;
import eu.jonahbauer.android.preference.annotations.Preferences;
import eu.jonahbauer.android.preference.annotations.R;
import eu.jonahbauer.android.preference.annotations.serializer.Serializer;

import java.math.BigInteger;

@Preferences(name = "eu.jonahbauer.android.preference.annotations.generated.TestPreferences", r = R.class, makeFile = true, value = {
        @PreferenceGroup(name = "general", prefix = "preferences_general_", suffix = "_key", value = {
                @Preference(name = "big_int_pref", type = BigInteger.class, description = "a big integer preference", serializer = TestPreferencesIncompatibleSerializer.VoidSerializer.class),
        })
})
public final class TestPreferencesIncompatibleSerializer {
    private TestPreferencesIncompatibleSerializer() {}

    public static class VoidSerializer implements Serializer<Void, String> {
        public String serialize(Void value) {
            return null;
        }

        public Void deserialize(String value) {
            return null;
        }
    }
}
