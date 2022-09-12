package eu.jonahbauer.android.preference.annotations.processor;

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
    static boolean isJavaIdentifier(String string) {
        return IDENTIFIER.matcher(string).matches();
    }

    /**
     * Checks whether the given string is a fully qualified class name as per JLS 6.7, i.e. a dot-seperated list of
     * {@linkplain #isJavaIdentifier(String) identifiers}.
     * @param string the string to be checked
     */
    static boolean isFQCN(String string) {
        return FQCN.matcher(string).matches();
    }

    static String getMethodName(String preferenceName) {
        int index;
        while ((index = preferenceName.indexOf('_')) != -1) {
            if (index == preferenceName.length() - 1) preferenceName = preferenceName.substring(0, index);
            preferenceName = preferenceName.substring(0, index)
                    + preferenceName.substring(index + 1, index + 2).toUpperCase(Locale.ROOT)
                    + preferenceName.substring(index + 2);
        }
        return preferenceName;
    }

    /**
     * Escapes the given string for use in a String literal in Java source code. Note that quotes are not added
     * automatically.
     */
    static String escape(String string) {
        return string.replace("\\", "\\\\")
                     .replace("\t", "\\t")
                     .replace("\b", "\\b")
                     .replace("\n", "\\n")
                     .replace("\r", "\\r")
                     .replace("\f", "\\f")
                     .replace("'", "\\'")
                     .replace("\"", "\\\"");
    }
}
