package eu.jonahbauer.android.preference.annotations;

import eu.jonahbauer.android.preference.annotations.serializer.PreferenceSerializer;

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
     * The type of the preference.
     * <p>
     *     If no {@linkplain #serializer() serializer} is provided this must be one of {@code byte.class},
     *     {@code char.class}, {@code short.class}, {@code int.class}, {@code long.class}, {@code float.class},
     *     {@code double.class}, {@code boolean.class}, {@code String.class}, {@code void.class}, {@code Set.class}
     *     or any subtype of {@code Enum.class}.
     * </p>
     * <p>
     *     If a serializer is provided the type declared here will serve as type-argument and/or constructor argument
     *     for the serializer. The actual preference type will be the serializers source type and may differ.
     * </p>
     * <p>
     *     Since {@code SharedPreferences} only support {@code int}, {@code long}, {@code float}, {@code boolean},
     *     {@code String} and {@code Set<String>} the other natively supported types must be converted into one of those types:
     *     <ul>
     *         <li>{@code byte}, {@code char} and {@code short} are stored as an {@code int}</li>
     *         <li>
     *             {@code double} is stored as a {@code long} via {@link Double#doubleToRawLongBits(double)} and
     *             {@link Double#longBitsToDouble(long)}
     *         </li>
     *         <li>
     *             {@code enum} is stored as a {@code String} via {@link Enum#name()} and
     *             {@link Enum#valueOf(Class, String)}
     *         </li>
     *         <li>
     *             {@code Set} is interpreted as {@code Set<String>}
     *         </li>
     *         <li>
     *             {@code void} is not stored
     *         </li>
     *     </ul>
     * </p>
     * @see #serializer()
     */
    Class<?> type();

    /**
     * The default value for the preference. If no default value is provided the default value will be
     * <ul>
     *     <li>{@code false} for {@code boolean} preferences,</li>
     *     <li>{@code 0} for {@code byte}, {@code short}, {@code char}, {@code int}, {@code long}, {@code float} and {@code double} preferences and</li>
     *     <li>{@code null} for {@code String}, {@code enum} and {@code Set<String>} preferences</li>
     * </ul>
     * This field does not have an effect for {@code void} preferences. If the {@link #type()} is {@code String}, then
     * the default value is automatically escaped and quoted, otherwise it will be copied into the generated class
     * source code as is.
     * If a {@link #serializer()} is used, the default value must be provided in serialized form, i.e. it must be
     * a valid argument to the serializers {@link PreferenceSerializer#deserialize(Object)} method.
     * @implNote it is possibly to inject code into the generated classes by misusing this field. Just don't.
     */
    String defaultValue() default NO_DEFAULT_VALUE;

    /**
     * A description that will be used as documentation for the preference accessors in the generated class.
     */
    String description() default "";

    /**
     * <p>
     *     A serializer used for converting the preference type to a type supported by {@code SharedPreferences} and back.
     * </p>
     * <p>
     *     A serializer must have at most one type argument {@code T} - the
     *     {@linkplain #type() declared preference type} - and must either have a default constructor or a
     *     constructor taking exactly one argument of type {@link Class Class&lt;? extends T&gt;}. If both constructors
     *     are present it is not defined which one will be used. Furthermore the serializers target type must be a
     *     primitive wrapper or {@code String}. If the target type is a primitive wrapper the argument of
     *     {@link PreferenceSerializer#deserialize(Object)} is guaranteed to be non-{@code null} and the return value of
     *     {@link PreferenceSerializer#serialize(Object)} must be non-{@code null}.
     * </p>
     * <p>
     *     The actual preference type will be the serializers source type. Therefore a preference
     *     <pre>{@code
     *     @Preference(name = "list", type = Foo.class, serializer = ListSerializer.class)
     *     }</pre>
     *     with a serializer
     *     <pre>{@code
     *     public class ListSerializer<T> implements PreferenceSerializer<List<T>, String> {
     *         public ListSerializer(Class<? extends T> clazz) {
     *         }
     *     }
     *     }</pre>
     *     would result in a preference of type {@code List<Foo>}. The serializer would be instantiated with
     *     <pre>{@code
     *     new ListSerializer<>(Foo.class)
     *     }</pre>
     * </p>
     */
    Class<? extends PreferenceSerializer> serializer() default PreferenceSerializer.class;
}
