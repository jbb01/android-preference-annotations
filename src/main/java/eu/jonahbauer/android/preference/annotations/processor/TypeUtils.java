package eu.jonahbauer.android.preference.annotations.processor;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import java.util.function.Function;

public final class TypeUtils {

    public static <S> TypeMirror mirror(S object, Function<S, ? extends Class<?>> function) {
        try {
            function.apply(object);
            throw new RuntimeException();
        } catch (MirroredTypeException e) {
            return e.getTypeMirror();
        }
    }

    public static TypeMirror tryUnbox(ProcessingEnvironment processingEnv, TypeMirror type) {
        try {
            return processingEnv.getTypeUtils().unboxedType(type);
        } catch (IllegalArgumentException e) {
            return type;
        }
    }

    public static boolean isSame(Class<?> clazz, TypeMirror type) {
        return clazz.getName().equals(type.toString());
    }

    public static boolean isEnum(ProcessingEnvironment processingEnv, TypeMirror type) {
        var utils = processingEnv.getTypeUtils();
        for (TypeMirror supertype : utils.directSupertypes(type)) {
            if (isSame(Enum.class, utils.erasure(supertype))) return true;
        }
        return false;
    }

    public static MethodSpec getter(String name, FieldSpec spec) {
        return MethodSpec.methodBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .returns(spec.type)
                .addStatement("return $N", spec)
                .build();
    }
}