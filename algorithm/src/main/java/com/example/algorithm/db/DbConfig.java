package com.example.algorithm.db;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads database connection properties from db.properties file.
 */
public class DbConfig {

    private static final Properties props = new Properties();

    static {
        try (InputStream in = DbConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new RuntimeException("db.properties not found in classpath");
            }
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load db.properties", e);
        }
    }

    public static String getUrl()      { return props.getProperty("db.url"); }
    public static String getUsername() { return props.getProperty("db.username"); }
    public static String getPassword() { return props.getProperty("db.password"); }
}

