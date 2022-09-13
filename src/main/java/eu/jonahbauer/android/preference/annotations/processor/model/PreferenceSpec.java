package eu.jonahbauer.android.preference.annotations.processor.model;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import eu.jonahbauer.android.preference.annotations.Preference;
import eu.jonahbauer.android.preference.annotations.processor.StringUtils;
import eu.jonahbauer.android.preference.annotations.processor.TypeUtils;
import eu.jonahbauer.android.preference.annotations.serializer.Serializer;
import lombok.Value;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.Map;

import static eu.jonahbauer.android.preference.annotations.processor.PreferenceProcessor.error;

@Value
public class PreferenceSpec {
    private static final Map<String, String> GETTER = Map.of(
            "boolean", "boolean value = $N.getBoolean($N, $L)",
            "byte", "byte value = (byte) $N.getInt($N, $L)",
            "char", "char value = (char) $N.getInt($N, $L)",
            "short", "short value = (short) $N.getInt($N, $L)",
            "int", "int value = $N.getInt($N, $L)",
            "long", "long value = $N.getLong($N, $L)",
            "float", "float value = $N.getFloat($N, $L)",
            "double", "double value = Double.longBitsToDouble($N.getLong($N, $L))",
            "java.lang.String", "String value = $N.getString($N, $S)"
    );

    private static final Map<String, String> SETTER = Map.of(
            "boolean", "$N.edit().putBoolean($N, serializedValue).apply()",
            "byte", "$N.edit().putInt($N, (int) serializedValue).apply()",
            "char", "$N.edit().putInt($N, (int) serializedValue).apply()",
            "short", "$N.edit().putInt($N, (int) serializedValue).apply()",
            "int", "$N.edit().putInt($N, (int) serializedValue).apply()",
            "long", "$N.edit().putLong($N, serializedValue).apply()",
            "float", "$N.edit().putFloat($N, serializedValue).apply()",
            "double", "$N.edit().putLong($N, Double.doubleToRawLongBits(serializedValue)).apply()",
            "java.lang.String", "$N.edit().putString($N, serializedValue).apply()"
    );

    String name;

    FieldSpec key;
    FieldSpec serializer;

    MethodSpec getter;
    MethodSpec setter;

    TypeMirror serializedType;
    TypeMirror deserializedType;

    public static PreferenceSpec create(ProcessingEnvironment env, Element element, FieldSpec sharedPreferences, int index, Preference preference) {
        if (!check(env, element, preference)) return null;

        var deserializedType = TypeUtils.mirror(preference, Preference::type);
        var serializedType = (TypeMirror) null;
        var serializer = (FieldSpec) null;

        var serializerImpl = TypeUtils.mirror(preference, Preference::serializer);
        if (TypeUtils.isSame(Serializer.class, serializerImpl)) {
            serializedType = deserializedType;
        } else {
            var serializerInt = findSerializerType(env.getTypeUtils(), serializerImpl);
            if (check(env, element, preference, serializerInt, deserializedType)) {
                assert serializerInt != null;
                serializedType = TypeUtils.tryUnbox(env, serializerInt.getTypeArguments().get(1));
                serializer = FieldSpec.builder(TypeName.get(serializerImpl), "serializer$" + index, Modifier.PRIVATE, Modifier.FINAL)
                        .initializer("new $T()", serializerImpl)
                        .build();
            } else {
                return null;
            }
        }

        if (!checkType(serializedType)) {
            error(env, element, "Unsupported preference type: %s", serializedType);
            return null;
        }

        var key = FieldSpec.builder(String.class, "key$" + index, Modifier.PRIVATE, Modifier.FINAL).build();
        return new PreferenceSpec(
                StringUtils.getMethodName(preference.name()),
                sharedPreferences,
                key,
                serializer,
                serializedType,
                deserializedType,
                getDefaultValue(preference, serializedType),
                preference.description()
        );
    }

    public PreferenceSpec(
            String name, FieldSpec sharedPreferences,
            FieldSpec key, FieldSpec serializer,
            TypeMirror serializedType, TypeMirror deserializedType,
            Object defaultValue, String description
    ) {
        this.name = name;
        this.key = key;
        this.serializer = serializer;
        this.serializedType = serializedType;
        this.deserializedType = deserializedType;

        if (serializedType.getKind() == TypeKind.VOID) {
            getter = null;
            setter = null;
        } else {
            var getter = MethodSpec.methodBuilder(name)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(TypeName.get(deserializedType))
                    .addStatement(GETTER.get(serializedType.toString()), sharedPreferences, key, defaultValue)
                    .addStatement(serializer == null ? "return value" : "return $N.deserialize(value)", serializer);
            addJavadoc(getter, description, serializedType, defaultValue);
            this.getter = getter.build();

            var setter = MethodSpec.methodBuilder(name)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(TypeName.get(deserializedType), "value")
                    .addStatement(serializer == null ? "var serializedValue = value" : "var serializedValue = $N.serialize(value)", serializer)
                    .addStatement(SETTER.get(serializedType.toString()), sharedPreferences, key);
            addJavadoc(setter, description, serializedType, defaultValue);
            this.setter = setter.build();
        }
    }

    public void apply(TypeSpec.Builder builder) {
        if (getter != null) builder.addMethod(getter);
        if (setter != null) builder.addMethod(setter);
        if (serializer != null) builder.addField(serializer);
        builder.addField(key);
    }

    private static boolean check(ProcessingEnvironment env, Element element, Preference preference) {
        if (!StringUtils.isJavaIdentifier(preference.name())) {
            error(env, element, "Illegal preference name: %s", preference.name());
            return false;
        }

        return true;
    }

    private static boolean check(ProcessingEnvironment env, Element element, Preference preference, DeclaredType serializer, TypeMirror deserializedType) {
        if (serializer == null) {
            error(env, element, "No serializer for preference %s", preference.name());
            return false;
        } else if (serializer.getTypeArguments().size() != 2) {
            error(env, element, "Unable to identify type arguments of serializer %s for preference %s", serializer, preference.name());
            return false;
        } else if (!env.getTypeUtils().isSubtype(deserializedType, serializer.getTypeArguments().get(0))) {
            error(env, element, "Incompatible serializer %s for type %s of preference %s", serializer, deserializedType, preference.name());
            return false;
        } else {
            return true;
        }
    }

    private static boolean checkType(TypeMirror type) {
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

    private static void addJavadoc(MethodSpec.Builder method, String description, TypeMirror type, Object defaultValue) {
        if (description.isEmpty()) return;

        method.addJavadoc(description);

        if (type.getKind() == TypeKind.DECLARED && String.class.getName().equals(type.toString())) {
            method.addJavadoc("\n(default: $S)", defaultValue);
        } else {
            method.addJavadoc("\n(default: $L)", defaultValue);
        }
    }
}
