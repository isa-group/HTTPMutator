package es.us.isa.httpmutator.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 
 * @author Alberto Martin-Lopez
 */
public class PropertyManager {

	// private static String propertyFilePath = "src/main/resources/json-mutation.properties";
	private static final String CLASSPATH_PROP = "json-mutation.properties";

	private static 	Properties properties = null;

	public static String readProperty(String name) {
		loadProperties();
		return properties.getProperty(name);
	}

	public static void setProperty(String propertyName, String propertyValue) {
		loadProperties();
		properties.setProperty(propertyName, propertyValue);
	}

	public static void resetProperties() {
		properties = new Properties();
		try (InputStream in = PropertyManager.class.getClassLoader().getResourceAsStream(CLASSPATH_PROP)) {
            if (in == null) {
                throw new IOException("Resource not found: " + CLASSPATH_PROP);
            }
            properties.load(in);
        } catch (IOException e) {
            System.err.printf("Error reading classpath config %s: %s%n", CLASSPATH_PROP, e.getMessage());
            throw new RuntimeException("Cannot load mutation properties", e);
        }
	}

	private static void loadProperties() {
		if (properties==null) {
			resetProperties();
		}
	}
}
