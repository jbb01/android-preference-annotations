package eu.jonahbauer.android.preference.annotations.sources;

import eu.jonahbauer.android.preference.annotations.Preference;
import eu.jonahbauer.android.preference.annotations.PreferenceGroup;
import eu.jonahbauer.android.preference.annotations.Preferences;
import eu.jonahbauer.android.preference.annotations.R;

@Preferences(name = "eu.jonahbauer.android.preference.annotations.generated.TestPreferences", r = R.class, value = {
        @PreferenceGroup(name = "general", prefix = "preferences_general_", suffix = "_key", value = {
                @Preference(name = "1int_pref", type = int.class, description = "an invalid preference"),
        })
})
public final class TestPreferencesInvalidName {
    private TestPreferencesInvalidName() {}
}
