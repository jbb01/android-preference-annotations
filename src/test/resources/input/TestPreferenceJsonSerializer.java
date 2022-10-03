package eu.jonahbauer.android.preference.annotations.sources;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.jonahbauer.android.preference.annotations.Preference;
import eu.jonahbauer.android.preference.annotations.PreferenceGroup;
import eu.jonahbauer.android.preference.annotations.Preferences;
import eu.jonahbauer.android.preference.annotations.R;
import eu.jonahbauer.android.preference.annotations.serializer.PreferenceSerializer;

import java.util.Objects;

@Preferences(name = "eu.jonahbauer.android.preference.annotations.generated.TestPreferences", r = R.class, makeFile = true, value = {
        @PreferenceGroup(name = "general", prefix = "preferences_general_", suffix = "_key", value = {
                @Preference(name = "object_pref", type = TestPreferenceJsonSerializer.Bean.class, description = "an object preference", serializer = TestPreferenceJsonSerializer.JsonBeanSerializer.class)
        })
})
public final class TestPreferenceJsonSerializer {
    private TestPreferenceJsonSerializer() {}

    public static class Bean {
        private String foo;
        private String bar;

        public String getFoo() {
            return foo;
        }

        public void setFoo(String foo) {
            this.foo = foo;
        }

        public String getBar() {
            return bar;
        }

        public void setBar(String bar) {
            this.bar = bar;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Bean bean = (Bean) o;
            return Objects.equals(foo, bean.foo) && Objects.equals(bar, bean.bar);
        }

        @Override
        public int hashCode() {
            return Objects.hash(foo, bar);
        }
    }

    public static class JsonBeanSerializer<T> implements PreferenceSerializer<T, String> {
        private final Class<? extends T> clazz;
        private final ObjectMapper mapper = new ObjectMapper();

        public JsonBeanSerializer(Class<? extends T> clazz) {
            this.clazz = clazz;
        }

        public T deserialize(String value) {
            if (value == null) return null;
            try {
                return mapper.readValue(value, clazz);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public String serialize(T value) {
            if (value == null) return null;
            try {
                return mapper.writeValueAsString(value);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
