package eu.jonahbauer.android.preference.annotations.processor;

import eu.jonahbauer.android.preference.annotations.Preference;
import eu.jonahbauer.android.preference.annotations.Preferences;
import eu.jonahbauer.android.preference.annotations.PreferenceGroup;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.function.Function;

@SupportedAnnotationTypes({
        "eu.jonahbauer.android.preference.annotations.Preferences"
})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public final class PreferenceProcessor extends AbstractProcessor {
    private static final String PACKAGE_DECLARATION =           "package %s ;%n";
    private static final String IMPORT_DECLARATION =            "import android.content.SharedPreferences;%n" +
                                                                "import android.content.res.Resources;%n" +
                                                                "import java.util.Objects;%n" +
                                                                "import %s;%n";
    private static final String CLASS_DECLARATION_START =       "class %1$s {%n" +
                                                                "    protected %1$s() {%n" +
                                                                "        throw new IllegalStateException(\"This class is not supposed to be instantiated.\");%n" +
                                                                "    }%n";
    private static final String CLASS_DECLARATION_START_FINAL = "public final class %1$s {%n" +
                                                                "    private %1$s() {%n" +
                                                                "        throw new IllegalStateException(\"This class is not supposed to be instantiated.\");%n" +
                                                                "    }%n";
    private static final String CLASS_PREFERENCES_DECLARATION = "    private static SharedPreferences sharedPreferences;%n";
    private static final String GROUP_FIELD_DECLARATION =       "    private static %1$s group$%2$d;%n";
    private static final String CLASS_INITIALIZER_START =       "    /**%n" +
                                                                "     * Initialize this preference class to use the given {@link SharedPreferences}.%n" +
                                                                "     * This function is supposed to be called from the applications {@code onCreate()} method.%n" +
                                                                "     * @param pSharedPreferences the {@link SharedPreferences} to be used. Not {@code null}.%n" +
                                                                "     * @param pResources the {@link Resources} from which the preference keys should be loaded. Not {@code null}.%n" +
                                                                "     * @throws IllegalStateException if this preference class has already been initialized.%n" +
                                                                "    */%n" +
                                                                "    public static void init(SharedPreferences pSharedPreferences, Resources pResources) {%n" +
                                                                "        if (sharedPreferences != null) {%n" +
                                                                "            throw new IllegalStateException(\"Preferences have already been initialized.\");%n" +
                                                                "        }%n" +
                                                                "        Objects.requireNonNull(pSharedPreferences, \"SharedPreferences must not be null.\");%n" +
                                                                "        Objects.requireNonNull(pResources, \"Resources must not be null.\");%n" +
                                                                "        sharedPreferences = pSharedPreferences;%n";
    private static final String CLASS_INITIALIZER_FIELD =       "        group$%2$d = new %1$s(pResources);%n";
    private static final String CLASS_INITIALIZER_END =         "    }%n";
    private static final String GROUP_ACCESSOR_DECLARATION =    "    public static %1$s %1$s() {%n" +
                                                                "        if (sharedPreferences == null) {%n" +
                                                                "            throw new IllegalStateException(\"Preferences have not yet been initialized.\");%n" +
                                                                "        }%n" +
                                                                "        return group$%2$d;%n" +
                                                                "    }%n";
    private static final String GROUP_CLASS_DECLARATION_START = "    public static final class %s {%n";
    private static final String PROPERTY_KEY =                  "        private final String key$%d;%n";
    private static final String GROUP_CLASS_CONSTRUCTOR_START = "        private %s(Resources resources) {%n";
    private static final String PROPERTY_KEY_INITIALIZER =      "            key$%d = resources.getString(R.string.%s);%n";
    private static final String GROUP_CLASS_CONSTRUCTOR_END =   "        }%n";
    private static final String PROPERTY_DOCUMENTATION =        "        /**%n" +
                                                                "         * %s%n" +
                                                                "         * (default: {@code %s})%n" +
                                                                "         */%n";
    private static final String PROPERTY_GETTER_START =         "        public %s %s() {%n";
    private static final String PROPERTY_GETTER_BODY_BOOLEAN =  "            return sharedPreferences.getBoolean(key$%d, %s);%n";
    private static final String PROPERTY_GETTER_BODY_INTEGER =  "            return sharedPreferences.getInt(key$%d, %s);%n";
    private static final String PROPERTY_GETTER_BODY_BYTE =     "            return (byte) sharedPreferences.getInt(key$%d, %s);%n";
    private static final String PROPERTY_GETTER_BODY_SHORT =    "            return (short) sharedPreferences.getInt(key$%d, %s);%n";
    private static final String PROPERTY_GETTER_BODY_CHAR =     "            return (char) sharedPreferences.getInt(key$%d, %s);%n";
    private static final String PROPERTY_GETTER_BODY_LONG =     "            return sharedPreferences.getLong(key$%d, %s);%n";
    private static final String PROPERTY_GETTER_BODY_FLOAT =    "            return sharedPreferences.getFloat(key$%d, %s);%n";
    private static final String PROPERTY_GETTER_BODY_DOUBLE =   "            return Double.longBitsToDouble(sharedPreferences.getLong(key$%d, %s));%n";
    private static final String PROPERTY_GETTER_BODY_STRING =   "            return sharedPreferences.getString(key$%d, %s);%n";
    private static final String PROPERTY_GETTER_END =           "        }%n";
    private static final String PROPERTY_SETTER_START =         "        public void %2$s(%1$s value) {%n";
    private static final String PROPERTY_SETTER_BODY_BOOLEAN =  "           sharedPreferences.edit().putBoolean(key$%d, value).apply();%n";
    private static final String PROPERTY_SETTER_BODY_INTEGER =  "           sharedPreferences.edit().putInt(key$%d, value).apply();%n";
    private static final String PROPERTY_SETTER_BODY_BYTE =     "           sharedPreferences.edit().putInt(key$%d, (int) value).apply();%n";
    private static final String PROPERTY_SETTER_BODY_SHORT =    "           sharedPreferences.edit().putInt(key$%d, (int) value).apply();%n";
    private static final String PROPERTY_SETTER_BODY_CHAR =     "           sharedPreferences.edit().putInt(key$%d, (int) value).apply();%n";
    private static final String PROPERTY_SETTER_BODY_LONG =     "           sharedPreferences.edit().putLong(key$%d, value).apply();%n";
    private static final String PROPERTY_SETTER_BODY_FLOAT =    "           sharedPreferences.edit().putFloat(key$%d, value).apply();%n";
    private static final String PROPERTY_SETTER_BODY_DOUBLE =   "           sharedPreferences.edit().putLong(key$%d, Double.doubleToRawLongBits(value)).apply();%n";
    private static final String PROPERTY_SETTER_BODY_STRING =   "           sharedPreferences.edit().putString(key$%d, value).apply();%n";
    private static final String PROPERTY_SETTER_END =           "        }%n";
    private static final String KEY_CLASS_FIELD_DECLARATION =   "        private final Keys keys = new Keys();%n";
    private static final String KEY_CLASS_ACCESSOR =            "        public Keys keys() {%n" +
                                                                "            return keys;%n" +
                                                                "        }%n";
    private static final String KEY_CLASS_DECLARATION_START =   "        public final class Keys {%n" +
                                                                "            private Keys() {}%n";
    private static final String KEY_CLASS_PROPERTY_ACCESSOR =   "            public String %s() {%n" +
                                                                "                return key$%d;%n" +
                                                                "            }%n";
    private static final String KEY_CLASS_DECLARATION_END =     "        }%n";
    private static final String GROUP_CLASS_DECLARATION_END =   "    }%n";
    private static final String CLASS_DECLARATION_END =         "}%n";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var clazzes = roundEnv.getElementsAnnotatedWith(Preferences.class);

        try {
            for (Element clazz : clazzes) {
                var root = clazz.getAnnotation(Preferences.class);
                var source = processingEnv.getFiler().createSourceFile(root.name());

                try (var out = new PrintWriter(source.openWriter())) {
                    writeRootClass(out, root);
                }
            }
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeRootClass(PrintWriter out, Preferences root) throws IOException {
        var groups = root.value();
        var fqcn = root.name();
        if (!StringUtils.isFQCN(root.name())) {
            throw new IllegalArgumentException("Illegal preference class name " + root.name() + ".");
        }
        var pckg = fqcn.lastIndexOf('.') != -1 ? fqcn.substring(0, fqcn.lastIndexOf('.')) : "";
        var cn = fqcn.lastIndexOf('.') != -1 ? fqcn.substring(fqcn.lastIndexOf('.') + 1) : fqcn;
        var r = mirror(root, Preferences::r);

        if (!pckg.isEmpty()) out.printf(PACKAGE_DECLARATION, pckg);
        out.println();
        out.printf(IMPORT_DECLARATION, r);
        out.println();
        if (root.makeFile()) {
            out.printf(CLASS_DECLARATION_START_FINAL, cn);
        } else {
            out.printf(CLASS_DECLARATION_START, cn);
        }

        out.printf(CLASS_PREFERENCES_DECLARATION);
        for (int i = 0; i < groups.length; i++) {
            out.printf(GROUP_FIELD_DECLARATION, groups[i].name(), i);
        }
        out.println();

        out.printf(CLASS_INITIALIZER_START);
        for (int i = 0; i < groups.length; i++) {
            out.printf(CLASS_INITIALIZER_FIELD, groups[i].name(), i);
        }
        out.printf(CLASS_INITIALIZER_END);
        out.println();

        for (int i = 0; i < groups.length; i++) {
            out.printf(GROUP_ACCESSOR_DECLARATION, groups[i].name(), i);
        }
        out.println();

        for (PreferenceGroup group : groups) {
            if (!StringUtils.isJavaIdentifier(group.name())) {
                throw new IllegalArgumentException("Illegal preference group name " + group.name() + ".");
            }
            if (!group.prefix().isEmpty() && !StringUtils.isJavaIdentifier(group.prefix())) {
                throw new IllegalArgumentException("Illegal preference group prefix " + group.prefix() + ".");
            }
            if (!group.suffix().isEmpty() && !group.suffix().matches("\\p{javaJavaIdentifierPart}*")) {
                throw new IllegalArgumentException("Illegal preference group suffix " + group.suffix() + ".");
            }
            writeGroupClass(out, group);
        }

        out.printf(CLASS_DECLARATION_END);
    }

    private void writeGroupClass(PrintWriter out, PreferenceGroup group) {
        var preferences = group.value();

        out.printf(GROUP_CLASS_DECLARATION_START, group.name());

        // Keys
        out.printf(KEY_CLASS_FIELD_DECLARATION);
        for (int j = 0; j < preferences.length; j++) {
            out.printf(PROPERTY_KEY, j);
        }
        out.println();

        // Constructor
        out.printf(GROUP_CLASS_CONSTRUCTOR_START, group.name());
        for (int i = 0; i < preferences.length; i++) {
            if (!StringUtils.isJavaIdentifier(preferences[i].name())) {
                throw new IllegalArgumentException("Illegal preference name " + preferences[i].name() + ".");
            }
            if (!StringUtils.isJavaIdentifier(group.prefix() + preferences[i].name() + group.suffix())) {
                throw new IllegalArgumentException("Illegal preference name " + preferences[i].name() + ".");
            }
            out.printf(PROPERTY_KEY_INITIALIZER, i, group.prefix() + preferences[i].name() + group.suffix());
        }
        out.printf(GROUP_CLASS_CONSTRUCTOR_END);
        out.println();

        // Key class accessor
        out.printf(KEY_CLASS_ACCESSOR);
        out.println();

        // Getters
        for (int j = 0; j < preferences.length; j++) {
            writeGetter(out, preferences[j], j);
        }
        out.println();

        // Setters
        for (int j = 0; j < preferences.length; j++) {
            writeSetter(out, preferences[j], j);
        }
        out.println();

        // Key class
        out.printf(KEY_CLASS_DECLARATION_START);
        for (int j = 0; j < preferences.length; j++) {
            out.printf(KEY_CLASS_PROPERTY_ACCESSOR, StringUtils.getMethodName(preferences[j].name()), j);
        }
        out.printf(KEY_CLASS_DECLARATION_END);
        out.println();

        out.printf(GROUP_CLASS_DECLARATION_END);
        out.println();
    }

    private static void writeGetter(PrintWriter out, Preference preference, int index) {
        var type = mirror(preference, Preference::type);
        if (type.getKind() == TypeKind.VOID) return;

        writeDocumentation(out, preference, type);
        out.printf(PROPERTY_GETTER_START, type, StringUtils.getMethodName(preference.name()));
        switch (type.getKind()) {
            case BOOLEAN:
                out.printf(PROPERTY_GETTER_BODY_BOOLEAN, index, getDefaultValue(preference, type));
                break;
            case BYTE:
                out.printf(PROPERTY_GETTER_BODY_BYTE, index, getDefaultValue(preference, type));
                break;
            case CHAR:
                out.printf(PROPERTY_GETTER_BODY_CHAR, index, getDefaultValue(preference, type));
                break;
            case SHORT:
                out.printf(PROPERTY_GETTER_BODY_SHORT, index, getDefaultValue(preference, type));
                break;
            case INT:
                out.printf(PROPERTY_GETTER_BODY_INTEGER, index, getDefaultValue(preference, type));
                break;
            case LONG:
                out.printf(PROPERTY_GETTER_BODY_LONG, index, getDefaultValue(preference, type));
                break;
            case FLOAT:
                out.printf(PROPERTY_GETTER_BODY_FLOAT, index, getDefaultValue(preference, type));
                break;
            case DOUBLE:
                out.printf(PROPERTY_GETTER_BODY_DOUBLE, index, getDefaultValue(preference, type));
                break;
        }
        if (String.class.getName().equals(type.toString())) {
            out.printf(PROPERTY_GETTER_BODY_STRING, index, getDefaultValue(preference, type));
        }
        out.printf(PROPERTY_GETTER_END);
    }

    private static void writeSetter(PrintWriter out, Preference preference, int index) {
        var type = mirror(preference, Preference::type);
        if (type.getKind() == TypeKind.VOID) return;

        writeDocumentation(out, preference, type);
        out.printf(PROPERTY_SETTER_START, type, StringUtils.getMethodName(preference.name()));
        switch (type.getKind()) {
            case BOOLEAN:
                out.printf(PROPERTY_SETTER_BODY_BOOLEAN, index);
                break;
            case BYTE:
                out.printf(PROPERTY_SETTER_BODY_BYTE, index);
                break;
            case SHORT:
                out.printf(PROPERTY_SETTER_BODY_SHORT, index);
                break;
            case CHAR:
                out.printf(PROPERTY_SETTER_BODY_CHAR, index);
                break;
            case INT:
                out.printf(PROPERTY_SETTER_BODY_INTEGER, index);
                break;
            case LONG:
                out.printf(PROPERTY_SETTER_BODY_LONG, index);
                break;
            case FLOAT:
                out.printf(PROPERTY_SETTER_BODY_FLOAT, index);
                break;
            case DOUBLE:
                out.printf(PROPERTY_SETTER_BODY_DOUBLE, index);
                break;
        }
        if (String.class.getName().equals(type.toString())) {
            out.printf(PROPERTY_SETTER_BODY_STRING, index);
        }
        out.printf(PROPERTY_SETTER_END);
    }

    private static void writeDocumentation(PrintWriter out, Preference preference, TypeMirror type) {
        if (preference.description().isEmpty()) return;
        out.printf(PROPERTY_DOCUMENTATION, preference.description(), getDefaultValue(preference, type));
    }

    private static String getDefaultValue(Preference preference, TypeMirror type) {
        if (!Preference.NO_DEFAULT_VALUE.equals(preference.defaultValue())) {
            if (String.class.getName().equals(type.toString())) {
                return "\"" + StringUtils.escape(preference.defaultValue()) + "\"";
            } else {
                return preference.defaultValue();
            }
        } else switch (type.getKind()) {
            case BOOLEAN: return "false";
            case BYTE: case CHAR: case SHORT: case INT: case LONG: case FLOAT: case DOUBLE: return "0";
            case ARRAY: case NULL: case DECLARED: return "null";
            default: throw new RuntimeException();
        }
    }

    private static <S> TypeMirror mirror(S object, Function<S, ? extends Class<?>> function) {
        try {
            function.apply(object);
            throw new RuntimeException();
        } catch (MirroredTypeException e) {
            return e.getTypeMirror();
        }
    }
}