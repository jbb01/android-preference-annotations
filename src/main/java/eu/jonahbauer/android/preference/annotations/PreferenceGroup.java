package eu.jonahbauer.android.preference.annotations;

import java.lang.annotation.*;

/**
 * Defines a group of preferences. Every preference group has a name and may contain
 * multiple preferences.
 * <br>
 * For each preference group a {@code static} inner class
 * <pre>{@code public static final class ${name} {...}}</pre>
 * and an accessor
 * <pre>{@code public static ${name} ${name}()}</pre>
 * will be generated in the preferences class.
 * @see Preferences
 * @see Preference
 */
@Target({})
@Retention(RetentionPolicy.CLASS)
public @interface PreferenceGroup {
    /**
     * A prefix that is prepended to the {@linkplain Preference#name() preference name} in order to build the preference key.
     */
    String prefix() default "";

    /**
     * A suffix that is appended to the {@linkplain Preference#name() preference name} in order to build the preference key.
     */
    String suffix() default "";

    /**
     * The name of the preference group. This is only used as a class name and does not contribute to the preference key.
     * Must be a valid Java identifier.
     */
    String name();

    /**
     * A list of {@link Preference}s.
     */
    Preference[] value();
}
