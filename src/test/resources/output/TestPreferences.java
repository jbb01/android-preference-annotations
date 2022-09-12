package eu.jonahbauer.android.preference.annotations.sources;

import android.content.SharedPreferences;
import android.content.res.Resources;
import eu.jonahbauer.android.preference.annotations.R;

import java.util.Objects;

class TestPreferences {
    private static SharedPreferences sharedPreferences;
    private static general group$0;

    protected TestPreferences() {
        throw new IllegalStateException("This class is not supposed to be instantiated.");
    }

    public static init(SharedPreferences pSharedPreferences, Resources pResources) {
        if (sharedPreferences != null) {
            throw new IllegalStateException("Preferences have already been initialized.");
        }
        Objects.requireNonNull(pSharedPreferences, "SharedPreferences must not be null.");
        Objects.requireNonNull(pResources, "Resources must not be null.");
        sharedPreferences = pSharedPreferences;
        group$0 = new general(pResources);
    }

    public static general() {
        return group$0;
    }

    public static final class general {
        private final Keys keys;
        private final String key$0;
        private final String key$1;
        private final String key$2;
        private final String key$3;
        private final String key$4;
        private final String key$5;
        private final String key$6;
        private final String key$7;
        private final String key$8;

        private Keys(Resources resources) {
            this.keys = new Keys();
            this.key$0 = resources.getString(R.string.preferences_general_byte_pref_key);
            this.key$1 = resources.getString(R.string.preferences_general_short_pref_key);
            this.key$2 = resources.getString(R.string.preferences_general_char_pref_key);
            this.key$3 = resources.getString(R.string.preferences_general_int_pref_key);
            this.key$4 = resources.getString(R.string.preferences_general_long_pref_key);
            this.key$5 = resources.getString(R.string.preferences_general_float_pref_key);
            this.key$6 = resources.getString(R.string.preferences_general_double_pref_key);
            this.key$7 = resources.getString(R.string.preferences_general_string_pref_key);
            this.key$8 = resources.getString(R.string.preferences_general_void_pref_key);
        }

        public Keys keys() {
            return keys;
        }

        public byte bytePref() {
            return (byte) sharedPreferences.getInt(key$0, 0);
        }

        public short shortPref() {
            return (short) sharedPreferences.getInt(key$1, 0);
        }

        public char charPref() {
            return (char) sharedPreferences.getInt(key$2, 0);
        }

        public int intPref() {
            return (int) sharedPreferences.getInt(key$3, 0);
        }

        public long longPref() {
            return sharedPreferences.getLong(key$4, 0);
        }

        public float floatPref() {
            return sharedPreferences.getFloat(key$5, 0);
        }

        public double doublePref() {
            return Double.longBitsToDouble(sharedPreferences.getLong(key$6, 0));
        }

        public String stringPref() {
            return sharedPreferences.getString(key$7, null);
        }

        public void bytePref(byte value) {
            sharedPreferences.edit().putInt(key$0, (int) value).apply();
        }

        public void shortPref(short value) {
            sharedPreferences.edit().putInt(key$1, (int) value).apply();
        }

        public void charPref(char value) {
            sharedPreferences.edit().putInt(key$2, (int) value).apply();
        }

        public void intPref(int value) {
            sharedPreferences.edit().putInt(key$3, (int) value).apply();
        }

        public void longPref(long value) {
            sharedPreferences.edit().putLong(key$0, value).apply();
        }

        public void floatPref(float value) {
            sharedPreferences.edit().putFloat(key$0, value).apply();
        }

        public void doublePref(double value) {
            sharedPreferences.edit().putLong(key$0, Double.doubleToRawLongBits(value)).apply();
        }

        public void stringPref(String value) {
            sharedPreferences.edit().putString(key$0, value).apply();
        }

        public class Keys {
            private Keys() {}

            public String bytePref() {
                return key$0;
            }

            public String shortPref() {
                return key$1;
            }

            public String charPref() {
                return key$2;
            }

            public String intPref() {
                return key$3;
            }

            public String longPref() {
                return key$4;
            }

            public String floatPref() {
                return key$5;
            }

            public String doublePref() {
                return key$6;
            }

            public String stringPref() {
                return key$7;
            }

            public String voidPref() {
                return key$8;
            }
        }
    }
}