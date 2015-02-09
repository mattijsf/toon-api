package com.sqcubes.toon.api.persistence;

import java.io.*;
import java.util.Properties;

public class ToonFilePersistenceHandler implements ToonPersistenceHandler {
    private final String file;
    private final Properties props;

    public ToonFilePersistenceHandler(String file) throws IOException {
        this.file = file;
        this.props = new Properties();

        File f = new File(file);
        if (f.exists() || f.createNewFile()){
            props.load(new FileReader(file));
        }
        else{
            throw new IllegalStateException("Cannot open or create file at " + file);
        }
    }

    @Override
    public void persistKeyValuePair(String key, Serializable value) {
        throw new IllegalStateException("Not yet implemented");
    }

    @Override
    public void persistKeyValuePair(String key, String value) {
        props.put(key, value);
        try {
            props.store(new FileWriter(file), null);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String getPersistedKeyValue(String key) {
        return props.getProperty(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Serializable> T getPersistedKeyValue(String key, Class<T> classOfT) {
        throw new IllegalStateException("Not yet implemented");
    }
}
