package eu.jonahbauer.android.preference.annotations.sources;

import eu.jonahbauer.android.preference.annotations.Preference;
import eu.jonahbauer.android.preference.annotations.PreferenceGroup;
import eu.jonahbauer.android.preference.annotations.Preferences;
import eu.jonahbauer.android.preference.annotations.R;
import eu.jonahbauer.android.preference.annotations.serializer.PreferenceExportSerializer;
import eu.jonahbauer.android.preference.annotations.serializer.PreferenceSerializer;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.Set;

@Preferences(name = "eu.jonahbauer.android.preference.annotations.generated.TestPreferences", r = R.class, makeFile = true, value = {
        @PreferenceGroup(name = "general", prefix = "preferences_general_", suffix = "_key", value = {
                @Preference(name = "boolean_pref", type = boolean.class, export = true),
                @Preference(name = "byte_pref", type = byte.class, export = true),
                @Preference(name = "short_pref", type = short.class, export = true),
                @Preference(name = "char_pref", type = char.class, export = true),
                @Preference(name = "int_pref", type = int.class, export = true),
                @Preference(name = "long_pref", type = long.class, export = true),
                @Preference(name = "float_pref", type = float.class, export = true),
                @Preference(name = "double_pref", type = double.class, export = true),
                @Preference(name = "string_pref", type = String.class, export = true),
                @Preference(name = "set_pref", type = Set.class, export = true),
                @Preference(name = "enum_pref", type = StandardOpenOption.class, export = true),
                @Preference(name = "void_pref", type = void.class),
                @Preference(name = "list_pref", type = String.class),
                @Preference(name = "big_int_pref", type = BigInteger.class, export = true,
                        serializer = TestPreferencesExport.BigIntSerializer.class,
                        exportSerializer = TestPreferencesExport.BigIntExportSerializer.class),
                @Preference(name = "object_pref", type = TestPreferencesExport.Point.class, export = true,
                        serializer = TestPreferencesExport.PointSerializer.class,
                        exportSerializer = TestPreferencesExport.PointExportSerializer.class)
        })
})
public final class TestPreferencesExport {
    private TestPreferencesExport() {}

    public static class BigIntSerializer implements PreferenceSerializer<BigInteger, String> {
        public String serialize(BigInteger value) {
            if (value == null) return null;
            return Base64.getEncoder().encodeToString(value.toByteArray());
        }

        public BigInteger deserialize(String value) {
            if (value == null) return null;
            return new BigInteger(Base64.getDecoder().decode(value));
        }
    }

    public static class BigIntExportSerializer implements PreferenceExportSerializer<BigInteger, String> {
        public String export(BigInteger value) {
            if (value == null) return null;
            return value.toString();
        }

        public BigInteger restore(String value) {
            if (value == null) return null;
            return new BigInteger(value);
        }
    }

    public static class Point {
        private final int x;
        private final int y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Point)) return false;
            Point point = (Point) o;
            return x == point.x && y == point.y;
        }

        @Override
        public int hashCode() {
            return 31 * x + y;
        }
    }

    public static class PointSerializer implements PreferenceSerializer<Point, String> {
        public String serialize(Point value) {
            if (value == null) return null;
            return value.getX() + "," + value.getY();
        }

        public Point deserialize(String value) {
            if (value == null) return null;
            var parts = value.split(",");
            return new Point(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
    }

    public static class PointExportSerializer implements PreferenceExportSerializer<Point, JSONObject> {
        public JSONObject export(Point value) {
            if (value == null) return null;
            try {
                return new JSONObject().put("x", value.getX()).put("y", value.getY());
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        public Point restore(JSONObject value) {
            if (value == null) return null;
            try {
                return new Point(value.getInt("x"), value.getInt("y"));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
