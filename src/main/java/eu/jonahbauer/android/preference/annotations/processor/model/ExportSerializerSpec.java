package eu.jonahbauer.android.preference.annotations.processor.model;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import eu.jonahbauer.android.preference.annotations.Preference;
import eu.jonahbauer.android.preference.annotations.processor.TypeUtils;
import eu.jonahbauer.android.preference.annotations.serializer.EnumPreferenceExportSerializer;
import eu.jonahbauer.android.preference.annotations.serializer.PreferenceExportSerializer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

import javax.lang.model.element.Modifier;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.Set;

import static eu.jonahbauer.android.preference.annotations.processor.model.SerializerSpec.*;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ExportSerializerSpec {
    private static final Set<String> SUPPORTED_JSON_TYPES = Set.of(
            "boolean", "byte", "short", "int", "long", "float", "double",
            "java.lang.String", "org.json.JSONObject", "org.json.JSONArray"
    );

    /**
     * The JSON representation type of the export serializer, unboxed when possible. One of
     * {@link #SUPPORTED_JSON_TYPES}.
     */
    TypeMirror jsonType;
    FieldSpec exportSerializer;

    /**
     * Creates an {@link ExportSerializerSpec} for the given preference.
     * @param context the processing context
     * @param index the index of the preference within its group
     * @param preference the preference
     * @param serializerSpec the {@link SerializerSpec} of the preference
     * @return the spec, or {@code null} if no export serializer was specified or an error occurred
     */
    public static ExportSerializerSpec create(Context context, int index, Preference preference, SerializerSpec serializerSpec) {
        var deserializedType = serializerSpec.getDeserializedType();

        var serializerRawType = TypeUtils.mirror(preference, Preference::exportSerializer);
        if (serializerRawType == null || context.isSame(serializerRawType, PreferenceExportSerializer.class)) {
            if (serializerSpec.isEnumDefault()) {
                // use enum export serializer for enum types when no export serializer is specified
                serializerRawType = context.getType(EnumPreferenceExportSerializer.class);
            } else {
                return null;
            }
        }
        if (!(serializerRawType instanceof DeclaredType)) {
            context.error("Invalid export serializer type %s", serializerRawType);
            return null;
        }

        var declaredType = TypeUtils.mirror(preference, Preference::type);

        // add type arguments to serializer when necessary
        var serializerType = withTypeArguments(context, (DeclaredType) serializerRawType, declaredType);
        var serializerTypeName = TypeName.get(serializerType);
        // find export serializer interface in type hierarchy
        var serializerInt = findInterfaceType(context, serializerType, PreferenceExportSerializer.class);

        if (serializerInt == null || serializerInt.getTypeArguments().size() != 2) {
            context.error("Unable to identify type arguments of export serializer %s for preference %s", serializerType, preference.name());
            return null;
        }

        var sourceType = context.tryUnbox(serializerInt.getTypeArguments().get(0));
        var jsonType = context.tryUnbox(serializerInt.getTypeArguments().get(1));

        if (!context.getTypeUtils().isSameType(sourceType, deserializedType)) {
            context.error("Source type %s of export serializer for preference %s does not match the preference type %s", sourceType, preference.name(), deserializedType);
            return null;
        }

        if (!SUPPORTED_JSON_TYPES.contains(jsonType.toString())) {
            context.error("Unsupported export serializer target type %s for preference %s", jsonType, preference.name());
            return null;
        }

        // find constructor
        var constructor = hasClassConstructor(context, serializerType);
        if (constructor == null) {
            context.error("Could not find a suitable constructor in export serializer class " + serializerType + ".");
            return null;
        }

        var builder = FieldSpec.builder(serializerTypeName, "exportSerializer$" + index, Modifier.PRIVATE, Modifier.FINAL);
        if (constructor) {
            builder.initializer("new $T($T.class)", serializerTypeName, context.tryBox(declaredType));
        } else {
            builder.initializer("new $T()", serializerTypeName);
        }
        return new ExportSerializerSpec(jsonType, builder.build());
    }
}
