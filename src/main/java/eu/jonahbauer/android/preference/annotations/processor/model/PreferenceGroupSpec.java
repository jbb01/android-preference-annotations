package eu.jonahbauer.android.preference.annotations.processor.model;

import com.squareup.javapoet.*;
import eu.jonahbauer.android.preference.annotations.PreferenceGroup;
import eu.jonahbauer.android.preference.annotations.processor.StringUtils;
import lombok.Value;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;

import static eu.jonahbauer.android.preference.annotations.processor.PreferenceProcessor.*;

@Value
public class PreferenceGroupSpec {
    ClassName name;
    FieldSpec field;
    MethodSpec accessor;
    TypeSpec type;

    public static PreferenceGroupSpec create(Context context, int index, PreferenceGroup group) {
        if (!check(context, group)) return null;

        var name = context.getRoot().nestedClass(group.name());
        var type = TypeSpec.classBuilder(name).addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
        var constructorCode = CodeBlock.builder();

        var preferences = group.value();
        var preferenceSpecs = new ArrayList<PreferenceSpec>();
        for (int i = 0; i < preferences.length; i++) {
            var spec = PreferenceSpec.create(context, i, preferences[i]);
            if (spec == null) continue;

            preferenceSpecs.add(spec);
            spec.apply(type);

            var key = group.prefix() + preferences[i].name() + group.suffix();
            constructorCode.addStatement("$N = resources.getString($T.string.$N)", spec.getKey(), context.getR(), key);
        }

        // constructor
        type.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(RESOURCES, "resources")
                .addCode(constructorCode.build())
                .build()
        );

        PreferenceKeysSpec.create(context, name, preferenceSpecs).apply(type);

        if (context.isEditor()) {
            PreferenceEditorSpec.create(context, name, preferenceSpecs).apply(type);
        }

        var field = field(index, name);
        var accessor = accessor(context, group.name(), field, context.getSharedPreferences());
        return new PreferenceGroupSpec(name, field, accessor, type.build());
    }

    public void apply(TypeSpec.Builder builder) {
        builder.addField(field).addMethod(accessor).addType(type);
    }

    private static boolean check(Context context, PreferenceGroup group) {
        if (!StringUtils.isJavaIdentifier(group.name())) {
            context.error("Illegal preference group name: %s", group.name());
            return false;
        } else if (!group.prefix().isEmpty() && !StringUtils.isJavaIdentifier(group.prefix())) {
            context.error("Illegal preference group prefix: %s", group.prefix());
            return false;
        } else if (!group.suffix().isEmpty() && !group.suffix().matches("\\p{javaJavaIdentifierPart}+")) {
            context.error("Illegal preference group suffix: %s", group.suffix());
            return false;
        }

        return true;
    }

    private static FieldSpec field(int index, ClassName name) {
        return FieldSpec.builder(name, "group$" + index, Modifier.PRIVATE, Modifier.STATIC).build();
    }

    private static MethodSpec accessor(Context context, String name, FieldSpec field, FieldSpec sharedPreferences) {
        return MethodSpec.methodBuilder(StringUtils.getGetterName(name, field.type, context.isFluent()))
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(field.type)
                .addCode(CodeBlock.builder()
                                 .beginControlFlow("if ($N == null)", sharedPreferences)
                                 .addStatement("throw new $T($S)", ILLEGAL_STATE_EXCEPTION, "Preferences have not yet been initialized.")
                                 .endControlFlow()
                                 .addStatement("return $N", field)
                                 .build()
                )
                .build();
    }
}
