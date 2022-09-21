package eu.jonahbauer.android.preference.annotations.processor.model;

import com.squareup.javapoet.*;
import eu.jonahbauer.android.preference.annotations.Preference;
import eu.jonahbauer.android.preference.annotations.processor.StringUtils;
import lombok.Value;

import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Map;
import java.util.Set;

@Value
public class PreferenceSpec {
    private static final Map<String, String> GETTER = Map.of(
            "boolean", "var value = $N.getBoolean($N, $L)",
            "byte", "var value = (byte) $N.getInt($N, $L)",
            "char", "var value = (char) $N.getInt($N, $L)",
            "short", "var value = (short) $N.getInt($N, $L)",
            "int", "var value = $N.getInt($N, $L)",
            "long", "var value = $N.getLong($N, $L)",
            "float", "var value = $N.getFloat($N, $L)",
            "double", "var value = Double.longBitsToDouble($N.getLong($N, $L))",
            "java.lang.String", "var value = $N.getString($N, $S)",
            "java.util.Set<java.lang.String>", "var value = $N.getStringSet($N, $L)"
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
            "java.lang.String", "$N.edit().putString($N, serializedValue).apply()",
            "java.util.Set<java.lang.String>", "$N.edit().putStringSet($N, serializedValue).apply()"
    );

    String name;

    FieldSpec key;
    FieldSpec serializer;

    MethodSpec getter;
    MethodSpec setter;

    TypeMirror serializedType;
    TypeMirror deserializedType;

    public static PreferenceSpec create(Context context, int index, Preference preference) {
        if (!check(context, preference)) return null;

        var serializerSpec = SerializerSpec.create(context, index, preference);

        if (!checkType(context, serializerSpec.getSerializedType())) {
            context.error("Unsupported preference type: %s", serializerSpec.getSerializedType());
            return null;
        }

        var key = FieldSpec.builder(String.class, "key$" + index, Modifier.PRIVATE, Modifier.FINAL).build();
        return new PreferenceSpec(context, preference, key, serializerSpec);
    }

    public PreferenceSpec(Context context, Preference preference, FieldSpec key, SerializerSpec serializerSpec) {
        this.name = StringUtils.getMethodName(preference.name());
        this.key = key;
        this.serializer = serializerSpec.getSerializer();
        this.serializedType = serializerSpec.getSerializedType();
        this.deserializedType = serializerSpec.getDeserializedType();

        var sharedPreferences = context.getSharedPreferences();
        var fluent = context.isFluent();
        var defaultValue = getDefaultValue(preference, serializedType);
        var description = preference.description();

        if (serializedType.getKind() == TypeKind.VOID) {
            getter = null;
            setter = null;
        } else {
            var getter = MethodSpec.methodBuilder(StringUtils.getGetterName(name, serializedType, fluent))
                    .addModifiers(Modifier.PUBLIC)
                    .returns(TypeName.get(deserializedType))
                    .addStatement(GETTER.get(serializedType.toString()), sharedPreferences, key, defaultValue)
                    .addStatement(serializer == null ? "return value" : "return $N.deserialize(value)", serializer);
            addJavadoc(getter, description, serializedType, defaultValue);
            this.getter = getter.build();

            var setter = MethodSpec.methodBuilder(StringUtils.getSetterName(name, fluent))
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

    private static boolean check(Context context, Preference preference) {
        if (!StringUtils.isJavaIdentifier(preference.name())) {
            context.error("Illegal preference name: %s", preference.name());
            return false;
        }

        return true;
    }

    private static boolean checkType(Context context, TypeMirror type) {
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
                return context.isSame(type, String.class) || context.isSame(type, Set.class, String.class);
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
