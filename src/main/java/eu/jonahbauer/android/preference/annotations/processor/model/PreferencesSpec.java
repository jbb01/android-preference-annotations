package eu.jonahbauer.android.preference.annotations.processor.model;

import com.squareup.javapoet.*;
import eu.jonahbauer.android.preference.annotations.Preferences;
import eu.jonahbauer.android.preference.annotations.processor.ClassNames;
import eu.jonahbauer.android.preference.annotations.processor.StringUtils;
import eu.jonahbauer.android.preference.annotations.processor.TypeUtils;
import lombok.Value;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;

@Value
public class PreferencesSpec {
    JavaFile file;

    public static PreferencesSpec create(ProcessingEnvironment env, Element element, Preferences root) {
        var context = new Context(env, element);

        if (!check(context, root)) return null;

        context.setFluent(root.fluent());
        context.setEditor(root.editor());

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
        var sharedPreferencesField = FieldSpec.builder(ClassNames.SHARED_PREFERENCES, "sharedPreferences", Modifier.PRIVATE, Modifier.STATIC).build();
        context.setSharedPreferences(sharedPreferencesField);
        builder.addField(sharedPreferencesField);

        // init method
        var initMethod = init(sharedPreferencesField);

        // group classes, fields, accessors and init statements
        var groups = root.value();
        var groupSpecs = new ArrayList<PreferenceGroupSpec>();
        for (int i = 0; i < groups.length; i++) {
            var spec = PreferenceGroupSpec.create(context, i, groups[i]);
            if (spec == null) continue;

            groupSpecs.add(spec);
            spec.apply(builder);

            initMethod.addStatement("$N = new $T(pResources)", spec.getField(), spec.getName());
        }

        builder.addMethod(initMethod.build());
        builder.addMethod(clear(sharedPreferencesField));
        builder.addMethod(getSharedPreferences(sharedPreferencesField));

        if (groupSpecs.stream().anyMatch(PreferenceGroupSpec::isExport)) {
            builder.addMethod(exportPreferences(sharedPreferencesField, groupSpecs));
            builder.addMethod(importPreferences(sharedPreferencesField, groupSpecs));
        }

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
                .addStatement("throw new $T($S)", ClassNames.ILLEGAL_STATE_EXCEPTION, "This class is not supposed to be instantiated.")
                .build();
    }

    private static MethodSpec.Builder init(FieldSpec sharedPreferencesField) {
        return MethodSpec.methodBuilder("init")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(ClassNames.SHARED_PREFERENCES, "pSharedPreferences")
                .addParameter(ClassNames.RESOURCES, "pResources")
                .addJavadoc("Initialize this preference class to use the given {@link $T}\n", ClassNames.SHARED_PREFERENCES)
                .addJavadoc("This function is supposed to be called from the applications {@code onCreate()} method.\n")
                .addJavadoc("@param pSharedPreferences the {@link $T} to be used. Not {@code null}.\n", ClassNames.SHARED_PREFERENCES)
                .addJavadoc("@param pResources the {@link $T} from which the preference keys should be loaded. Not {@code null}.\n", ClassNames.RESOURCES)
                .addJavadoc("@throws $T if this preference class has already been initialized.\n", ClassNames.ILLEGAL_STATE_EXCEPTION)
                .addCode(CodeBlock.builder()
                                 .beginControlFlow("if ($N != null)", sharedPreferencesField)
                                 .addStatement("throw new $T($S)", ClassNames.ILLEGAL_STATE_EXCEPTION, "Preferences have already been initialized.")
                                 .endControlFlow()
                                 .addStatement("$T.requireNonNull(pSharedPreferences, $S)", ClassNames.OBJECTS, "SharedPreferences must not be null.")
                                 .addStatement("$T.requireNonNull(pResources, $S)", ClassNames.OBJECTS, "Resources must not be null.")
                                 .addStatement("$N = pSharedPreferences", sharedPreferencesField)
                                 .build()
                );
    }

    private static MethodSpec clear(FieldSpec sharedPreferencesField) {
        var builder = MethodSpec.methodBuilder("clear")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        return addInitCheck(builder, sharedPreferencesField)
                .addStatement("$N.edit().clear().apply()", sharedPreferencesField)
                .addJavadoc("@see $T#clear()", ClassNames.SHARED_PREFERENCES_EDITOR)
                .build();
    }

    private static MethodSpec getSharedPreferences(FieldSpec sharedPreferencesField) {
        var builder = MethodSpec.methodBuilder("getSharedPreferences")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassNames.SHARED_PREFERENCES)
                .addJavadoc("Returns the underlying {@link $T} instance.\n", ClassNames.SHARED_PREFERENCES)
                .addJavadoc("Modifying the underlying {@code SharedPreferences} instance is not recommended as it can\n")
                .addJavadoc("lead to invalid data which can cause {@code PreferenceSerializer}s to throw exceptions.\n")
                .addJavadoc("@throws $T if this preference class has not yet been initialized.\n", ClassNames.ILLEGAL_STATE_EXCEPTION)
                .addJavadoc("@return the underlying {@code SharedPreferences} instance");
        return addInitCheck(builder, sharedPreferencesField)
                .addStatement("return $N", sharedPreferencesField)
                .build();
    }

    private static MethodSpec exportPreferences(FieldSpec sharedPreferencesField, List<PreferenceGroupSpec> groups) {
        var builder = MethodSpec.methodBuilder("exportPreferences")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassNames.JSON_OBJECT)
                .addException(ClassNames.JSON_EXCEPTION)
                .addJavadoc("Exports all preferences marked with {@code export = true} into a {@link $T}.\n", ClassNames.JSON_OBJECT)
                .addJavadoc("The preferences are keyed by their preference key. Preferences with a value of {@code null} are omitted.\n")
                .addJavadoc("@return a {@link $T} containing the exported preferences\n", ClassNames.JSON_OBJECT)
                .addJavadoc("@throws $T if this preference class has not yet been initialized.\n", ClassNames.ILLEGAL_STATE_EXCEPTION);
        addInitCheck(builder, sharedPreferencesField)
                .addStatement("var json = new $T()", ClassNames.JSON_OBJECT);
        for (var group : groups) {
            if (!group.isExport()) continue;
            builder.addStatement("$N.exportPreferences(json)", group.getField());
        }
        return builder.addStatement("return json").build();
    }

    private static MethodSpec importPreferences(FieldSpec sharedPreferencesField, List<PreferenceGroupSpec> groups) {
        var builder = MethodSpec.methodBuilder("importPreferences")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(ClassNames.JSON_OBJECT, "json")
                .addException(ClassNames.JSON_EXCEPTION)
                .addJavadoc("Imports all preferences marked with {@code export = true} from the given {@link $T}.\n", ClassNames.JSON_OBJECT)
                .addJavadoc("Preferences not present in the given {@link $T} (or with a value of {@code null}) are left unchanged.\n", ClassNames.JSON_OBJECT)
                .addJavadoc("The import is atomic, i.e. if any of the preferences cannot be imported none of them are changed.\n")
                .addJavadoc("@param json a {@link $T} as returned by {@code exportPreferences()}. Not {@code null}.\n", ClassNames.JSON_OBJECT)
                .addJavadoc("@throws $T if this preference class has not yet been initialized.\n", ClassNames.ILLEGAL_STATE_EXCEPTION);
        addInitCheck(builder, sharedPreferencesField)
                .addStatement("$T.requireNonNull(json, $S)", ClassNames.OBJECTS, "JSONObject must not be null.")
                .addStatement("var editor = $N.edit()", sharedPreferencesField);
        for (var group : groups) {
            if (!group.isExport()) continue;
            builder.addStatement("$N.importPreferences(json, editor)", group.getField());
        }
        return builder.addStatement("editor.apply()").build();
    }

    static MethodSpec.Builder addInitCheck(MethodSpec.Builder builder, FieldSpec sharedPreferencesField) {
        return builder.beginControlFlow("if ($N == null)", sharedPreferencesField)
                .addStatement("throw new $T($S)", ClassNames.ILLEGAL_STATE_EXCEPTION, "Preferences have not yet been initialized.")
                .endControlFlow();
    }
}
