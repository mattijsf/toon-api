package com.sqcubes.toon.api.persistence;

import java.io.Serializable;

public interface ToonPersistenceHandler {
    void persistKeyValuePair(String key, Serializable value);
    void persistKeyValuePair(String key, String value);

    String getPersistedKeyValue(String key);
    <T extends Serializable> T getPersistedKeyValue(String key, Class<T>classOfT);
}
