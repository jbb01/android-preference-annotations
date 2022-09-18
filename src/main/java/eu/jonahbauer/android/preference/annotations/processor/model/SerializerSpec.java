package eu.jonahbauer.android.preference.annotations.processor.model;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import eu.jonahbauer.android.preference.annotations.Preference;
import eu.jonahbauer.android.preference.annotations.processor.TypeUtils;
import eu.jonahbauer.android.preference.annotations.serializer.EnumSerializer;
import eu.jonahbauer.android.preference.annotations.serializer.Serializer;
import lombok.Value;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;

@Value
public class SerializerSpec {
    TypeMirror deserializedType;
    TypeMirror serializedType;
    FieldSpec serializer;

    public SerializerSpec(Context context, int index, Preference preference) {
        deserializedType = TypeUtils.mirror(preference, Preference::type);

        var serializerRawType = TypeUtils.mirror(preference, Preference::serializer);
        if (TypeUtils.isSame(Serializer.class, serializerRawType)) {
            if (TypeUtils.isEnum(context, deserializedType)) {
                serializerRawType = TypeUtils.getType(context, EnumSerializer.class);
            } else {
                serializedType = deserializedType;
                serializer = null;
                return;
            }
        }

        // handle serializers
        if (serializerRawType == null) {
            context.error("No serializer for preference %s", preference.name());
            serializer = null;
            serializedType = deserializedType;
            return;
        } else if (!(serializerRawType instanceof DeclaredType)) {
            context.error("Invalid serializer type %s", serializerRawType);
            serializer = null;
            serializedType = deserializedType;
            return;
        }

        var serializerType = withTypeArguments(context, (DeclaredType) serializerRawType);
        var serializerTypeName = TypeName.get(serializerType);
        var serializerInt = findSerializerType(context, serializerType);

        if (check(context, preference, serializerInt, deserializedType)) {
            assert serializerInt != null;
            serializedType = TypeUtils.tryUnbox(context, serializerInt.getTypeArguments().get(1));
        } else {
            serializer = null;
            serializedType = deserializedType;
            return;
        }

        var constructor = hasClassConstructor(context, serializerType);
        if (constructor == null) {
            serializer = null;
        } else {
            var builder = FieldSpec.builder(serializerTypeName, "serializer$" + index, Modifier.PRIVATE, Modifier.FINAL);
            if (constructor) {
                builder.initializer("new $T($T.class)", serializerTypeName, TypeUtils.tryBox(context, deserializedType));
            } else {
                builder.initializer("new $T()", serializerTypeName);
            }
            serializer = builder.build();
        }
    }

    /**
     * When the serializer requires exactly one type parameter it is filled with the deserialized type.
     * @param context the processing context
     * @param serializer the raw declared serializer type
     * @return the declared serializer type, optionally with type arguments
     */
    private DeclaredType withTypeArguments(Context context, DeclaredType serializer) {
        // converting to element and back ensures that type arguments are available
        serializer = (DeclaredType) serializer.asElement().asType();
        var typeArguments = serializer.getTypeArguments();
        if (typeArguments.size() == 1) {
            // since the input was a Class<?> the type argument cannot be specified already
            // and must be a type variable which we can simply override
            var boxedType = TypeUtils.tryBox(context, deserializedType);
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
     * @param deserializedType the deserialized preference type
     * @return {@code true} iff the serializer is well-defined
     */
    private static boolean check(Context context, Preference preference, DeclaredType serializer, TypeMirror deserializedType) {
        if (serializer == null) {
            // this should not be able to happen since the serializer type must (by generics) always implement
            // the Serializer interface
            context.error("No serializer for preference %s", preference.name());
            return false;
        } else if (serializer.getTypeArguments().size() != 2) {
            context.error("Unable to identify type arguments of serializer %s for preference %s", serializer, preference.name());
            return false;
        } else if (!context.getTypeUtils().isSubtype(TypeUtils.tryBox(context, deserializedType), serializer.getTypeArguments().get(0))) {
            context.error("Incompatible serializer %s for type %s of preference %s", serializer, deserializedType, preference.name());
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
