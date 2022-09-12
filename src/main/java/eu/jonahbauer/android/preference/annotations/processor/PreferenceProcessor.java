package eu.jonahbauer.android.preference.annotations.processor;

import com.squareup.javapoet.*;
import eu.jonahbauer.android.preference.annotations.Preference;
import eu.jonahbauer.android.preference.annotations.Preferences;
import eu.jonahbauer.android.preference.annotations.PreferenceGroup;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@SupportedAnnotationTypes({
        "eu.jonahbauer.android.preference.annotations.Preferences"
})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public final class PreferenceProcessor extends AbstractProcessor {
    private static final ClassName SHARED_PREFERENCES = ClassName.get("android.content", "SharedPreferences");
    private static final ClassName RESOURCES = ClassName.get("android.content.res", "Resources");
    private static final ClassName ILLEGAL_STATE_EXCEPTION = ClassName.get("java.lang", "IllegalStateException");
    private static final ClassName OBJECTS = ClassName.get("java.util", "Objects");

    private static final Set<String> SUPPORTED_DECLARED_TYPES = Set.of(
            String.class.getName()
    );

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var clazzes = roundEnv.getElementsAnnotatedWith(Preferences.class);

        try {
            for (Element clazz : clazzes) {
                var root = clazz.getAnnotation(Preferences.class);

                if (checkPreferences(clazz, root)) {
                    var type = buildRootClass(root);
                    type.writeTo(processingEnv.getFiler());
                }
            }
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static JavaFile buildRootClass(Preferences root) {
        var name = ClassName.get(
                root.name().lastIndexOf('.') != -1 ? root.name().substring(0, root.name().lastIndexOf('.')) : "",
                root.name().lastIndexOf('.') != -1 ? root.name().substring(root.name().lastIndexOf('.') + 1) : root.name()
        );

        var groups = root.value();
        var r = TypeName.get(mirror(root, Preferences::r));

        var builder = TypeSpec.classBuilder(name);

        if (root.makeFile()) {
            builder.addModifiers(Modifier.PUBLIC, Modifier.FINAL);
        }

        // constructor
        builder.addMethod(MethodSpec.constructorBuilder()
                                    .addModifiers(root.makeFile() ? Modifier.PRIVATE : Modifier.PROTECTED)
                                    .addStatement("throw new $T($S)", ILLEGAL_STATE_EXCEPTION, "This class is not supposed to be instantiated.")
                                    .build()
        );

        // shared preferences
        var sharedPreferencesField = FieldSpec.builder(SHARED_PREFERENCES, "sharedPreferences", Modifier.PRIVATE, Modifier.STATIC).build();
        builder.addField(sharedPreferencesField);

        // init method
        var initMethod = MethodSpec.methodBuilder("init")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(SHARED_PREFERENCES, "pSharedPreferences")
                .addParameter(RESOURCES, "pResources")
                .addJavadoc("Initialize this preference class to use the given {@link $T}\n", SHARED_PREFERENCES)
                .addJavadoc("This function is supposed to be called from the applications {@code onCreate()} method.\n")
                .addJavadoc("@param pSharedPreferences the {@link $T} to be used. Not {@code null}.\n", SHARED_PREFERENCES)
                .addJavadoc("@param pResources the {@link $T} from which the preference keys should be loaded. Not {@code null}.\n", RESOURCES)
                .addJavadoc("@throws $T if this preference class has already been initialized.\n", ILLEGAL_STATE_EXCEPTION)
                .addCode(CodeBlock.builder()
                                  .beginControlFlow("if ($N != null)", sharedPreferencesField)
                                  .addStatement("throw new $T($S)", ILLEGAL_STATE_EXCEPTION, "Preferences have already been initialized.")
                                  .endControlFlow()
                                  .addStatement("$T.requireNonNull(pSharedPreferences, $S)", OBJECTS, "SharedPreferences must not be null.")
                                  .addStatement("$T.requireNonNull(pResources, $S)", OBJECTS, "Resources must not be null.")
                                  .addStatement("$N = pSharedPreferences", sharedPreferencesField)
                                  .build()
                );

        // group classes, fields, accessors and init statements
        for (int i = 0; i < groups.length; i++) {
            var groupClassName = name.nestedClass(groups[i].name());
            var groupClass = buildGroupClass(groupClassName, groups[i], r, sharedPreferencesField);
            var groupField = FieldSpec.builder(groupClassName, "group$" + i, Modifier.PRIVATE, Modifier.STATIC).build();

            initMethod.addStatement("$N = new $T(pResources)", groupField, groupClassName);
            builder.addType(groupClass);
            builder.addField(groupField);
            builder.addMethod(buildGroupAccessor(groups[i], groupField, sharedPreferencesField));
        }

        builder.addMethod(initMethod.build());

        return JavaFile.builder(name.packageName(), builder.build()).indent("    ").build();
    }

    /**
     * <pre>{@code public static ${group.name} ${group.name}() {}}</pre>
     */
    private static MethodSpec buildGroupAccessor(PreferenceGroup group, FieldSpec groupField, FieldSpec sharedPreferences) {
        return MethodSpec.methodBuilder(group.name())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(groupField.type)
                .addCode(CodeBlock.builder()
                                 .beginControlFlow("if ($N == null)", sharedPreferences)
                                 .addStatement("throw new $T($S)", ILLEGAL_STATE_EXCEPTION, "Preferences have not yet been initialized.")
                                 .endControlFlow()
                                 .addStatement("return $N", groupField)
                                 .build()
                )
                .build();
    }

    /**
     * <pre>{@code public static final ${group.name} {
     *     private final Keys keys;
     *     private final String key$i;
     *
     *     private ${group.name}(Resources resources) {
     *         this.keys = new Keys();
     *         this.key$i = resources.get(R.string.${preferences[i].name});
     *     }
     *
     *     // preference accessors
     *
     *     // key class
     * }}</pre>
     */
    private static TypeSpec buildGroupClass(ClassName name, PreferenceGroup group, TypeName r, FieldSpec sharedPreferences) {
        var preferences = group.value();

        var type = TypeSpec
                .classBuilder(name)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);

        var constructorCode = CodeBlock.builder();
        var keys = new HashMap<Preference, FieldSpec>();

        for (int i = 0; i < preferences.length; i++) {
            var key = FieldSpec.builder(String.class, "key$" + i, Modifier.PRIVATE, Modifier.FINAL).build();
            type.addField(key);
            keys.put(preferences[i], key);

            constructorCode.addStatement("$N = resources.getString($T.string.$N)", key, r, group.prefix() + preferences[i].name() + group.suffix());
            var getter = buildGetter(preferences[i], sharedPreferences, key);
            var setter = buildSetter(preferences[i], sharedPreferences, key);
            if (getter != null) type.addMethod(getter);
            if (setter != null) type.addMethod(setter);
        }

        type.addMethod(
                MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PRIVATE)
                        .addParameter(RESOURCES, "resources")
                        .addCode(constructorCode.build())
                        .build()
        );

        var keyClassName = name.nestedClass("Keys");
        var keyClass = buildKeyClass(keyClassName, keys);
        var keyField = FieldSpec
                .builder(keyClassName, "keys", Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T()", keyClassName)
                .build();

        type.addType(keyClass);
        type.addField(keyField);
        type.addMethod(
                MethodSpec.methodBuilder("keys")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(keyClassName)
                        .addStatement("return $N", keyField)
                        .build()
        );

        return type.build();
    }

    /**
     * <pre>{@code
     * public final Keys {
     *     private Keys() {}
     *     public String ${preferences[i].name}() {
     *         return key$i;
     *     }
     * }}</pre>
     */
    private static TypeSpec buildKeyClass(ClassName name, Map<Preference, FieldSpec> preferences) {
        var builder = TypeSpec.classBuilder(name).addModifiers(Modifier.PUBLIC, Modifier.FINAL);
        builder.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());

        preferences.forEach((preference, key) -> builder.addMethod(
                MethodSpec.methodBuilder(StringUtils.getMethodName(preference.name()))
                        .addModifiers(Modifier.PUBLIC)
                        .returns(String.class)
                        .addCode(CodeBlock.builder()
                                         .addStatement("return $N", key)
                                         .build())
                        .build()
        ));

        return builder.build();
    }

    /**
     * <pre>{@code public ${preference.type} ${preference.name}() {
     *     return sharedPreferences.get${preference.type}(${preference.key});
     * }}</pre>
     */
    private static MethodSpec buildGetter(Preference preference, FieldSpec sharedPreferences, FieldSpec key) {
        var type = mirror(preference, Preference::type);

        var builder = MethodSpec.methodBuilder(StringUtils.getMethodName(preference.name()))
                  .returns(TypeName.get(type))
                  .addModifiers(Modifier.PUBLIC);

        addJavadoc(builder, preference, type);

        switch (type.getKind()) {
            case BOOLEAN:
                builder.addStatement("return $N.getBoolean($N, $L)", sharedPreferences, key, getDefaultValue(preference, type));
                break;
            case BYTE:
                builder.addStatement("return (byte) $N.getInt($N, $L)", sharedPreferences, key, getDefaultValue(preference, type));
                break;
            case CHAR:
                builder.addStatement("return (char) $N.getInt($N, $L)", sharedPreferences, key, getDefaultValue(preference, type));
                break;
            case SHORT:
                builder.addStatement("return (short) $N.getInt($N, $L)", sharedPreferences, key, getDefaultValue(preference, type));
                break;
            case INT:
                builder.addStatement("return $N.getInt($N, $L)", sharedPreferences, key, getDefaultValue(preference, type));
                break;
            case LONG:
                builder.addStatement("return $N.getLong($N, $L)", sharedPreferences, key, getDefaultValue(preference, type));
                break;
            case FLOAT:
                builder.addStatement("return $N.getFloat($N, $L)", sharedPreferences, key, getDefaultValue(preference, type));
                break;
            case DOUBLE:
                builder.addStatement("return Double.longBitsToDouble($N.getLong($N, $L))", sharedPreferences, key, getDefaultValue(preference, type));
                break;
            case DECLARED:
                if (String.class.getName().equals(type.toString())) {
                    builder.addStatement("return $N.getString($N, $S)", sharedPreferences, key, getDefaultValue(preference, type));
                    break;
                } else {
                    return null;
                }
            default:
                return null;
        }

        return builder.build();
    }

    /**
     * <pre>{@code public void ${preference.name}(${preference.type} value) {
     *     return sharedPreferences.edit().put${preference.type}(${preference.key}, value).apply();
     * }}</pre>
     */
    private static MethodSpec buildSetter(Preference preference, FieldSpec sharedPreferences, FieldSpec key) {
        var type = mirror(preference, Preference::type);

        var builder = MethodSpec.methodBuilder(StringUtils.getMethodName(preference.name()))
                                .addParameter(TypeName.get(type), "value")
                                .addModifiers(Modifier.PUBLIC);

        addJavadoc(builder, preference, type);

        switch (type.getKind()) {
            case BOOLEAN:
                builder.addStatement("$N.edit().putBoolean($N, value).apply()", sharedPreferences, key);
                break;
            case BYTE:
            case SHORT:
            case CHAR:
            case INT:
                builder.addStatement("$N.edit().putInt($N, (int) value).apply()", sharedPreferences, key);
                break;
            case LONG:
                builder.addStatement("$N.edit().putLong($N, value).apply()", sharedPreferences, key);
                break;
            case FLOAT:
                builder.addStatement("$N.edit().putFloat($N, value).apply()", sharedPreferences, key);
                break;
            case DOUBLE:
                builder.addStatement("$N.edit().putLong($N, Double.doubleToRawLongBits(value)).apply()", sharedPreferences, key);
                break;
            case DECLARED:
                if (String.class.getName().equals(type.toString())) {
                    builder.addStatement("$N.edit().putString($N, value).apply()", sharedPreferences, key);
                    break;
                } else {
                    return null;
                }
            default:
                return null;
        }

        return builder.build();
    }

    private static void addJavadoc(MethodSpec.Builder method, Preference preference, TypeMirror type) {
        if (preference.description().isEmpty()) return;

        method.addJavadoc(preference.description());

        if (type.getKind() == TypeKind.DECLARED && String.class.getName().equals(type.toString())) {
            method.addJavadoc("\n(default: $S)", getDefaultValue(preference, type));
        } else {
            method.addJavadoc("\n(default: $L)", getDefaultValue(preference, type));
        }
    }

    private boolean checkPreferences(Element element, Preferences root) {
        boolean success = true;
        if (!StringUtils.isFQCN(root.name())) {
            success = false;
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Illegal preference class name: " + root.name(),
                    element
            );
        }

        for (var preferenceGroup : root.value()) {
            success &= checkPreferenceGroup(element, preferenceGroup);
        }

        return success;
    }

    private boolean checkPreferenceGroup(Element element, PreferenceGroup group) {
        boolean success = true;
        if (!StringUtils.isJavaIdentifier(group.name())) {
            success = false;
            error(element, "Illegal preference group name: %s", group.name());
        } else if (!group.prefix().isEmpty() && !StringUtils.isJavaIdentifier(group.prefix())) {
            success = false;
            error(element, "Illegal preference group prefix: %s", group.prefix());
        } else if (!group.suffix().isEmpty() && !group.suffix().matches("\\p{javaJavaIdentifierPart}+")) {
            success = false;
            error(element, "Illegal preference group suffix: %s", group.suffix());
        }

        for (Preference preference : group.value()) {
            success &= checkPreference(element, preference);
        }

        return success;
    }

    private boolean checkPreference(Element element, Preference preference) {
        boolean success = true;
        if (!StringUtils.isJavaIdentifier(preference.name())) {
            success = false;
            error(element, "Illegal preference name: %s", preference.name());
        }

        success &= checkType(element, preference);
        return success;
    }

    private boolean checkType(Element element, Preference preference) {
        var type = mirror(preference, Preference::type);

        switch (type.getKind()) {
            case BOOLEAN:
            case BYTE:
            case CHAR:
            case SHORT:
            case INT:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case VOID:
                return true;
            case DECLARED:
                if (!SUPPORTED_DECLARED_TYPES.contains(type.toString())) {
                    error(element, "Unsupported preference type: %s", type);
                    return false;
                }
                return true;
            default:
                error(element, "Unsupported preference type: %s", type);
                return false;
        }
    }

    private void error(Element element, String message, Object...args) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format(message, args), element);
    }

    private static Object getDefaultValue(Preference preference, TypeMirror type) {
        if (!Preference.NO_DEFAULT_VALUE.equals(preference.defaultValue())) {
            return preference.defaultValue();
        } else switch (type.getKind()) {
            case BOOLEAN: return false;
            case BYTE: case CHAR: case SHORT: case INT: case LONG: case FLOAT: case DOUBLE: return 0;
            case ARRAY: case NULL: case DECLARED: case VOID: return null;
            default: throw new RuntimeException();
        }
    }

    private static <S> TypeMirror mirror(S object, Function<S, ? extends Class<?>> function) {
        try {
            function.apply(object);
            throw new RuntimeException();
        } catch (MirroredTypeException e) {
            return e.getTypeMirror();
        }
    }
}