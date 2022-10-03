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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class PreferenceAnnotationProcessorTest {
    public static final String PREFERENCES_GENERAL_BOOLEAN = "preferences.general.boolean";
    public static final String PREFERENCES_GENERAL_BYTE = "preferences.general.byte";
    public static final String PREFERENCES_GENERAL_SHORT = "preferences.general.short";
    public static final String PREFERENCES_GENERAL_CHAR = "preferences.general.char";
    public static final String PREFERENCES_GENERAL_INT = "preferences.general.int";
    public static final String PREFERENCES_GENERAL_LONG = "preferences.general.long";
    public static final String PREFERENCES_GENERAL_FLOAT = "preferences.general.float";
    public static final String PREFERENCES_GENERAL_DOUBLE = "preferences.general.double";
    public static final String PREFERENCES_GENERAL_STRING = "preferences.general.string";
    public static final String PREFERENCES_GENERAL_VOID = "preferences.general.void";
    public static final String PREFERENCES_GENERAL_BIG_INT = "preferences.general.big_int";
    public static final String PREFERENCES_GENERAL_ENUM = "preferences.general.enum";
    public static final String PREFERENCES_GENERAL_OBJECT = "preferences.general.object";
    public static final String PREFERENCES_GENERAL_LIST = "preferences.general.list";
    public static final String PREFERENCES_GENERAL_SET = "preferences.general.set";

    private SharedPreferences sharedPreferences;
    private Resources resources;
    
    @BeforeEach
    public void setUp() {
        sharedPreferences = new InMemorySharedPreferences();
        resources = InMemoryResources.builder()
                .put(R.string.preferences_general_boolean_pref_key, PREFERENCES_GENERAL_BOOLEAN)
                .put(R.string.preferences_general_byte_pref_key, PREFERENCES_GENERAL_BYTE)
                .put(R.string.preferences_general_short_pref_key, PREFERENCES_GENERAL_SHORT)
                .put(R.string.preferences_general_char_pref_key, PREFERENCES_GENERAL_CHAR)
                .put(R.string.preferences_general_int_pref_key, PREFERENCES_GENERAL_INT)
                .put(R.string.preferences_general_long_pref_key, PREFERENCES_GENERAL_LONG)
                .put(R.string.preferences_general_float_pref_key, PREFERENCES_GENERAL_FLOAT)
                .put(R.string.preferences_general_double_pref_key, PREFERENCES_GENERAL_DOUBLE)
                .put(R.string.preferences_general_string_pref_key, PREFERENCES_GENERAL_STRING)
                .put(R.string.preferences_general_void_pref_key, PREFERENCES_GENERAL_VOID)
                .put(R.string.preferences_general_big_int_pref_key, PREFERENCES_GENERAL_BIG_INT)
                .put(R.string.preferences_general_enum_pref_key, PREFERENCES_GENERAL_ENUM)
                .put(R.string.preferences_general_object_pref_key, PREFERENCES_GENERAL_OBJECT)
                .put(R.string.preferences_general_list_pref_key, PREFERENCES_GENERAL_LIST)
                .put(R.string.preferences_general_set_pref_key, PREFERENCES_GENERAL_SET)
                .build();

    }

    @Test
    public void testSuccessfulCompilation() throws Exception {
        var compilation = compile("input/TestPreferences.java");
        assertThat(compilation).succeededWithoutWarnings();

        var classLoader = new CompilationClassLoader(PreferenceAnnotationProcessorTest.class.getClassLoader(), compilation);
        var clazz = classLoader.loadClass("eu.jonahbauer.android.preference.annotations.generated.TestPreferences");

        check(clazz, Map.of("general", List.of(
                new Preference<>("booleanPref", boolean.class, false, true, PREFERENCES_GENERAL_BOOLEAN),
                new Preference<>("bytePref", byte.class, (byte) 0, (byte) 16, PREFERENCES_GENERAL_BYTE),
                new Preference<>("shortPref", short.class, (short) 0, (short) 16, PREFERENCES_GENERAL_SHORT),
                new Preference<>("charPref", char.class, (char) 0, (char) 16, PREFERENCES_GENERAL_CHAR),
                new Preference<>("intPref", int.class, 0, 16, PREFERENCES_GENERAL_INT),
                new Preference<>("longPref", long.class, (long) 0, (long) 16, PREFERENCES_GENERAL_LONG),
                new Preference<>("floatPref", float.class, (float) 0, (float) 16, PREFERENCES_GENERAL_FLOAT),
                new Preference<>("doublePref", double.class, (double) 0, (double) 16, PREFERENCES_GENERAL_DOUBLE),
                new Preference<>("stringPref", String.class, null, "Hello World!", PREFERENCES_GENERAL_STRING),
                new Preference<>("voidPref", void.class, null, null, PREFERENCES_GENERAL_VOID),
                new Preference<>("setPref", Set.class, null, Set.of("a", "b", "c"), PREFERENCES_GENERAL_SET)
        )));
    }

    @Test
    public void testSuccessfulCompilationWithNonFluentAccessors() throws Exception {
        var compilation = compile("input/TestPreferencesNonFluent.java");
        assertThat(compilation).succeededWithoutWarnings();

        var classLoader = new CompilationClassLoader(PreferenceAnnotationProcessorTest.class.getClassLoader(), compilation);
        var clazz = classLoader.loadClass("eu.jonahbauer.android.preference.annotations.generated.TestPreferences");

        check(clazz, false, Map.of("general", List.of(
                new Preference<>("booleanPref", boolean.class, false, true, PREFERENCES_GENERAL_BOOLEAN),
                new Preference<>("bytePref", byte.class, (byte) 0, (byte) 16, PREFERENCES_GENERAL_BYTE),
                new Preference<>("shortPref", short.class, (short) 0, (short) 16, PREFERENCES_GENERAL_SHORT),
                new Preference<>("charPref", char.class, (char) 0, (char) 16, PREFERENCES_GENERAL_CHAR),
                new Preference<>("intPref", int.class, 0, 16, PREFERENCES_GENERAL_INT),
                new Preference<>("longPref", long.class, (long) 0, (long) 16, PREFERENCES_GENERAL_LONG),
                new Preference<>("floatPref", float.class, (float) 0, (float) 16, PREFERENCES_GENERAL_FLOAT),
                new Preference<>("doublePref", double.class, (double) 0, (double) 16, PREFERENCES_GENERAL_DOUBLE),
                new Preference<>("stringPref", String.class, null, "Hello World!", PREFERENCES_GENERAL_STRING),
                new Preference<>("voidPref", void.class, null, null, PREFERENCES_GENERAL_VOID),
                new Preference<>("setPref", Set.class, null, Set.of("a", "b", "c"), PREFERENCES_GENERAL_SET)
        )));
    }

    @Test
    public void testSuccessfulCompilationWithDefaultValues() throws Exception {
        var compilation = compile("input/TestPreferencesDefaultValue.java");
        assertThat(compilation).succeededWithoutWarnings();

        var classLoader = new CompilationClassLoader(PreferenceAnnotationProcessorTest.class.getClassLoader(), compilation);
        var clazz = classLoader.loadClass("eu.jonahbauer.android.preference.annotations.generated.TestPreferences");

        check(clazz, Map.of("general", List.of(
                new Preference<>("bytePref", byte.class, (byte) 5, (byte) 16, PREFERENCES_GENERAL_BYTE),
                new Preference<>("stringPref", String.class, "this has to be \"quoted\" 'properly'", "Hello World!", PREFERENCES_GENERAL_STRING),
                new Preference<>("setPref", Set.class, Set.of("Hello", "World"), Set.of("Foo", "Bar"), PREFERENCES_GENERAL_SET)
        )));
    }

    @Test
    public void testSuccessfulCompilationWithSerializer() throws Exception {
        var compilation = compile("input/TestPreferencesSerializer.java");
        assertThat(compilation).succeededWithoutWarnings();

        var classLoader = new CompilationClassLoader(PreferenceAnnotationProcessorTest.class.getClassLoader(), compilation);
        var clazz = classLoader.loadClass("eu.jonahbauer.android.preference.annotations.generated.TestPreferences");

        check(clazz, Map.of("general", List.of(
                new Preference<>("bigIntPref", BigInteger.class, null, BigInteger.valueOf(12345), PREFERENCES_GENERAL_BIG_INT)
        )));
    }

    @Test
    public void testSuccessfulCompilationWithEnum() throws Exception {
        var compilation = compile("input/TestPreferencesEnum.java");
        assertThat(compilation).succeededWithoutWarnings();

        var classLoader = new CompilationClassLoader(PreferenceAnnotationProcessorTest.class.getClassLoader(), compilation);
        var clazz = classLoader.loadClass("eu.jonahbauer.android.preference.annotations.generated.TestPreferences");

        check(clazz, Map.of("general", List.of(
                new Preference<>("enumPref", StandardOpenOption.class, null, StandardOpenOption.APPEND, PREFERENCES_GENERAL_ENUM)
        )));
    }

    @Test
    public void testSuccessfulCompilationWithGenericSerializer() throws Exception {
        var compilation = compile("input/TestPreferenceGenericSerializer.java");
        assertThat(compilation).succeededWithoutWarnings();

        var classLoader = new CompilationClassLoader(PreferenceAnnotationProcessorTest.class.getClassLoader(), compilation);
        var clazz = classLoader.loadClass("eu.jonahbauer.android.preference.annotations.generated.TestPreferences");

        check(clazz, Map.of("general", List.of(
                new Preference<>("intPref", int.class, 0, 10, PREFERENCES_GENERAL_INT),
                new Preference<>("floatPref", float.class, 0f, 10f, PREFERENCES_GENERAL_FLOAT)
        )));
    }

    @Test
    public void testSuccessfulCompilationWithListSerializer() throws Exception {
        var compilation = compile("input/TestPreferenceListSerializer.java");
        assertThat(compilation).succeededWithoutWarnings();

        var classLoader = new CompilationClassLoader(PreferenceAnnotationProcessorTest.class.getClassLoader(), compilation);
        var clazz = classLoader.loadClass("eu.jonahbauer.android.preference.annotations.generated.TestPreferences");

        check(clazz, Map.of("general", List.of(
                new Preference<>("listPref", List.class, null, List.of(1, 2, 3), PREFERENCES_GENERAL_LIST)
        )));
    }

    @Test
    public void testSuccessfulCompilationWithSetSerializer() throws Exception {
        var compilation = compile("input/TestPreferenceSetSerializer.java");
        assertThat(compilation).succeededWithoutWarnings();

        var classLoader = new CompilationClassLoader(PreferenceAnnotationProcessorTest.class.getClassLoader(), compilation);
        var clazz = classLoader.loadClass("eu.jonahbauer.android.preference.annotations.generated.TestPreferences");

        check(clazz, Map.of("general", List.of(
                new Preference<>("setPref", Set.class, null, Set.of(1, 2, 3), PREFERENCES_GENERAL_SET)
        )));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void testSuccessfulCompilationWithJsonSerializer() throws Exception {
        var compilation = compile("input/TestPreferenceJsonSerializer.java");
        assertThat(compilation).succeededWithoutWarnings();

        var classLoader = new CompilationClassLoader(PreferenceAnnotationProcessorTest.class.getClassLoader(), compilation);
        var clazz = classLoader.loadClass("eu.jonahbauer.android.preference.annotations.generated.TestPreferences");

        var beanClazz = classLoader.loadClass("eu.jonahbauer.android.preference.annotations.sources.TestPreferenceJsonSerializer$Bean");
        var bean = beanClazz.getConstructor().newInstance();
        beanClazz.getMethod("setFoo", String.class).invoke(bean, "Foo");
        beanClazz.getMethod("setBar", String.class).invoke(bean, "Bar");

        check(clazz, Map.of("general", List.of(
                new Preference<>("objectPref", (Class) beanClazz, null, bean, PREFERENCES_GENERAL_OBJECT)
        )));
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

    @Test
    public void testInvalidSerializerTypeBound() {
        var compilation = compile("input/TestPreferenceInvalidSerializerTypeBound.java");
        compilation.errors().forEach(System.out::println);
        assertThat(compilation).failed();
    }

    @SuppressWarnings("deprecation")
    private static Compilation compile(String file) {
        var compilation = Compiler.javac()
                .withProcessors(new PreferenceProcessor())
                .withClasspathFrom(PreferenceAnnotationProcessorTest.class.getClassLoader())
                .withOptions("--release", "11")
                .compile(JavaFileObjects.forResource(file));
        if (compilation.status() == Compilation.Status.SUCCESS) {
            print(compilation);
        }
        return compilation;
    }

    private static void print(Compilation compilation) {
        for (var source : compilation.generatedSourceFiles()) {
            System.out.println(source.getName());
            System.out.println();
            try {
                System.out.println(source.getCharContent(true));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void check(Class<?> clazz,  Map<String, List<Preference<?>>> groups) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        check(clazz, true, groups);
    }

    private void check(Class<?> clazz, boolean fluent, Map<String, List<Preference<?>>> groups) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        var init = clazz.getMethod("init", SharedPreferences.class, Resources.class);
        init.invoke(null, sharedPreferences, resources);

        for (var entry : groups.entrySet()) {
            String group = entry.getKey();
            List<Preference<?>> preferences = entry.getValue();
            
            var groupObj = clazz.getMethod(getGetterName(group, null, fluent)).invoke(null);
            var groupClass = groupObj.getClass();
            
            var keyObj = groupClass.getMethod(getGetterName("keys", null, fluent)).invoke(groupObj);
            var keyClass = keyObj.getClass();

            for (var preference : preferences) {
                if (preference.type != void.class) try {
                    var setter = groupClass.getMethod(getSetterName(preference.name, fluent), preference.type);
                    var getter = groupClass.getMethod(getGetterName(preference.name, preference.type, fluent));
                    assertEquals(preference.defaultValue, getter.invoke(groupObj), "Default value of " + group + ":" + preference.name + " should be " + preference.defaultValue);
                    setter.invoke(groupObj, preference.newValue);
                    assertEquals(preference.newValue, getter.invoke(groupObj), "Could not change value of " + group + ":" + preference.name + " to " + preference.newValue);
                } catch (NoSuchMethodException e) {
                    fail("Could not find accessor for preference " + preference.name + ".");
                }

                var key = keyClass.getMethod(getGetterName(preference.name, String.class, fluent));
                assertEquals(preference.key, key.invoke(keyObj), "Key of " + group + ":" + preference.name + " should be " + preference.key);
            }
        }

        var clear = clazz.getMethod("clear");
        clear.invoke(null);
        groups.values().stream().flatMap(List::stream).forEach(preference -> {
            assertFalse(sharedPreferences.contains(preference.key));
        });
    }

    private String getGetterName(String name, Class<?> type, boolean fluent) {
        return getAccessorName(name, type, false, fluent);
    }

    private String getSetterName(String name, boolean fluent) {
        return getAccessorName(name, null, true, fluent);
    }

    private String getAccessorName(String name, Class<?> type, boolean setter, boolean fluent) {
        if (fluent) {
            return name;
        } else if (setter) {
            return "set" + name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
        } else if (boolean.class.equals(type)) {
            return "is" + name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
        } else {
            return "get" + name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
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
