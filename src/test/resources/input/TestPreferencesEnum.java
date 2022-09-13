package eu.jonahbauer.android.preference.annotations.sources;

import eu.jonahbauer.android.preference.annotations.Preference;
import eu.jonahbauer.android.preference.annotations.PreferenceGroup;
import eu.jonahbauer.android.preference.annotations.Preferences;
import eu.jonahbauer.android.preference.annotations.R;

import java.nio.file.StandardOpenOption;

@Preferences(name = "eu.jonahbauer.android.preference.annotations.generated.TestPreferences", r = R.class, makeFile = true, value = {
        @PreferenceGroup(name = "general", prefix = "preferences_general_", suffix = "_key", value = {
                @Preference(name = "enum_pref", type = StandardOpenOption.class, description = "an enum preference")
        })
})
public final class TestPreferencesEnum {
    private TestPreferencesEnum() {}
}
