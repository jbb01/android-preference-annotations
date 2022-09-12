package eu.jonahbauer.android.preference.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a set of preferences. A preference class for easier and type-safe access to {@code SharedPreferences}
 * is generated from this annotation.
 * <br>
 * <h2>Example</h2>
 * <strong>Source Code</strong>
 * <pre>{@code @Preferences(name = "org.example.AppPreferences$Generated", r = R.class, value = {
 *     @PreferenceGroup(name = "general", prefix = "preferences_general_", suffix = "_key", value = {
 *         @Preference(type = byte.class, name = "byte_pref"),
 *         @Preference(type = short.class, name = "short_pref"),
 *         @Preference(type = char.class, name = "char_pref"),
 *         @Preference(type = int.class, name = "int_pref"),
 *         @Preference(type = long.class, name = "long_pref"),
 *         @Preference(type = float.class, name = "float_pref"),
 *         @Preference(type = double.class, name = "double_pref"),
 *         @Preference(type = String.class, name = "string_pref"),
 *         @Preference(type = void.class, name = "void_pref")
 *     }
 * }
 * public final AppPreferences extends AppPreferences$Generated {}
 * }</pre>
 * <strong>String Resources</strong>
 * <pre>{@code <resources>
 *     <string name="preferences_general_byte_pref_key">...</string>
 *     <string name="preferences_general_short_pref_key">...</string>
 *     <string name="preferences_general_char_pref_key">...</string>
 *     <string name="preferences_general_int_pref_key">...</string>
 *     <string name="preferences_general_long_pref_key">...</string>
 *     <string name="preferences_general_float_pref_key">...</string>
 *     <string name="preferences_general_double_pref_key">...</string>
 *     <string name="preferences_general_string_pref_key">...</string>
 * </resources>
 * }</pre>
 * <strong>Generated Code</strong>
 * <pre>{@code
 * class AppPreferences$Generated {
 *     protected AppPreferences$Generated {...} // throws exception
 *
 *     public static void init(SharedPreferences sharedPreferences, Resources resources) {...}
 *
 *     public static general general() {...}
 *
 *     public static final class general {
 *         private general(Resources resources) {} // private constructor
 *
 *         public Keys keys() {}
 *
 *         public byte bytePref() {}
 *         public short shortPref() {}
 *         public char charPref() {}
 *         public int intPref() {}
 *         public long longPref() {}
 *         public float floatPref() {}
 *         public double doublePref() {}
 *         public String stringPref() {}
 *
 *         public void bytePref(byte value) {}
 *         public void shortPref(short value) {}
 *         public void charPref(char value) {}
 *         public void intPref(int value) {}
 *         public void longPref(long value) {}
 *         public void floatPref(float value) {}
 *         public void doublePref(double value) {}
 *         public void stringPref(String value) {}
 *
 *         public final class Keys {
 *             private Keys() {} // private constructor
 *             public String bytePref() {}
 *             public String shortPref() {}
 *             public String charPref() {}
 *             public String intPref() {}
 *             public String longPref() {}
 *             public String floatPref() {}
 *             public String doublePref() {}
 *             public String stringPref() {}
 *         }
 *     }
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Preferences {
    /**
     * The fully qualified class name of the generated preferences class.
     */
    String name();

    /**
     * A list of {@link PreferenceGroup}s.
     */
    PreferenceGroup[] value();

    /**
     * The type of the app's {@code R} class.
     */
    Class<?> r();

    /**
     * Whether the generated class should be {@code public final} with a {@code private} constructor,
     * or package-private non-{@code final} with a {@code protected} constructor.
     * <br>
     * Having the generated class be non-{@code final} allows the syntax
     * <pre>
     * {@code @Preferences(name = "Prefs$Generated", ...)}
     * {@code public class Prefs extends Prefs$Generated {}}</pre>
     * Either way the generated class will throw an exception on instantiation.
     */
    boolean makeFile() default false;
}
