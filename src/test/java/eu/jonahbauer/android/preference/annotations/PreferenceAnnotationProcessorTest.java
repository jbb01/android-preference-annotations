package eu.jonahbauer.android.preference.annotations;

import android.content.SharedPreferences;
import android.content.res.Resources;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import eu.jonahbauer.android.preference.annotations.processor.PreferenceProcessor;
import eu.jonahbauer.android.preference.annotations.util.CompilationClassLoader;
import eu.jonahbauer.android.preference.annotations.util.InMemoryResources;
import eu.jonahbauer.android.preference.annotations.util.InMemorySharedPreferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PreferenceAnnotationProcessorTest {
    private SharedPreferences sharedPreferences;
    private Resources resources;
    
    @BeforeEach
    public void setUp() {
        sharedPreferences = new InMemorySharedPreferences();
        resources = InMemoryResources.builder()
                .put(R.string.preferences_general_boolean_pref_key, "preferences.general.boolean")
                .put(R.string.preferences_general_byte_pref_key, "preferences.general.byte")
                .put(R.string.preferences_general_short_pref_key, "preferences.general.short")
                .put(R.string.preferences_general_char_pref_key, "preferences.general.char")
                .put(R.string.preferences_general_int_pref_key, "preferences.general.int")
                .put(R.string.preferences_general_long_pref_key, "preferences.general.long")
                .put(R.string.preferences_general_float_pref_key, "preferences.general.float")
                .put(R.string.preferences_general_double_pref_key, "preferences.general.double")
                .put(R.string.preferences_general_string_pref_key, "preferences.general.string")
                .put(R.string.preferences_general_void_pref_key, "preferences.general.void")
                .put(R.string.preferences_general_big_int_pref_key, "preferences.general.big_int")
                .put(R.string.preferences_general_enum_pref_key, "preferences.general.enum")
                .build();

    }

    @Test
    public void testSuccessfulCompilation() throws Exception {
        var compilation = compile("input/TestPreferences.java");
        assertThat(compilation).succeededWithoutWarnings();

        var classLoader = new CompilationClassLoader(PreferenceAnnotationProcessorTest.class.getClassLoader(), compilation);
        var clazz = classLoader.loadClass("eu.jonahbauer.android.preference.annotations.generated.TestPreferences");
        
        check(clazz, Map.of("general", List.of(
                new Preference<>("booleanPref", boolean.class, false, true, "preferences.general.boolean"),
                new Preference<>("bytePref", byte.class, (byte) 0, (byte) 16, "preferences.general.byte"),
                new Preference<>("shortPref", short.class, (short) 0, (short) 16, "preferences.general.short"),
                new Preference<>("charPref", char.class, (char) 0, (char) 16, "preferences.general.char"),
                new Preference<>("intPref", int.class, 0, 16, "preferences.general.int"),
                new Preference<>("longPref", long.class, (long) 0, (long) 16, "preferences.general.long"),
                new Preference<>("floatPref", float.class, (float) 0, (float) 16, "preferences.general.float"),
                new Preference<>("doublePref", double.class, (double) 0, (double) 16, "preferences.general.double"),
                new Preference<>("stringPref", String.class, null, "Hello World!", "preferences.general.string"),
                new Preference<>("voidPref", void.class, null, null, "preferences.general.void")
        )));
    }

    @Test
    public void testSuccessfulCompilationWithDefaultValues() throws Exception {
        var compilation = compile("input/TestPreferencesDefaultValue.java");
        assertThat(compilation).succeededWithoutWarnings();

        var classLoader = new CompilationClassLoader(PreferenceAnnotationProcessorTest.class.getClassLoader(), compilation);
        var clazz = classLoader.loadClass("eu.jonahbauer.android.preference.annotations.generated.TestPreferences");

        check(clazz, Map.of("general", List.of(
                new Preference<>("bytePref", byte.class, (byte) 5, (byte) 16, "preferences.general.byte"),
                new Preference<>("stringPref", String.class, "this has to be \"quoted\" 'properly'", "Hello World!", "preferences.general.string")
        )));
    }

    @Test
    public void testSuccessfulCompilationWithSerializer() throws Exception {
        var compilation = compile("input/TestPreferencesSerializer.java");
        assertThat(compilation).succeededWithoutWarnings();

        var classLoader = new CompilationClassLoader(PreferenceAnnotationProcessorTest.class.getClassLoader(), compilation);
        var clazz = classLoader.loadClass("eu.jonahbauer.android.preference.annotations.generated.TestPreferences");

        check(clazz, Map.of("general", List.of(
                new Preference<>("bigIntPref", BigInteger.class, null, BigInteger.valueOf(12345), "preferences.general.big_int")
        )));
    }

    @Test
    public void testSuccessfulCompilationWithEnum() throws Exception {
        var compilation = compile("input/TestPreferencesEnum.java");
        assertThat(compilation).succeededWithoutWarnings();

        var classLoader = new CompilationClassLoader(PreferenceAnnotationProcessorTest.class.getClassLoader(), compilation);
        var clazz = classLoader.loadClass("eu.jonahbauer.android.preference.annotations.generated.TestPreferences");

        check(clazz, Map.of("general", List.of(
                new Preference<>("enumPref", StandardOpenOption.class, null, StandardOpenOption.APPEND, "preferences.general.enum")
        )));
    }

    @Test
    public void testIncompatibleSerializer() {
        var compilation = compile("input/TestPreferencesIncompatibleSerializer.java");
        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Incompatible serializer eu.jonahbauer.android.preference.annotations.serializer.Serializer<java.lang.Void,java.lang.String> for type java.math.BigInteger of preference big_int_pref");
    }

    @Test
    public void testUnsupportedSerializer() {
        var compilation = compile("input/TestPreferencesUnsupportedSerializer.java");
        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Unsupported preference type: java.lang.Object");
    }

    @Test
    public void testUnsupportedType() {
        var compilation = compile("input/TestPreferencesUnsupportedType.java");
        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Unsupported preference type: java.lang.Object");
    }

    @Test
    public void testInvalidPreferenceName() {
        var compilation = compile("input/TestPreferencesInvalidName.java");
        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Illegal preference name: 1int_pref");
    }

    @Test
    public void testInvalidPreferenceGroupName() {
        var compilation = compile("input/TestPreferencesInvalidGroupName.java");
        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Illegal preference group prefix: 1preferences_general_");
    }

    private static Compilation compile(String file) {
        return Compiler.javac()
                .withProcessors(new PreferenceProcessor())
                .withOptions("--release", "11")
                .compile(JavaFileObjects.forResource(file));
    }
    
    private void check(Class<?> clazz, Map<String, List<Preference<?>>> groups) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        var init = clazz.getMethod("init", SharedPreferences.class, Resources.class);
        init.invoke(null, sharedPreferences, resources);

        for (var entry : groups.entrySet()) {
            String group = entry.getKey();
            List<Preference<?>> preferences = entry.getValue();
            
            var groupObj = clazz.getMethod(group).invoke(null);
            var groupClass = groupObj.getClass();
            
            var keyObj = groupClass.getMethod("keys").invoke(groupObj);
            var keyClass = keyObj.getClass();

            for (var preference : preferences) {
                if (preference.type != void.class) {
                    var setter = groupClass.getMethod(preference.name, preference.type);
                    var getter = groupClass.getMethod(preference.name);
                    assertEquals(preference.defaultValue, getter.invoke(groupObj), "Default value of " + group + ":" + preference.name + " should be " + preference.defaultValue);
                    setter.invoke(groupObj, preference.newValue);
                    assertEquals(preference.newValue, getter.invoke(groupObj), "Could not change value of " + group + ":" + preference.name + " to " + preference.newValue);
                }

                var key = keyClass.getMethod(preference.name);
                assertEquals(preference.key, key.invoke(keyObj), "Key of " + group + ":" + preference.name + " should be " + preference.key);
            }
        }
    }
        
    static class Preference<T> {
        final String name;
        final Class<T> type;
        final T defaultValue;
        final T newValue;
        final String key;

        Preference(String name, Class<T> type, T defaultValue, T newValue, String key) {
            this.name = name;
            this.type = type;
            this.defaultValue = defaultValue;
            this.newValue = newValue;
            this.key = key;
        }
    }
}
