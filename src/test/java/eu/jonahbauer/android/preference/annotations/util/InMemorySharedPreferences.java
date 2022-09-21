package eu.jonahbauer.android.preference.annotations.util;

import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class InMemorySharedPreferences implements SharedPreferences {
    private final AtomicReference<Map<String, ?>> map = new AtomicReference<>(new HashMap<>());

    @SuppressWarnings("unchecked")
    private <T> T get(String key, T defaultValue) {
        var map = this.map.get();
        if (map.containsKey(key)) {
            return (T) map.get(key);
        } else {
            return defaultValue;
        }
    }

    @Override
    public Map<String, ?> getAll() {
        return map.get();
    }

    @Override
    public String getString(String key, String defValue) {
        return get(key, defValue);
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        return get(key, defValues);
    }

    @Override
    public int getInt(String key, int defValue) {
        return get(key, defValue);
    }

    @Override
    public long getLong(String key, long defValue) {
        return get(key, defValue);
    }

    @Override
    public float getFloat(String key, float defValue) {
        return get(key, defValue);
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return get(key, defValue);
    }

    @Override
    public boolean contains(String key) {
        return map.get().containsKey(key);
    }

    @Override
    public Editor edit() {
        return new Editor();
    }

    private class Editor implements SharedPreferences.Editor {
        private final Map<String, Object> changes = new HashMap<>(map.get());

        @Override
        public SharedPreferences.Editor putString(String key, String value) {
            changes.put(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor putStringSet(String key, Set<String> values) {
            changes.put(key, Set.copyOf(values));
            return this;
        }

        @Override
        public SharedPreferences.Editor putInt(String key, int value) {
            changes.put(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor putLong(String key, long value) {
            changes.put(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor putFloat(String key, float value) {
            changes.put(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor putBoolean(String key, boolean value) {
            changes.put(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor remove(String key) {
            changes.remove(key);
            return this;
        }

        @Override
        public SharedPreferences.Editor clear() {
            changes.clear();
            return this;
        }

        @Override
        public boolean commit() {
            InMemorySharedPreferences.this.map.set(changes);
            return true;
        }

        @Override
        public void apply() {
            commit();
        }
    }
}
