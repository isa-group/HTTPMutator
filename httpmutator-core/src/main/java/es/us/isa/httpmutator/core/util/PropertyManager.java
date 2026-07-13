package es.us.isa.httpmutator.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

/**
 * 
 * @author Alberto Martin-Lopez
 */
public class PropertyManager {

	private static final String CLASSPATH_PROP = "http-mutation.properties";

	private static Properties properties = null;

	public static synchronized String readProperty(String name) {
		ensurePropertiesLoaded();
		return properties.getProperty(name);
	}

	public static synchronized void setProperty(String propertyName, String propertyValue) {
		ensurePropertiesLoaded();
		properties.setProperty(propertyName, propertyValue);
	}

	public static synchronized void resetProperties() {
		properties = loadClasspathProperties();
	}

	public static synchronized void loadProperties(Path propertiesFile) {
		Objects.requireNonNull(propertiesFile, "propertiesFile must not be null");
		Properties loadedProperties = loadClasspathProperties();
		try (InputStream in = Files.newInputStream(propertiesFile)) {
			loadedProperties.load(in);
		} catch (IOException e) {
			throw new IllegalArgumentException("Cannot load mutation properties from " + propertiesFile, e);
		}
		properties = loadedProperties;
	}

	private static Properties loadClasspathProperties() {
		Properties loadedProperties = new Properties();
		try (InputStream in = PropertyManager.class.getClassLoader().getResourceAsStream(CLASSPATH_PROP)) {
	            if (in == null) {
	                throw new IOException("Resource not found: " + CLASSPATH_PROP);
	            }
	            loadedProperties.load(in);
	        } catch (IOException e) {
	            System.err.printf("Error reading classpath config %s: %s%n", CLASSPATH_PROP, e.getMessage());
	            throw new RuntimeException("Cannot load mutation properties", e);
	        }
		return loadedProperties;
	}

	private static void ensurePropertiesLoaded() {
		if (properties==null) {
			resetProperties();
		}
	}
}
