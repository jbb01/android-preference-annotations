package eu.jonahbauer.android.preference.annotations.processor.model;

import com.squareup.javapoet.*;
import eu.jonahbauer.android.preference.annotations.processor.ClassNames;
import eu.jonahbauer.android.preference.annotations.processor.StringUtils;
import lombok.Value;

import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import java.util.List;
import java.util.Map;

@Value
public class PreferenceEditorSpec {
    private static final String EDITOR_CLASS_NAME = "Editor";
    private static final String EDITOR_FIELD_NAME = "editor";

    private static final Map<String, String> SETTER = Map.of(
            "boolean", "$N.putBoolean($N, serializedValue)",
            "byte", "$N.putInt($N, (int) serializedValue)",
            "char", "$N.putInt($N, (int) serializedValue)",
            "short", "$N.putInt($N, (int) serializedValue)",
            "int", "$N.putInt($N, (int) serializedValue)",
            "long", "$N.putLong($N, serializedValue)",
            "float", "$N.putFloat($N, serializedValue)",
            "double", "$N.putLong($N, Double.doubleToRawLongBits(serializedValue))",
            "java.lang.String", "$N.putString($N, serializedValue)",
            "java.util.Set<java.lang.String>", "$N.putStringSet($N, serializedValue)"
    );

    MethodSpec accessor;
    TypeSpec type;

    public static PreferenceEditorSpec create(Context context, ClassName parent, List<PreferenceSpec> preferences) {
        var name = parent.nestedClass(EDITOR_CLASS_NAME);

        var editor = FieldSpec
                .builder(ClassNames.SHARED_PREFERENCES_EDITOR, EDITOR_FIELD_NAME, Modifier.PRIVATE, Modifier.FINAL)
                .initializer("sharedPreferences.edit()")
                .build();

        var constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .build();

        var type = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(constructor)
                .addField(editor);

        for (PreferenceSpec preference : preferences) {
            var setter = setter(context, name, preference, editor);
            if (setter != null) type.addMethod(setter);
        }

        type.addMethod(apply(editor));
        type.addMethod(commit(editor));

        return new PreferenceEditorSpec(accessor(name), type.build());
    }

    public void apply(TypeSpec.Builder builder) {
        builder.addMethod(accessor);
        builder.addType(type);
    }

    private static MethodSpec setter(Context context, ClassName name, PreferenceSpec preference, FieldSpec editor) {
        var serializedType = preference.getSerializedType();
        if (serializedType.getKind() == TypeKind.VOID) return null;

        var deserializedType = preference.getDeserializedType();
        var serializer = preference.getSerializer();
        var key = preference.getKey();
        var methodName = StringUtils.getSetterName(preference.getName(), context.isFluent());

        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(name)
                .addParameter(TypeName.get(deserializedType), "value")
                .addStatement(serializer == null ? "var serializedValue = value" : "var serializedValue = $N.serialize(value)", serializer)
                .addStatement(SETTER.get(serializedType.toString()), editor, key)
                .addStatement("return this")
                .build();
    }

    private static MethodSpec apply(FieldSpec editor) {
        return MethodSpec.methodBuilder("apply").addModifiers(Modifier.PUBLIC)
                .addStatement("$N.apply()", editor)
                .addJavadoc("Commit your preferences changes back from this Editor to the {@code SharedPreferences} object it is editing. This atomically performs the requested modifications, replacing whatever is currently in the SharedPreferences. ")
                .addJavadoc("\n@see $T#apply()", ClassNames.SHARED_PREFERENCES_EDITOR)
                .build();
    }

    private static MethodSpec commit(FieldSpec editor) {
        return MethodSpec.methodBuilder("commit")
                .addModifiers(Modifier.PUBLIC).returns(boolean.class)
                .addStatement("return $N.commit()", editor)
                .addJavadoc("Commit your preferences changes back from this Editor to the {@code SharedPreferences} object it is editing. This atomically performs the requested modifications, replacing whatever is currently in the SharedPreferences.")
                .addJavadoc("\n@see $T#commit()", ClassNames.SHARED_PREFERENCES_EDITOR)
                .build();
    }

    private static MethodSpec accessor(ClassName name) {
        return MethodSpec.methodBuilder("edit").addModifiers(Modifier.PUBLIC)
                .returns(name)
                .addStatement("return new $T()", name)
                .addJavadoc("Create a new Editor for these preferences, through which you can make modifications to the data in the preferences and atomically commit those changes back to the SharedPreferences object.")
                .addJavadoc("\n@see $T#edit()", ClassNames.SHARED_PREFERENCES)
                .build();
    }
}
