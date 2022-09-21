package eu.jonahbauer.android.preference.annotations.processor.model;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import eu.jonahbauer.android.preference.annotations.Preference;
import eu.jonahbauer.android.preference.annotations.processor.TypeUtils;
import eu.jonahbauer.android.preference.annotations.serializer.EnumSerializer;
import eu.jonahbauer.android.preference.annotations.serializer.Serializer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SerializerSpec {
    TypeMirror deserializedType;
    TypeMirror serializedType;
    FieldSpec serializer;

    public static SerializerSpec create(Context context, int index, Preference preference) {
        var declaredType = TypeUtils.mirror(preference, Preference::type);

        // no serializer
        var serializerRawType = TypeUtils.mirror(preference, Preference::serializer);
        if (TypeUtils.isSame(Serializer.class, serializerRawType)) {
            if (TypeUtils.isEnum(context, declaredType)) {
                // use enum serializer for enum types when no serializer is specified
                serializerRawType = TypeUtils.getType(context, EnumSerializer.class);
            } else {
                return new SerializerSpec(declaredType);
            }
        }

        if (serializerRawType == null) {
            // strang things did happen here
            context.error("No serializer for preference %s", preference.name());
            return new SerializerSpec(declaredType);
        } else if (!(serializerRawType instanceof DeclaredType)) {
            // strang things did happen here
            context.error("Invalid serializer type %s", serializerRawType);
            return new SerializerSpec(declaredType);
        }

        // add type arguments to serializer when necessary
        var serializerType = withTypeArguments(context, (DeclaredType) serializerRawType, declaredType);
        var serializerTypeName = TypeName.get(serializerType);
        // find serializer interface in type hierarchy
        var serializerInt = findSerializerType(context, serializerType);

        TypeMirror serializedType, deserializedType;

        if (check(context, preference, serializerInt)) {
            assert serializerInt != null;
            serializedType = TypeUtils.tryUnbox(context, serializerInt.getTypeArguments().get(1));
            deserializedType = TypeUtils.tryUnbox(context, serializerInt.getTypeArguments().get(0));
        } else {
            return new SerializerSpec(declaredType);
        }

        // find constructor
        var constructor = hasClassConstructor(context, serializerType);
        if (constructor == null) {
            context.error("Could not find a suitable constructor in serializer class " + serializerType + ".");
            return new SerializerSpec(declaredType);
        } else {
            // build field spec
            var builder = FieldSpec.builder(serializerTypeName, "serializer$" + index, Modifier.PRIVATE, Modifier.FINAL);
            if (constructor) {
                builder.initializer("new $T($T.class)", serializerTypeName, TypeUtils.tryBox(context, declaredType));
            } else {
                builder.initializer("new $T()", serializerTypeName);
            }
            return new SerializerSpec(deserializedType, serializedType, builder.build());
        }
    }

    private SerializerSpec(TypeMirror type) {
        this(type, type, null);
    }

    /**
     * When the serializer requires exactly one type parameter it is filled with the deserialized type.
     * @param context the processing context
     * @param serializer the raw declared serializer type
     * @return the declared serializer type, optionally with type arguments
     */
    private static DeclaredType withTypeArguments(Context context, DeclaredType serializer, TypeMirror declaredType) {
        // converting to element and back ensures that type arguments are available
        serializer = (DeclaredType) serializer.asElement().asType();
        var typeArguments = serializer.getTypeArguments();
        if (typeArguments.size() == 1) {
            // since the input was a Class<?> the type argument cannot be specified already
            // and must be a type variable which we can simply override
            var boxedType = TypeUtils.tryBox(context, declaredType);
            return context.getTypeUtils().getDeclaredType((TypeElement) serializer.asElement(), boxedType);
        } else {
            return serializer;
        }
    }

    /**
     * Traverses the type hierarchy of the given {@code type} to find the generic type of the implemented
     * {@link Serializer} interface.
     * @param context the processing context
     * @param type the type of the actual {@link Serializer} implementation
     * @return the type (with generics) of the {@link Serializer} interface or {@code null} if not found
     */
    private static DeclaredType findSerializerType(Context context, TypeMirror type) {
        var typeUtils = context.getTypeUtils();
        if (Serializer.class.getName().equals(typeUtils.erasure(type).toString())) {
            return (DeclaredType) type;
        }

        for (TypeMirror supertype : typeUtils.directSupertypes(type)) {
            if (Object.class.toString().equals(supertype.toString())) continue;

            var serializerType = findSerializerType(context, supertype);
            if (serializerType != null) {
                return serializerType;
            }
        }

        return null;
    }

    /**
     * Checks that the serializer is compatible with the given type.
     * @param context the processing context
     * @param preference the preference
     * @param serializer the {@linkplain #findSerializerType(Context, TypeMirror) resolved} serializer interface
     * @return {@code true} iff the serializer is well-defined
     */
    private static boolean check(Context context, Preference preference, DeclaredType serializer) {
        if (serializer == null) {
            // this should not be able to happen since the serializer type must (by generics) always implement
            // the Serializer interface
            context.error("No serializer for preference %s", preference.name());
            return false;
        } else if (serializer.getTypeArguments().size() != 2) {
            context.error("Unable to identify type arguments of serializer %s for preference %s", serializer, preference.name());
            return false;
        } else {
            return true;
        }
    }

    /**
     * Checks the serializers constructors.
     * @param context the processing context
     * @param serializer the declared serializer type with type arguments
     * @return {@code true} if the type has a constructor that takes one argument of type {@link Class}, {@code false}
     * if the type has a constructor that takes no arguments, {@code null} if no suitable constructor could be found
     */
    private static Boolean hasClassConstructor(Context context, DeclaredType serializer) {
        for (var element : serializer.asElement().getEnclosedElements()) {
            if (element.getKind() != ElementKind.CONSTRUCTOR) continue;

            var constructor = (ExecutableType) element.asType();
            var parameters = constructor.getParameterTypes();

            if (parameters.size() == 0) {
                return false;
            } else if (parameters.size() == 1) {
                var param = parameters.get(0);
                if (TypeUtils.isSameErasure(context, Class.class, param)) {
                    return true;
                }
            }
        }

        context.error("No suitable constructor found for serializer %s", serializer);
        return null;
    }
}
