package eu.jonahbauer.android.preference.annotations.sources;

import eu.jonahbauer.android.preference.annotations.Preference;
import eu.jonahbauer.android.preference.annotations.PreferenceGroup;
import eu.jonahbauer.android.preference.annotations.Preferences;
import eu.jonahbauer.android.preference.annotations.R;

@Preferences(name = "eu.jonahbauer.android.preference.annotations.generated.TestPreferences", r = R.class, makeFile = true, value = {
        @PreferenceGroup(name = "general", prefix = "preferences_general_", suffix = "_key", value = {
                @Preference(name = "byte_pref", type = byte.class, description = "a byte preference", defaultValue = "5"),
                @Preference(name = "string_pref", type = String.class, description = "a string preference", defaultValue = "this has to be \"quoted\" 'properly'")
        })
})
public final class TestPreferencesDefaultValue {
    private TestPreferencesDefaultValue() {}
}
