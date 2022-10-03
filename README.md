[![MIT license](https://img.shields.io/badge/License-MIT-blue.svg)](https://github.com/jbb01/android-preference-annotations/blob/main/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/eu.jonahbauer/android-preference-annotations)](https://mvnrepository.com/artifact/eu.jonahbauer/android-preference-annotations)
[![javadoc](https://javadoc.io/badge2/eu.jonahbauer/android-preference-annotations/javadoc.svg?color=blue)](https://javadoc.io/doc/eu.jonahbauer/android-preference-annotations)
[![Java CI with Gradle](https://img.shields.io/github/workflow/status/jbb01/android-preference-annotations/Java%20CI%20with%20Gradle)](https://github.com/jbb01/android-preference-annotations/actions/workflows/gradle.yml)

# android-preference-annotations

An annotation processor for type-safe access to shared preferences.

## installation

Download the [latest jar](https://search.maven.org/remote_content?g=eu.jonahbauer&a=android-preference-annotations&v=LATEST)
or use it as a dependency via Gradle
```
compileOnly 'eu.jonahbauer:android-preference-annotations:1.0.0' 
annotationProcessor 'eu.jonahbauer:android-preference-annotations:1.0.0' 
```
or Gradle Kotlin DSL
```
compileOnly("eu.jonahbauer:android-preference-annotations:1.0.0") 
annotationProcessor("eu.jonahbauer:android-preference-annotations:1.0.0")
```

## requirements

This annotation processor requires the Android SDK to be present on the classpath 
and a java version of at least 11.

## usage

To make use of this annotation processor simply annotate a class with the `@Preferences` annotation
and define your preferences therein:

```java
@Preferences(name = "org.example.AppPreferences$Generated", r = R.class, value = {
    @PreferenceGroup(name = "general", prefix = "preferences_general_", suffix = "_key", value = {
        @Preference(name = "boolean_pref", type = boolean.class),
        @Preference(name = "byte_pref", type = byte.class),
        @Preference(name = "short_pref", type = short.class),
        @Preference(name = "char_pref", type = char.class),
        @Preference(name = "int_pref", type = int.class),
        @Preference(name = "long_pref", type = long.class),
        @Preference(name = "float_pref", type = float.class),
        @Preference(name = "double_pref", type = double.class),
        @Preference(name = "string_pref", type = String.class), 
        @Preference(name = "string_set_pref", type = Set.class),
        @Preference(name = "void_pref", type = void.class)
    })
})
public final class AppPreferences extends AppPreferences$Generated {}
 ```

This will generate a class `org.example.AppPreferences$Generated` which contains accessors
for all the specified preferences (except `void` preferences). Additionally, an accessor for
the preference keys is generated.

### initialization
Before you can actually access any shared preferences you first have to tell the generated class which
`SharedPreferences` to use. This can easily be done in the applications `onCreate` method:

```
public void onCreate() {
    // ...
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    AppPreferences.init(preferences, this.getResources());
    // ...
}
```

### access

You can access the preferences from anywhere in the application without requiring a `Context`:

```
AppPreferences.general().intPref()        // returns the default value of 0
AppPreferences.general().intPref(42)      // sets the preference R.string.preferences_general_int_pref_key to 42
AppPreferences.general().intPref()        // returns the newly assigned value of 42
AppPreferences.general().keys().intPref() // returns the string resource R.string.preferences_general_int_pref_key 
```

If necessary you can also disable the generation of fluent getters and setters by setting
`fluent = false` on the `@Preferences` annotation, which will
help with Kotlin interoperability:

```
AppPreferences.getGeneral().getIntPref()
AppPreferences.getGeneral().setIntPref(42)
AppPreferences.getGeneral().getKeys().getIntPref()
```

or in Kotlin

```
AppPreferences.general.intPref
AppPreferences.general.intPref = 42
AppPreferences.general.keys.intPref
```

Furthermore, you can enable generation of `Editor` classes by setting `editor = true` on the `@Preferences` annotation:

```
AppPreferences.general().edit()
        .intPref(42)
        .stringPref("Hello World!")
        .apply()
```

### types
By default, `boolean`, `byte`, `short`, `char`, `int`, `long`, `float`, `double`, `String`, `void`, `Set<String>` and
enums are supported (to declare a string set preference just use `Set.class`).
Since `SharedPreferences` don't support all of these types natively some special handling is required:

| type                                                       | storage format                                                                   |
|------------------------------------------------------------|----------------------------------------------------------------------------------|
| `boolean`, `int`, `long`, `float`, `String`, `Set<String>` | natively supported                                                               |
| `byte`, `short`, `char`                                    | stored as `int`                                                                  |
| `double`                                                   | stored as `long` via `Double.longBitsToDouble` and  `Double.doubleToRawLongBits` |
| `enum`                                                     | stored as `String` via `Enum.name` and `Enum.valueOf`                            |
| `void`                                                     | no accessors generated                                                           |

Other types may be used by specifying a custom serializer that will convert between the preference
type and one of the supported types (except `void` and `enum`):

```
@Preference(name = "bean_pref", type = Bean.class, serializer = JsonBeanSerializer.class)
```
```java
@Data
public class Bean {
    private String foo;
    private String bar;
}
```
```java
public class JsonBeanSerializer<T> implements PreferenceSerializer<T, String> {
    private final Class<? extends T> clazz;
    private final ObjectMapper mapper = new ObjectMapper();

    public JsonBeanSerializer(Class<? extends T> clazz) {
        this.clazz = clazz;
    }
    
    @SneakyThrows
    public T deserialize(String value) {
        if (value == null) return null;
        return mapper.readValue(value, clazz);
    }
    
    @SneakyThrows
    public String serialize(T value) {
        if (value == null) return null;
        return mapper.writeValueAsString(value);
    }
}
```

### encryption

Since the generated class can be initialized with any `SharedPreferences` implementation, you can easily
provide an instance of [`EncryptedSharedPreferences`](https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences)
in order to encrypt your preferences.

## issues
Find a bug or want to request a new feature? Please let us know by submitting an issue.