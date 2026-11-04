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
import org.json.JSONObject;
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
    public void testExportImport() throws Exception {
        var compilation = compile("input/TestPreferencesExport.java");
        assertThat(compilation).succeededWithoutWarnings();

        var classLoader = new CompilationClassLoader(PreferenceAnnotationProcessorTest.class.getClassLoader(), compilation);
        var clazz = classLoader.loadClass("eu.jonahbauer.android.preference.annotations.generated.TestPreferences");

        var init = clazz.getMethod("init", SharedPreferences.class, Resources.class);
        init.invoke(null, sharedPreferences, resources);

        var general = clazz.getMethod("general").invoke(null);
        var generalClass = general.getClass();

        var pointClass = classLoader.loadClass("eu.jonahbauer.android.preference.annotations.sources.TestPreferencesExport$Point");
        var point = pointClass.getConstructor(int.class, int.class).newInstance(3, 4);

        generalClass.getMethod("booleanPref", boolean.class).invoke(general, true);
        generalClass.getMethod("bytePref", byte.class).invoke(general, (byte) 5);
        generalClass.getMethod("shortPref", short.class).invoke(general, (short) 6);
        generalClass.getMethod("charPref", char.class).invoke(general, 'x');
        generalClass.getMethod("intPref", int.class).invoke(general, 7);
        generalClass.getMethod("longPref", long.class).invoke(general, 8L);
        generalClass.getMethod("floatPref", float.class).invoke(general, 9.5f);
        generalClass.getMethod("doublePref", double.class).invoke(general, 10.5);
        generalClass.getMethod("stringPref", String.class).invoke(general, "Hello World!");
        generalClass.getMethod("setPref", Set.class).invoke(general, Set.of("a", "b", "c"));
        generalClass.getMethod("enumPref", StandardOpenOption.class).invoke(general, StandardOpenOption.APPEND);
        generalClass.getMethod("bigIntPref", BigInteger.class).invoke(general, BigInteger.valueOf(12345));
        generalClass.getMethod("objectPref", pointClass).invoke(general, point);
        generalClass.getMethod("listPref", String.class).invoke(general, "not exported");

        var json = (JSONObject) clazz.getMethod("exportPreferences").invoke(null);

        assertTrue(json.getBoolean(PREFERENCES_GENERAL_BOOLEAN));
        assertEquals(5, json.getInt(PREFERENCES_GENERAL_BYTE));
        assertEquals(6, json.getInt(PREFERENCES_GENERAL_SHORT));
        assertEquals('x', json.getInt(PREFERENCES_GENERAL_CHAR));
        assertEquals(7, json.getInt(PREFERENCES_GENERAL_INT));
        assertEquals(8L, json.getLong(PREFERENCES_GENERAL_LONG));
        assertEquals(9.5, json.getDouble(PREFERENCES_GENERAL_FLOAT));
        assertEquals(10.5, json.getDouble(PREFERENCES_GENERAL_DOUBLE));
        assertEquals("Hello World!", json.getString(PREFERENCES_GENERAL_STRING));
        assertEquals("APPEND", json.getString(PREFERENCES_GENERAL_ENUM));
        assertEquals("12345", json.getString(PREFERENCES_GENERAL_BIG_INT));

        var array = json.getJSONArray(PREFERENCES_GENERAL_SET);
        var set = new java.util.HashSet<String>();
        for (int i = 0; i < array.length(); i++) set.add(array.getString(i));
        assertEquals(Set.of("a", "b", "c"), set);

        var object = json.getJSONObject(PREFERENCES_GENERAL_OBJECT);
        assertEquals(3, object.getInt("x"));
        assertEquals(4, object.getInt("y"));

        assertFalse(json.has(PREFERENCES_GENERAL_LIST), "preferences without export = true must not be exported");
        assertFalse(json.has(PREFERENCES_GENERAL_VOID), "void preferences must not be exported");

        clazz.getMethod("clear").invoke(null);
        clazz.getMethod("importPreferences", JSONObject.class).invoke(null, json);

        assertEquals(true, generalClass.getMethod("booleanPref").invoke(general));
        assertEquals((byte) 5, generalClass.getMethod("bytePref").invoke(general));
        assertEquals((short) 6, generalClass.getMethod("shortPref").invoke(general));
        assertEquals('x', generalClass.getMethod("charPref").invoke(general));
        assertEquals(7, generalClass.getMethod("intPref").invoke(general));
        assertEquals(8L, generalClass.getMethod("longPref").invoke(general));
        assertEquals(9.5f, generalClass.getMethod("floatPref").invoke(general));
        assertEquals(10.5, generalClass.getMethod("doublePref").invoke(general));
        assertEquals("Hello World!", generalClass.getMethod("stringPref").invoke(general));
        assertEquals(Set.of("a", "b", "c"), generalClass.getMethod("setPref").invoke(general));
        assertEquals(StandardOpenOption.APPEND, generalClass.getMethod("enumPref").invoke(general));
        assertEquals(BigInteger.valueOf(12345), generalClass.getMethod("bigIntPref").invoke(general));
        assertEquals(point, generalClass.getMethod("objectPref").invoke(general));
        assertNull(generalClass.getMethod("listPref").invoke(general), "preferences without export = true must not be imported");

        // importing an empty JSONObject must not change any values
        clazz.getMethod("importPreferences", JSONObject.class).invoke(null, new JSONObject());
        assertEquals(7, generalClass.getMethod("intPref").invoke(general));
        assertEquals("Hello World!", generalClass.getMethod("stringPref").invoke(general));

        // null values must be omitted during export and must not reset preferences during import
        generalClass.getMethod("stringPref", String.class).invoke(general, (Object) null);
        json = (JSONObject) clazz.getMethod("exportPreferences").invoke(null);
        assertFalse(json.has(PREFERENCES_GENERAL_STRING), "null values must not be exported");
        generalClass.getMethod("stringPref", String.class).invoke(general, "Hello World!");
        clazz.getMethod("importPreferences", JSONObject.class).invoke(null, json);
        assertEquals("Hello World!", generalClass.getMethod("stringPref").invoke(general));

        // the import must be atomic
        var invalid = new JSONObject()
                .put(PREFERENCES_GENERAL_INT, 42)
                .put(PREFERENCES_GENERAL_LONG, "not a number");
        var importPreferences = clazz.getMethod("importPreferences", JSONObject.class);
        var exception = assertThrows(InvocationTargetException.class, () -> importPreferences.invoke(null, invalid));
        assertInstanceOf(org.json.JSONException.class, exception.getCause());
        assertEquals(7, generalClass.getMethod("intPref").invoke(general), "a failed import must not change any values");
    }

    @Test
    public void testExportWithCustomSerializerRequiresExportSerializer() {
        var compilation = compile("input/TestPreferencesExportMissingExportSerializer.java");
        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("requires an exportSerializer");
    }

    @Test
    public void testExportVoidPreference() {
        var compilation = compile("input/TestPreferencesExportVoid.java");
        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Cannot export void preference");
    }

    @Test
    public void testNoExportMethodsWithoutExportedPreferences() throws Exception {
        var compilation = compile("input/TestPreferences.java");
        assertThat(compilation).succeededWithoutWarnings();

        var classLoader = new CompilationClassLoader(PreferenceAnnotationProcessorTest.class.getClassLoader(), compilation);
        var clazz = classLoader.loadClass("eu.jonahbauer.android.preference.annotations.generated.TestPreferences");

        assertThrows(NoSuchMethodException.class, () -> clazz.getMethod("exportPreferences"));
        assertThrows(NoSuchMethodException.class, () -> clazz.getMethod("importPreferences", org.json.JSONObject.class));
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

        var getSharedPreferences = clazz.getMethod("getSharedPreferences");
        var sharedPreferences = getSharedPreferences.invoke(null);
        assertSame(this.sharedPreferences, sharedPreferences);
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
