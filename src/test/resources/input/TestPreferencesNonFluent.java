package eu.jonahbauer.android.preference.annotations.sources;

import eu.jonahbauer.android.preference.annotations.Preference;
import eu.jonahbauer.android.preference.annotations.PreferenceGroup;
import eu.jonahbauer.android.preference.annotations.Preferences;
import eu.jonahbauer.android.preference.annotations.R;

@Preferences(name = "eu.jonahbauer.android.preference.annotations.generated.TestPreferences", r = R.class, fluent = false, makeFile = true, value = {
        @PreferenceGroup(name = "general", prefix = "preferences_general_", suffix = "_key", value = {
                @Preference(name = "boolean_pref", type = boolean.class, description = "a boolean preference"),
                @Preference(name = "byte_pref", type = byte.class, description = "a byte preference"),
                @Preference(name = "short_pref", type = short.class, description = "a short preference"),
                @Preference(name = "char_pref", type = char.class, description = "a char preference"),
                @Preference(name = "int_pref", type = int.class, description = "a int preference"),
                @Preference(name = "long_pref", type = long.class, description = "a long preference"),
                @Preference(name = "float_pref", type = float.class, description = "a float preference"),
                @Preference(name = "double_pref", type = double.class, description = "a double preference"),
                @Preference(name = "string_pref", type = String.class, description = "a string preference"),
                @Preference(name = "void_pref", type = void.class, description = "a void preference")
        })
})
public final class TestPreferencesNonFluent {
    private TestPreferencesNonFluent() {}
}
