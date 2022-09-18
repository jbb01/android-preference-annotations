package eu.jonahbauer.android.preference.annotations.processor;

import com.squareup.javapoet.TypeName;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Locale;
import java.util.regex.Pattern;

public final class StringUtils {
    private static final Pattern IDENTIFIER = Pattern.compile("\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*");
    private static final Pattern FQCN = Pattern.compile("\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*(\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)*");

    /**
     * Checks whether the given string is an identifier as per JLS 3.8, i.e. it starts with a
     * {@linkplain Character#isJavaIdentifierStart(char) Java Letter} and all the remaining characters are
     * {@linkplain Character#isJavaIdentifierPart(char) Java Letters or Digits}.
     * @param string the string to be checked
     */
    public static boolean isJavaIdentifier(String string) {
        return IDENTIFIER.matcher(string).matches();
    }

    /**
     * Checks whether the given string is a fully qualified class name as per JLS 6.7, i.e. a dot-seperated list of
     * {@linkplain #isJavaIdentifier(String) identifiers}.
     * @param string the string to be checked
     */
    public static boolean isFQCN(String string) {
        return FQCN.matcher(string).matches();
    }

    public static String getMethodName(String preferenceName) {
        int index;
        while ((index = preferenceName.indexOf('_')) != -1) {
            if (index == preferenceName.length() - 1) preferenceName = preferenceName.substring(0, index);
            preferenceName = preferenceName.substring(0, index)
                    + capitalize(preferenceName.substring(index + 1));
        }
        return preferenceName;
    }

    public static String getGetterName(String methodName, TypeName type, boolean fluent) {
        return getGetterName(methodName, TypeName.BOOLEAN.equals(type), fluent);
    }

    public static String getGetterName(String methodName, TypeMirror type, boolean fluent) {
        return getGetterName(methodName, type.getKind() == TypeKind.BOOLEAN, fluent);
    }

    public static String getGetterName(String methodName, boolean bool, boolean fluent) {
        if (fluent) {
            return methodName;
        } else if (bool) {
            return "is" + capitalize(methodName);
        } else {
            return "get" + capitalize(methodName);
        }
    }

    public static String getSetterName(String methodName, boolean fluent) {
        if (fluent) {
            return methodName;
        } else {
            return "set" + capitalize(methodName);
        }
    }

    public static String capitalize(String str) {
        switch (str.length()) {
            case 0:
                return "";
            case 1:
                return str.toUpperCase(Locale.ROOT);
            default:
                return str.substring(0, 1).toUpperCase(Locale.ROOT) + str.substring(1);
        }
    }
}
