package eu.jonahbauer.android.preference.annotations.processor.model;

import com.squareup.javapoet.*;
import eu.jonahbauer.android.preference.annotations.Preference;
import eu.jonahbauer.android.preference.annotations.processor.ClassNames;
import eu.jonahbauer.android.preference.annotations.processor.StringUtils;
import lombok.Value;

import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.HashSet;
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

    private static final Map<String, String> JSON_GETTER = Map.ofEntries(
            Map.entry("boolean", "json.getBoolean($N)"),
            Map.entry("byte", "(byte) json.getInt($N)"),
            Map.entry("char", "(char) json.getInt($N)"),
            Map.entry("short", "(short) json.getInt($N)"),
            Map.entry("int", "json.getInt($N)"),
            Map.entry("long", "json.getLong($N)"),
            Map.entry("float", "(float) json.getDouble($N)"),
            Map.entry("double", "json.getDouble($N)"),
            Map.entry("java.lang.String", "json.getString($N)"),
            Map.entry("org.json.JSONObject", "json.getJSONObject($N)"),
            Map.entry("org.json.JSONArray", "json.getJSONArray($N)")
    );

    String name;

    FieldSpec key;
    FieldSpec serializer;
    FieldSpec exportSerializer;

    MethodSpec getter;
    MethodSpec setter;

    CodeBlock exportCode;
    CodeBlock importCode;

    TypeMirror serializedType;
    TypeMirror deserializedType;

    public static PreferenceSpec create(Context context, int index, Preference preference) {
        if (!check(context, preference)) return null;

        var serializerSpec = SerializerSpec.create(context, index, preference);

        if (!checkType(context, serializerSpec.getSerializedType())) {
            context.error("Unsupported preference type: %s", serializerSpec.getSerializedType());
            return null;
        }

        ExportSerializerSpec exportSerializerSpec = null;
        if (preference.export()) {
            if (serializerSpec.getSerializedType().getKind() == TypeKind.VOID) {
                context.error("Cannot export void preference %s", preference.name());
                return null;
            }

            exportSerializerSpec = ExportSerializerSpec.create(context, index, preference, serializerSpec);
            if (exportSerializerSpec == null && serializerSpec.getSerializer() != null && !serializerSpec.isEnumDefault()) {
                context.error("Preference %s uses a custom serializer and therefore requires an exportSerializer to be exported", preference.name());
                return null;
            }
        }

        var key = FieldSpec.builder(String.class, "key$" + index, Modifier.PRIVATE, Modifier.FINAL).build();
        return new PreferenceSpec(context, index, preference, key, serializerSpec, exportSerializerSpec);
    }

    public PreferenceSpec(Context context, int index, Preference preference, FieldSpec key, SerializerSpec serializerSpec, ExportSerializerSpec exportSerializerSpec) {
        this.name = StringUtils.getMethodName(preference.name());
        this.key = key;
        this.serializer = serializerSpec.getSerializer();
        this.exportSerializer = exportSerializerSpec == null ? null : exportSerializerSpec.getExportSerializer();
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

        if (preference.export() && getter != null && setter != null) {
            this.exportCode = exportCode(index, exportSerializerSpec);
            this.importCode = importCode(exportSerializerSpec);
        } else {
            this.exportCode = null;
            this.importCode = null;
        }
    }

    public void apply(TypeSpec.Builder builder) {
        if (getter != null) builder.addMethod(getter);
        if (setter != null) builder.addMethod(setter);
        if (serializer != null) builder.addField(serializer);
        if (exportSerializer != null) builder.addField(exportSerializer);
        builder.addField(key);
    }

    /**
     * Builds the code exporting this preference into a {@code JSONObject} named {@code json}.
     */
    private CodeBlock exportCode(int index, ExportSerializerSpec exportSpec) {
        var builder = CodeBlock.builder();
        if (exportSpec != null) {
            switch (exportSpec.getJsonType().toString()) {
                case "boolean":
                    return builder.addStatement("json.put($N, $N.export($N()))", key, exportSerializer, getter).build();
                case "byte": case "short": case "int":
                    return builder.addStatement("json.put($N, $N.export($N()).intValue())", key, exportSerializer, getter).build();
                case "long":
                    return builder.addStatement("json.put($N, $N.export($N()).longValue())", key, exportSerializer, getter).build();
                case "float": case "double":
                    return builder.addStatement("json.put($N, $N.export($N()).doubleValue())", key, exportSerializer, getter).build();
                default: // String, JSONObject, JSONArray
                    return builder
                            .addStatement("var export$L = $N.export($N())", index, exportSerializer, getter)
                            .beginControlFlow("if (export$L != null)", index)
                            .addStatement("json.put($N, export$L)", key, index)
                            .endControlFlow()
                            .build();
            }
        } else {
            switch (serializedType.toString()) {
                case "boolean": case "int": case "long":
                    return builder.addStatement("json.put($N, $N())", key, getter).build();
                case "byte": case "char": case "short":
                    return builder.addStatement("json.put($N, (int) $N())", key, getter).build();
                case "float": case "double":
                    return builder.addStatement("json.put($N, (double) $N())", key, getter).build();
                case "java.lang.String":
                    return builder
                            .addStatement("var export$L = $N()", index, getter)
                            .beginControlFlow("if (export$L != null)", index)
                            .addStatement("json.put($N, export$L)", key, index)
                            .endControlFlow()
                            .build();
                case "java.util.Set<java.lang.String>":
                    return builder
                            .addStatement("var export$L = $N()", index, getter)
                            .beginControlFlow("if (export$L != null)", index)
                            .addStatement("json.put($N, new $T(export$L))", key, ClassNames.JSON_ARRAY, index)
                            .endControlFlow()
                            .build();
                default:
                    throw new RuntimeException("Unsupported export type: " + serializedType);
            }
        }
    }

    /**
     * Builds the code importing this preference from a {@code JSONObject} named {@code json} into a
     * {@code SharedPreferences.Editor} named {@code editor}.
     */
    private CodeBlock importCode(ExportSerializerSpec exportSpec) {
        var builder = CodeBlock.builder().beginControlFlow("if (!json.isNull($N))", key);
        if (exportSpec != null) {
            builder.addStatement("var value = $N.restore(" + JSON_GETTER.get(exportSpec.getJsonType().toString()) + ")", exportSerializer, key);
        } else if ("java.util.Set<java.lang.String>".equals(serializedType.toString())) {
            builder.addStatement("var array = json.getJSONArray($N)", key)
                    .addStatement("var value = new $T<$T>()", HashSet.class, String.class)
                    .beginControlFlow("for (int i = 0; i < array.length(); i++)")
                    .addStatement("value.add(array.getString(i))")
                    .endControlFlow();
        } else {
            builder.addStatement("var value = " + JSON_GETTER.get(serializedType.toString()), key);
        }
        builder.addStatement(serializer == null ? "var serializedValue = value" : "var serializedValue = $N.serialize(value)", serializer)
                .addStatement(PreferenceEditorSpec.SETTER.get(serializedType.toString()), "editor", key);
        return builder.endControlFlow().build();
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
