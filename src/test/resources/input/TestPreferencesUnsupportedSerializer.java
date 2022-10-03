package eu.jonahbauer.android.preference.annotations.sources;

import eu.jonahbauer.android.preference.annotations.Preference;
import eu.jonahbauer.android.preference.annotations.PreferenceGroup;
import eu.jonahbauer.android.preference.annotations.Preferences;
import eu.jonahbauer.android.preference.annotations.R;
import eu.jonahbauer.android.preference.annotations.serializer.PreferenceSerializer;

import java.math.BigInteger;

@Preferences(name = "eu.jonahbauer.android.preference.annotations.generated.TestPreferences", r = R.class, makeFile = true, value = {
        @PreferenceGroup(name = "general", prefix = "preferences_general_", suffix = "_key", value = {
                @Preference(name = "big_int_pref", type = BigInteger.class, description = "a big integer preference", serializer = TestPreferencesUnsupportedSerializer.UnsupportedSerializer.class),
        })
})
public final class TestPreferencesUnsupportedSerializer {
    private TestPreferencesUnsupportedSerializer() {}

    public static class UnsupportedSerializer implements PreferenceSerializer<BigInteger, Object> {
        public String serialize(BigInteger value) {
            return null;
        }

        public BigInteger deserialize(String value) {
            return null;
        }
    }
}
