[![MIT license](https://img.shields.io/badge/License-MIT-blue.svg)](https://github.com/jbb01/android-preference-annotations/blob/main/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/eu.jonahbauer/android-preference-annotations)](https://mvnrepository.com/artifact/eu.jonahbauer/android-preference-annotations)
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

```
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
        @Preference(name = "void_pref, type = void.class")
    }
}
public final AppPreferences extends AppPreferences$Generated {}
 ```

This will generate a class `org.example.AppPreferences$Generated` which contains accessors
for all the specified preferences (except `void` preferences). Additionally, an accessor for
the preference keys is generated:

```
AppPreferences.general().intPref()        // returns the default value of 0
AppPreferences.general().intPref(10)      // sets the preference R.string.preferences_general_int_pref_key to 10
AppPreferences.general().intPref()        // returns the newly assigned value of 10
AppPreferences.general().keys().intPref() // returns the string resource R.string.preferences_general_int_pref_key 
```

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

By default, `boolean`, `byte`, `short`, `char`, `int`, `long`, `float`, `double`, `String`, `void` and enums are
supported. Other types may be used by specifying a custom serializer that will convert between the preference type and 
one of the natively supported types (except `void` and `enum`).

## issues
Find a bug or want to request a new feature? Please let us know by submitting an issue.