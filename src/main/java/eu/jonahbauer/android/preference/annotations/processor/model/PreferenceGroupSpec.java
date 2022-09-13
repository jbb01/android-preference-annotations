package eu.jonahbauer.android.preference.annotations.processor.model;

import com.squareup.javapoet.*;
import eu.jonahbauer.android.preference.annotations.PreferenceGroup;
import eu.jonahbauer.android.preference.annotations.processor.StringUtils;
import lombok.Value;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import java.util.ArrayList;

import static eu.jonahbauer.android.preference.annotations.processor.PreferenceProcessor.*;

@Value
public class PreferenceGroupSpec {
    ClassName name;
    FieldSpec field;
    MethodSpec accessor;
    TypeSpec type;

    public static PreferenceGroupSpec create(ProcessingEnvironment env, Element element, TypeName r, ClassName parent, int index, PreferenceGroup group, FieldSpec sharedPreferences) {
        if (!check(env, element, group)) return null;

        var name = parent.nestedClass(group.name());
        var type = TypeSpec.classBuilder(name).addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
        var constructorCode = CodeBlock.builder();

        var preferences = group.value();
        var preferenceSpecs = new ArrayList<PreferenceSpec>();
        for (int i = 0; i < preferences.length; i++) {
            var spec = PreferenceSpec.create(env, element, sharedPreferences, i, preferences[i]);
            if (spec == null) continue;

            preferenceSpecs.add(spec);
            spec.apply(type);

            constructorCode.addStatement("$N = resources.getString($T.string.$N)", spec.getKey(), r, group.prefix() + preferences[i].name() + group.suffix());
        }

        // constructor
        type.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(RESOURCES, "resources")
                .addCode(constructorCode.build())
                .build()
        );

        PreferenceKeysSpec.create(name, preferenceSpecs).apply(type);

        var field = field(index, name);
        var accessor = accessor(group.name(), field, sharedPreferences);
        return new PreferenceGroupSpec(name, field, accessor, type.build());
    }

    public void apply(TypeSpec.Builder builder) {
        builder.addField(field).addMethod(accessor).addType(type);
    }

    private static boolean check(ProcessingEnvironment env, Element element, PreferenceGroup group) {
        if (!StringUtils.isJavaIdentifier(group.name())) {
            error(env, element, "Illegal preference group name: %s", group.name());
            return false;
        } else if (!group.prefix().isEmpty() && !StringUtils.isJavaIdentifier(group.prefix())) {
            error(env, element, "Illegal preference group prefix: %s", group.prefix());
            return false;
        } else if (!group.suffix().isEmpty() && !group.suffix().matches("\\p{javaJavaIdentifierPart}+")) {
            error(env, element, "Illegal preference group suffix: %s", group.suffix());
            return false;
        }

        return true;
    }

    private static FieldSpec field(int index, ClassName name) {
        return FieldSpec.builder(name, "group$" + index, Modifier.PRIVATE, Modifier.STATIC).build();
    }

    private static MethodSpec accessor(String name, FieldSpec field, FieldSpec sharedPreferences) {
        return MethodSpec.methodBuilder(name)
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
