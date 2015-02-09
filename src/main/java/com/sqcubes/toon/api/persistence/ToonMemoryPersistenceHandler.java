package com.sqcubes.toon.api.persistence;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ToonMemoryPersistenceHandler implements ToonPersistenceHandler {
    final Map<String, Object> values;

    public ToonMemoryPersistenceHandler() {
        this.values = new HashMap<String, Object>();
    }

    @Override
    public void persistKeyValuePair(String key, Serializable value) {
        values.put(key, value);
    }

    @Override
    public void persistKeyValuePair(String key, String value) {
        values.put(key, value);
    }

    @Override
    public String getPersistedKeyValue(String key) {
        return getPersistedKeyValue(key, String.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Serializable> T getPersistedKeyValue(String key, Class<T> classOfT) {
        Object value = values.get(key);
        return (T)value;
    }
}
