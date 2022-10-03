package eu.jonahbauer.android.preference.annotations.processor;

import com.squareup.javapoet.ClassName;
import lombok.experimental.UtilityClass;

import java.util.Objects;

@UtilityClass
public class ClassNames {
    public static final ClassName SHARED_PREFERENCES = ClassName.get("android.content", "SharedPreferences");
    public static final ClassName SHARED_PREFERENCES_EDITOR = ClassName.get("android.content", "SharedPreferences", "Editor");
    public static final ClassName RESOURCES = ClassName.get("android.content.res", "Resources");
    public static final ClassName ILLEGAL_STATE_EXCEPTION = ClassName.get(IllegalStateException.class);
    public static final ClassName OBJECTS = ClassName.get(Objects.class);
}
