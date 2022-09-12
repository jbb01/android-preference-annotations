package eu.jonahbauer.android.preference.annotations.util;

import android.content.res.Resources;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class InMemoryResources implements Resources {
    private final Map<Integer, String> map;

    public static Builder builder() {
        return new Builder();
    }

    private InMemoryResources(Map<Integer, String> map) {
        this.map = map;
    }

    @Override
    public String getString(int id) {
        if (map.containsKey(id)) {
            return map.get(id);
        } else {
            throw new NoSuchElementException();
        }
    }

    public static class Builder {
        private final Map<Integer, String> map = new HashMap<>();
        private Builder() {}

        public Builder put(int id, String value) {
            map.put(id, value);
            return this;
        }

        public InMemoryResources build() {
            return new InMemoryResources(new HashMap<>(map));
        }
    }
}
