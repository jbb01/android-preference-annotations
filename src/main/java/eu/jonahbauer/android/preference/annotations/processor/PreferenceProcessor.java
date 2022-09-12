package eu.jonahbauer.android.preference.annotations.processor;

import com.squareup.javapoet.*;
import eu.jonahbauer.android.preference.annotations.Preference;
import eu.jonahbauer.android.preference.annotations.Preferences;
import eu.jonahbauer.android.preference.annotations.PreferenceGroup;
import eu.jonahbauer.android.preference.annotations.serializer.Serializer;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.*;
import javax.lang.model.util.Types;
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

    private JavaFile buildRootClass(Preferences root) {
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
    private MethodSpec buildGroupAccessor(PreferenceGroup group, FieldSpec groupField, FieldSpec sharedPreferences) {
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
    private TypeSpec buildGroupClass(ClassName name, PreferenceGroup group, TypeName r, FieldSpec sharedPreferences) {
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

            var serializerImpl = mirror(preferences[i], Preference::serializer);
            var serializer = (FieldSpec) null;
            if (!Serializer.class.getName().equals(serializerImpl.toString())) {
                serializer = FieldSpec
                        .builder(TypeName.get(serializerImpl), "serializer$" + i, Modifier.PRIVATE, Modifier.FINAL)
                        .initializer("new $T()", serializerImpl)
                        .build();
                type.addField(serializer);
            }

            constructorCode.addStatement("$N = resources.getString($T.string.$N)", key, r, group.prefix() + preferences[i].name() + group.suffix());
            var getter = buildGetter(preferences[i], sharedPreferences, key, serializer);
            var setter = buildSetter(preferences[i], sharedPreferences, key, serializer);
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
    private TypeSpec buildKeyClass(ClassName name, Map<Preference, FieldSpec> preferences) {
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
    private MethodSpec buildGetter(Preference preference, FieldSpec sharedPreferences, FieldSpec key, FieldSpec serializer) {
        var type = mirror(preference, Preference::type);
        var serializerType = getSerializerInterface(mirror(preference, Preference::serializer));

        var builder = MethodSpec.methodBuilder(StringUtils.getMethodName(preference.name()))
                  .returns(TypeName.get(type))
                  .addModifiers(Modifier.PUBLIC);


        var prefType = serializer == null ? type : serializerType.getTypeArguments().get(1);
        var defaultValue = getDefaultValue(preference, prefType);
        addJavadoc(builder, preference, prefType);

        switch (prefType.getKind()) {
            case BOOLEAN:
                builder.addStatement("boolean value = $N.getBoolean($N, $L)", sharedPreferences, key, defaultValue);
                break;
            case BYTE:
                builder.addStatement("byte value = (byte) $N.getInt($N, $L)", sharedPreferences, key, defaultValue);
                break;
            case CHAR:
                builder.addStatement("char value = (char) $N.getInt($N, $L)", sharedPreferences, key, defaultValue);
                break;
            case SHORT:
                builder.addStatement("short value = (short) $N.getInt($N, $L)", sharedPreferences, key, defaultValue);
                break;
            case INT:
                builder.addStatement("int value = $N.getInt($N, $L)", sharedPreferences, key, defaultValue);
                break;
            case LONG:
                builder.addStatement("long value = $N.getLong($N, $L)", sharedPreferences, key, defaultValue);
                break;
            case FLOAT:
                builder.addStatement("float value = $N.getFloat($N, $L)", sharedPreferences, key, defaultValue);
                break;
            case DOUBLE:
                builder.addStatement("double value = Double.longBitsToDouble($N.getLong($N, $L))", sharedPreferences, key, defaultValue);
                break;
            case DECLARED:
                if (String.class.getName().equals(prefType.toString())) {
                    builder.addStatement("String value = $N.getString($N, $S)", sharedPreferences, key, defaultValue);
                    break;
                } else {
                    return null;
                }
            default:
                return null;
        }

        if (serializer == null) {
            builder.addStatement("return value");
        } else {
            builder.addStatement("return $N.deserialize(value)", serializer);
        }

        return builder.build();
    }

    /**
     * <pre>{@code public void ${preference.name}(${preference.type} value) {
     *     return sharedPreferences.edit().put${preference.type}(${preference.key}, value).apply();
     * }}</pre>
     */
    private MethodSpec buildSetter(Preference preference, FieldSpec sharedPreferences, FieldSpec key, FieldSpec serializer) {
        var type = mirror(preference, Preference::type);
        var serializerType = getSerializerInterface(mirror(preference, Preference::serializer));

        var builder = MethodSpec.methodBuilder(StringUtils.getMethodName(preference.name()))
                                .addParameter(TypeName.get(type), "value")
                                .addModifiers(Modifier.PUBLIC);


        var prefType = serializer == null ? type : serializerType.getTypeArguments().get(1);
        addJavadoc(builder, preference, prefType);

        if (serializer == null) {
            builder.addStatement("var serializedValue = value");
        } else {
            builder.addStatement("var serializedValue = $N.serialize(value)", serializer);
        }

        switch (prefType.getKind()) {
            case BOOLEAN:
                builder.addStatement("$N.edit().putBoolean($N, serializedValue).apply()", sharedPreferences, key);
                break;
            case BYTE:
            case SHORT:
            case CHAR:
            case INT:
                builder.addStatement("$N.edit().putInt($N, (int) serializedValue).apply()", sharedPreferences, key);
                break;
            case LONG:
                builder.addStatement("$N.edit().putLong($N, serializedValue).apply()", sharedPreferences, key);
                break;
            case FLOAT:
                builder.addStatement("$N.edit().putFloat($N, serializedValue).apply()", sharedPreferences, key);
                break;
            case DOUBLE:
                builder.addStatement("$N.edit().putLong($N, Double.doubleToRawLongBits(serializedValue)).apply()", sharedPreferences, key);
                break;
            case DECLARED:
                if (String.class.getName().equals(prefType.toString())) {
                    builder.addStatement("$N.edit().putString($N, serializedValue).apply()", sharedPreferences, key);
                    break;
                } else {
                    return null;
                }
            default:
                return null;
        }

        return builder.build();
    }

    private void addJavadoc(MethodSpec.Builder method, Preference preference, TypeMirror type) {
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

        var serializerImpl = mirror(preference, Preference::serializer);
        if (Serializer.class.getName().equals(serializerImpl.toString())) {
            success &= checkType(element, preference);
        } else {
            success &= checkSerializer(element, preference, serializerImpl);
        }
        return success;
    }

    private boolean checkSerializer(Element element, Preference preference, TypeMirror serializerImpl) {
        var type = mirror(preference, Preference::type);

        var serializerInterface = getSerializerInterface(serializerImpl);
        if (serializerInterface == null || serializerInterface.getTypeArguments().size() != 2) {
            error(element, "Unable to identify type arguments of Serializer for preference: %s", preference.name());
            return false;
        } else {
            var sourceType = serializerInterface.getTypeArguments().get(0);
            var targetType = serializerInterface.getTypeArguments().get(1);
            if (!checkType(tryUnbox(targetType))) {
                error(element, "Unsupported serializer target type: %s", targetType);
                return false;
            }

            if (!processingEnv.getTypeUtils().isSubtype(type, sourceType)) {
                error(element, "Incompatible serializer %s for preference %s", serializerImpl, preference.name());
                return false;
            }
        }

        return true;
    }

    private boolean checkType(Element element, Preference preference) {
        var type = mirror(preference, Preference::type);

        if (checkType(type)) {
            return true;
        } else {
            error(element, "Unsupported preference type: %s", type);
            return false;
        }
    }

    private boolean checkType(TypeMirror type) {
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
                return String.class.getName().equals(type.toString());
            default:
                return false;
        }
    }

    private TypeMirror tryUnbox(TypeMirror type) {
        try {
            return processingEnv.getTypeUtils().unboxedType(type);
        } catch (IllegalArgumentException e) {
            return type;
        }
    }

    private void error(Element element, String message, Object...args) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format(message, args), element);
    }

    private DeclaredType getSerializerInterface(TypeMirror type) {
        return findSerializerType(processingEnv.getTypeUtils(), type);
    }

    private static DeclaredType findSerializerType(Types typeUtils, TypeMirror type) {
        if (Serializer.class.getName().equals(typeUtils.erasure(type).toString())) {
            return (DeclaredType) type;
        }

        for (TypeMirror supertype : typeUtils.directSupertypes(type)) {
            if (Object.class.toString().equals(supertype.toString())) continue;

            var serializerType = findSerializerType(typeUtils, supertype);
            if (serializerType != null) {
                return serializerType;
            }
        }

        return null;
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