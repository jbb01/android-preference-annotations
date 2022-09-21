package eu.jonahbauer.android.preference.annotations.processor.model;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import lombok.Data;
import lombok.experimental.Delegate;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.Arrays;

@Data
public final class Context {
    @Delegate
    private final ProcessingEnvironment env;
    private final Element element;

    private TypeName r;
    private ClassName root;
    private FieldSpec sharedPreferences;
    private boolean fluent;
    private boolean editor;

    /**
     * Checks whether the given type and class are the same after type erasure.
     */
    public boolean isSame(TypeMirror type, Class<?> clazz, Class<?>...typeParameters) {
        var types = env.getTypeUtils();

        if (typeParameters == null || typeParameters.length == 0) {
            return types.isSameType(
                    types.erasure(type),
                    types.erasure(getType(clazz))
            );
        } else {
            return types.isSameType(
                    type,
                    getType(clazz, typeParameters)
            );
        }
    }

    /**
     * Checks whether the given type is an enum type.
     */
    public boolean isEnum(TypeMirror type) {
        var utils = env.getTypeUtils();
        for (var supertype : utils.directSupertypes(type)) {
            if (isSame(supertype, Enum.class)) return true;
        }
        return false;
    }

    /**
     * Unboxes the given type when possible and returns it unchanged if it is not a primitive wrapper.
     */
    public TypeMirror tryUnbox(TypeMirror type) {
        try {
            return env.getTypeUtils().unboxedType(type);
        } catch (IllegalArgumentException e) {
            return type;
        }
    }

    /**
     * Boxes the given type when it is a primitive and returns it unchanged if not.
     */
    public TypeMirror tryBox(TypeMirror type) {
        if (type instanceof PrimitiveType) {
            return env.getTypeUtils().boxedClass((PrimitiveType) type).asType();
        } else {
            return type;
        }
    }

    public DeclaredType getType(Class<?> clazz, Class<?>...typeParameters) {
        var types = env.getTypeUtils();
        var elements = env.getElementUtils();

        var element = elements.getTypeElement(clazz.getName());
        if (typeParameters == null || typeParameters.length == 0) {
            return types.getDeclaredType(element);
        } else {
            return types.getDeclaredType(
                    element,
                    Arrays.stream(typeParameters)
                            .map(Class::getName)
                            .map(elements::getTypeElement)
                            .map(TypeElement::asType)
                            .toArray(TypeMirror[]::new)
            );
        }
    }

    public void error(String message, Object...args) {
        env.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format(message, args), element);
    }
}
