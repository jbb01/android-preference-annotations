package eu.jonahbauer.android.preference.annotations;

import eu.jonahbauer.android.preference.annotations.serializer.Serializer;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a preference. The preference key is the value of the string resource
 * <pre>{@code R.string.${preference_group_prefix}${name}${preference_group_suffix}}</pre>
 * with {@code ${preference_group_prefix}} and {@code ${preference_group_suffix}} being the
 * {@linkplain PreferenceGroup#prefix() prefix} and {@linkplain PreferenceGroup#suffix() suffix} defined
 * in the enclosing {@link PreferenceGroup}-Annotation.
 * <br>
 * For each preference with a {@link #type()} other than {@code void.class} a getter
 * <pre>{@code public ${type} ${name}()}</pre>
 * and a setter
 * <pre>{@code public void ${name}(${type} value)}</pre>
 * will be generated in the preference group. Additionally, a key accessor
 * <pre>{@code public String ${name}()}</pre>
 * is generated in the {@code Keys} class of the preference group for each preference (including {@code void}).
 * @see Preferences
 * @see PreferenceGroup
 */
@Target({})
@Retention(RetentionPolicy.CLASS)
public @interface Preference {
    String NO_DEFAULT_VALUE = "__NO_DEFAULT_VALUE__";

    /**
     * The name of the preference. Must be a valid Java identifier on its own and when combined with
     * the preference gropus {@link PreferenceGroup#prefix()} and {@link PreferenceGroup#suffix()}.
     */
    String name();

    /**
     * The type of the preference. If no {@link #serializer()} is provided this must be one of {@code byte.class},
     * {@code char.class}, {@code short.class}, {@code int.class}, {@code long.class}, {@code float.class},
     * {@code double.class}, {@code boolean.class}, {@code String.class}, {@code void.class}
     * or any subtype of {@code Enum.class}, otherwise this must match the source type of the serializer.
     */
    Class<?> type();

    /**
     * The default value for the preference. If no default value is provided the default value will be
     * <ul>
     *     <li>{@code false} for {@code boolean} preferences,</li>
     *     <li>{@code 0} for {@code byte}, {@code short}, {@code char}, {@code int}, {@code long}, {@code float} and {@code double} preferences and</li>
     *     <li>{@code null} for {@code String} and {@code enum} preferences</li>
     * </ul>
     * This field does not have an effect for {@code void} preferences. If the {@link #type()} is {@code String}, then
     * the default value is automatically escaped and quoted, otherwise it will be copied into the generated class
     * source code as is.
     * If a {@link #serializer()} is used, the default value must be provided in serialized form, i.e. it must be
     * a valid argument to the serializers {@link Serializer#deserialize(Object)} method.
     * @implNote it is possibly to inject code into the generated classes by misusing this field. Just don't.
     */
    String defaultValue() default NO_DEFAULT_VALUE;

    /**
     * A description that will be used as documentation for the preference accessors in the generated class.
     */
    String description() default "";

    /**
     * A serializer used for converting the preference type to a type supported by {@code SharedPreferences} and back.
     * The class specified here must either have a default constructor or a constructor taking exactly one argument
     * of type {@link Class}. If both constructors are present, it is not defined which one will be used.
     * Also, the serializer class must have at most one type argument. If a type argument is present, the
     * {@linkplain #type() preference type} will be used.
     */
    Class<? extends Serializer> serializer() default Serializer.class;
}
