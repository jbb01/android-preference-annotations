package eu.jonahbauer.android.preference.annotations.processor;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;

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

    public static MethodSpec getter(String name, FieldSpec spec, boolean fluent) {
        return MethodSpec.methodBuilder(StringUtils.getGetterName(name, spec.type, fluent))
                .addModifiers(Modifier.PUBLIC)
                .returns(spec.type)
                .addStatement("return $N", spec)
                .build();
    }
}
