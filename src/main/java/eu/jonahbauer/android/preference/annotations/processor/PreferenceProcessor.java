package eu.jonahbauer.android.preference.annotations.processor;

import com.squareup.javapoet.ClassName;
import eu.jonahbauer.android.preference.annotations.Preferences;
import eu.jonahbauer.android.preference.annotations.processor.model.PreferencesSpec;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.Set;

@SupportedAnnotationTypes({
        "eu.jonahbauer.android.preference.annotations.Preferences"
})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public final class PreferenceProcessor extends AbstractProcessor {
    public static final ClassName SHARED_PREFERENCES = ClassName.get("android.content", "SharedPreferences");
    public static final ClassName SHARED_PREFERENCES_EDITOR = ClassName.get("android.content", "SharedPreferences", "Editor");
    public static final ClassName RESOURCES = ClassName.get("android.content.res", "Resources");
    public static final ClassName ILLEGAL_STATE_EXCEPTION = ClassName.get("java.lang", "IllegalStateException");
    public static final ClassName OBJECTS = ClassName.get("java.util", "Objects");

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var clazzes = roundEnv.getElementsAnnotatedWith(Preferences.class);

        try {
            for (Element clazz : clazzes) {
                var root = clazz.getAnnotation(Preferences.class);

                var preferencesSpec = PreferencesSpec.create(processingEnv, clazz, root);
                if (preferencesSpec != null) {
                    preferencesSpec.getFile().writeTo(processingEnv.getFiler());
                }
            }
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}