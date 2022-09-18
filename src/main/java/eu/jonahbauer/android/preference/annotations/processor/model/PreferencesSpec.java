package eu.jonahbauer.android.preference.annotations.processor.model;

import com.squareup.javapoet.*;
import eu.jonahbauer.android.preference.annotations.Preferences;
import eu.jonahbauer.android.preference.annotations.processor.StringUtils;
import eu.jonahbauer.android.preference.annotations.processor.TypeUtils;
import lombok.Value;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;

import static eu.jonahbauer.android.preference.annotations.processor.PreferenceProcessor.*;

@Value
public class PreferencesSpec {
    JavaFile file;

    public static PreferencesSpec create(ProcessingEnvironment env, Element element, Preferences root) {
        var context = new Context(env, element);

        if (!check(context, root)) return null;

        var name = name(root);
        context.setRoot(name);

        var r = TypeName.get(TypeUtils.mirror(root, Preferences::r));
        context.setR(r);

        var builder = TypeSpec.classBuilder(name);

        if (root.makeFile()) {
            builder.addModifiers(Modifier.PUBLIC, Modifier.FINAL);
        }

        builder.addMethod(constructor(root));

        // shared preferences
        var sharedPreferencesField = FieldSpec.builder(SHARED_PREFERENCES, "sharedPreferences", Modifier.PRIVATE, Modifier.STATIC).build();
        context.setSharedPreferences(sharedPreferencesField);
        builder.addField(sharedPreferencesField);

        // init method
        var initMethod = init(sharedPreferencesField);

        // group classes, fields, accessors and init statements
        var groups = root.value();
        for (int i = 0; i < groups.length; i++) {
            var spec = PreferenceGroupSpec.create(context, i, groups[i]);
            if (spec == null) continue;

            spec.apply(builder);

            initMethod.addStatement("$N = new $T(pResources)", spec.getField(), spec.getName());
        }

        builder.addMethod(initMethod.build());

        return new PreferencesSpec(JavaFile.builder(name.packageName(), builder.build()).indent("    ").build());
    }

    private static boolean check(Context context, Preferences root) {
        if (!StringUtils.isFQCN(root.name())) {
            context.error("Illegal preference class name: %s", root.name());
            return false;
        }
        return true;
    }

    private static ClassName name(Preferences root) {
        return ClassName.get(
                root.name().lastIndexOf('.') != -1 ? root.name().substring(0, root.name().lastIndexOf('.')) : "",
                root.name().lastIndexOf('.') != -1 ? root.name().substring(root.name().lastIndexOf('.') + 1) : root.name()
        );
    }

    private static MethodSpec constructor(Preferences root) {
        return MethodSpec.constructorBuilder()
                .addModifiers(root.makeFile() ? Modifier.PRIVATE : Modifier.PROTECTED)
                .addStatement("throw new $T($S)", ILLEGAL_STATE_EXCEPTION, "This class is not supposed to be instantiated.")
                .build();
    }

    private static MethodSpec.Builder init(FieldSpec sharedPreferencesField) {
        return MethodSpec.methodBuilder("init")
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
    }
}
