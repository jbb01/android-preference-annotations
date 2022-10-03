package eu.jonahbauer.android.preference.annotations.sources;

import eu.jonahbauer.android.preference.annotations.Preference;
import eu.jonahbauer.android.preference.annotations.PreferenceGroup;
import eu.jonahbauer.android.preference.annotations.Preferences;
import eu.jonahbauer.android.preference.annotations.R;
import eu.jonahbauer.android.preference.annotations.serializer.PreferenceSerializer;

import java.math.BigInteger;
import java.util.Base64;

@Preferences(name = "eu.jonahbauer.android.preference.annotations.generated.TestPreferences", r = R.class, makeFile = true, value = {
        @PreferenceGroup(name = "general", prefix = "preferences_general_", suffix = "_key", value = {
                @Preference(name = "big_int_pref", type = BigInteger.class, description = "a big integer preference", serializer = TestPreferencesSerializer.BigIntSerializer.class),
        })
})
public final class TestPreferencesSerializer {
    private TestPreferencesSerializer() {}

    public static interface ToStringSerializer<T> extends PreferenceSerializer<T, String> {}

    public static class BigIntSerializer implements ToStringSerializer<BigInteger> {
        public String serialize(BigInteger value) {
            if (value == null) return null;
            return Base64.getEncoder().encodeToString(value.toByteArray());
        }

        public BigInteger deserialize(String value) {
            if (value == null) return null;
            return new BigInteger(Base64.getDecoder().decode(value));
        }
    }
}
