package eu.jonahbauer.android.preference.annotations.processor.model;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import eu.jonahbauer.android.preference.annotations.processor.TypeUtils;
import lombok.Value;

import javax.lang.model.element.Modifier;
import java.util.List;

@Value
public class PreferenceKeysSpec {
    FieldSpec field;
    MethodSpec accessor;
    TypeSpec type;

    public static PreferenceKeysSpec create(ClassName parent, List<PreferenceSpec> preferences) {
        var name = parent.nestedClass("Keys");

        var type = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());

        for (PreferenceSpec preference : preferences) {
            type.addMethod(TypeUtils.getter(preference.getName(), preference.getKey()));
        }

        var field = FieldSpec.builder(name, "keys", Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T()", name)
                .build();

        var accessor = TypeUtils.getter("keys", field);

        return new PreferenceKeysSpec(field, accessor, type.build());
    }

    public void apply(TypeSpec.Builder builder) {
        builder.addField(field).addMethod(accessor).addType(type);
    }
}
