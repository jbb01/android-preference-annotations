package eu.jonahbauer.android.preference.annotations.processor.model;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import eu.jonahbauer.android.preference.annotations.processor.ClassNames;
import lombok.Value;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.stream.Collectors;

@Value
public class PreferenceExportSpec {
    MethodSpec exportMethod;
    MethodSpec importMethod;
    MethodSpec importIntoEditorMethod;

    /**
     * Creates the {@code exportPreferences(JSONObject)} and {@code importPreferences(JSONObject)} methods of a
     * preference group from the {@linkplain eu.jonahbauer.android.preference.annotations.Preference#export() exported}
     * preferences of the group, as well as a private {@code importPreferences(JSONObject, SharedPreferences.Editor)}
     * method that allows importing multiple groups with a single editor.
     * @return the spec, or {@code null} if the group does not contain any exported preferences
     */
    public static PreferenceExportSpec create(Context context, List<PreferenceSpec> preferences) {
        var exported = preferences.stream().filter(p -> p.getExportCode() != null).collect(Collectors.toList());
        if (exported.isEmpty()) return null;

        var exportMethod = MethodSpec.methodBuilder("exportPreferences")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassNames.JSON_OBJECT, "json")
                .addException(ClassNames.JSON_EXCEPTION)
                .addJavadoc("Exports all exportable preferences of this group into the given {@link $T}.\n", ClassNames.JSON_OBJECT)
                .addJavadoc("Preferences with a value of {@code null} are omitted.\n")
                .addJavadoc("@param json the {@link $T} the preferences are exported into. Not {@code null}.\n", ClassNames.JSON_OBJECT);

        var importIntoEditorMethod = MethodSpec.methodBuilder("importPreferences")
                .addModifiers(Modifier.PRIVATE)
                .addParameter(ClassNames.JSON_OBJECT, "json")
                .addParameter(ClassNames.SHARED_PREFERENCES_EDITOR, "editor")
                .addException(ClassNames.JSON_EXCEPTION);

        for (var preference : exported) {
            exportMethod.addCode(preference.getExportCode());
            importIntoEditorMethod.addCode(preference.getImportCode());
        }

        var importMethod = MethodSpec.methodBuilder("importPreferences")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassNames.JSON_OBJECT, "json")
                .addException(ClassNames.JSON_EXCEPTION)
                .addJavadoc("Imports all exportable preferences of this group from the given {@link $T}.\n", ClassNames.JSON_OBJECT)
                .addJavadoc("Preferences not present in the given {@link $T} (or with a value of {@code null}) are left unchanged.\n", ClassNames.JSON_OBJECT)
                .addJavadoc("The import is atomic, i.e. if any of the preferences cannot be imported none of them are changed.\n")
                .addJavadoc("@param json the {@link $T} the preferences are imported from. Not {@code null}.\n", ClassNames.JSON_OBJECT)
                .addStatement("var editor = $N.edit()", context.getSharedPreferences())
                .addStatement("importPreferences(json, editor)")
                .addStatement("editor.apply()");

        return new PreferenceExportSpec(exportMethod.build(), importMethod.build(), importIntoEditorMethod.build());
    }

    public void apply(TypeSpec.Builder builder) {
        builder.addMethod(exportMethod);
        builder.addMethod(importMethod);
        builder.addMethod(importIntoEditorMethod);
    }
}
