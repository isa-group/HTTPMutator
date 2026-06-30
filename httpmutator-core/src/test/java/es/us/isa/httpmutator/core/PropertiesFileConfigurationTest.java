package es.us.isa.httpmutator.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.us.isa.httpmutator.core.util.PropertyManager;
import es.us.isa.httpmutator.core.util.RandomUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

public class PropertiesFileConfigurationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @After
    public void resetProperties() {
        PropertyManager.resetProperties();
        RandomUtils.setSeed(42L);
    }

    @Test
    public void defaultConfigurationUsesHttpMutationResourceName() {
        ClassLoader loader = PropertyManager.class.getClassLoader();

        Assert.assertNotNull(loader.getResource("http-mutation.properties"));
        Assert.assertNull(loader.getResource("json-mutation.properties"));
    }

    @Test
    public void exampleConfigurationsAreClasspathResources() {
        ClassLoader loader = PropertyManager.class.getClassLoader();

        Assert.assertNotNull(loader.getResource("rest-mutation.properties"));
        Assert.assertNotNull(loader.getResource("graphql-mutation.properties"));
    }

    @Test
    public void loadPropertiesOverlaysDefaultConfiguration() throws Exception {
        Path override = temporaryFolder.newFile("override.properties").toPath();
        Files.write(override,
                ("operator.sc.replaceWith40x.enabled=false\n"
                        + "operator.value.string.length.max=256\n").getBytes(StandardCharsets.UTF_8));

        PropertyManager.loadProperties(override);

        Assert.assertEquals("false", PropertyManager.readProperty("operator.sc.replaceWith40x.enabled"));
        Assert.assertEquals("true", PropertyManager.readProperty("operator.sc.replaceWith20x.enabled"));
        Assert.assertEquals("256", PropertyManager.readProperty("operator.value.string.length.max"));
    }

    @Test
    public void restConfigurationMatchesDefaultOperatorSelection() throws Exception {
        Properties defaults = loadClasspathProperties("http-mutation.properties");
        Properties rest = loadClasspathProperties("rest-mutation.properties");

        Assert.assertEquals(defaults, rest);
        Assert.assertEquals(operatorEnabledKeys(defaults), operatorEnabledKeys(rest));
        for (String key : operatorEnabledKeys(rest)) {
            Assert.assertEquals("REST should enable " + key, "true", rest.getProperty(key));
            Assert.assertEquals("Default should match REST for " + key,
                    rest.getProperty(key), defaults.getProperty(key));
        }
    }

    @Test
    public void graphqlConfigurationDisablesOnlyStatusCodeOperators() throws Exception {
        Properties rest = loadClasspathProperties("rest-mutation.properties");
        Properties graphql = loadClasspathProperties("graphql-mutation.properties");
        Set<String> enabledKeys = operatorEnabledKeys(rest);

        Assert.assertEquals(enabledKeys, operatorEnabledKeys(graphql));
        for (String key : enabledKeys) {
            String expected = key.startsWith("operator.sc.") ? "false" : "true";
            Assert.assertEquals("Unexpected GraphQL value for " + key, expected, graphql.getProperty(key));
        }
    }

    @Test
    public void graphqlPropertiesFileGeneratesHeaderAndBodyButNoStatusMutants() throws Exception {
        Path graphql = classpathResourcePath("graphql-mutation.properties");
        HttpMutatorEngine engine;

        PropertyManager.loadProperties(graphql);
        engine = new HttpMutatorEngine();
        JsonNode response = MAPPER.readTree(
                "{\"Status Code\":200,"
                        + "\"Headers\":{\"Content-Type\":\"application/json; charset=utf-8\"},"
                        + "\"Body\":{\"id\":1,\"name\":\"book\"}}");
        List<String> identifiers = new ArrayList<>();

        engine.getAllMutants(response, group -> identifiers.add(group.getIdentifier()));

        Assert.assertFalse(identifiers.contains("Status Code"));
        Assert.assertTrue(identifiers.stream().anyMatch(id -> id.startsWith("Headers/")));
        Assert.assertTrue(identifiers.stream().anyMatch(id -> id.startsWith("Body")));
    }

    @Test
    public void httpMutatorDefaultConstructorUsesDefaultSeed() throws Exception {
        HttpMutator mutator = new HttpMutator();

        Assert.assertEquals(42L, mutator.getRandomSeed());
        Assert.assertEquals(42L, RandomUtils.getSeed());
        mutator.close();
    }

    @Test
    public void httpMutatorPropertiesConstructorUsesDefaultSeed() throws Exception {
        Path graphql = classpathResourcePath("graphql-mutation.properties");

        HttpMutator mutator = new HttpMutator(graphql);

        Assert.assertEquals(42L, mutator.getRandomSeed());
        Assert.assertEquals(42L, RandomUtils.getSeed());
        mutator.close();
    }

    @Test
    public void httpMutatorSeedAndPropertiesConstructorInitializesBeforeEngine() throws Exception {
        Path graphql = classpathResourcePath("graphql-mutation.properties");
        HttpMutator mutator = new HttpMutator(777L, graphql);
        JsonNode response = MAPPER.readTree(
                "{\"Status Code\":200,"
                        + "\"Headers\":{\"Content-Type\":\"application/json; charset=utf-8\"},"
                        + "\"Body\":{\"id\":1,\"name\":\"book\"}}");
        List<JsonNode> mutants;

        mutator.withMutationStrategy(new es.us.isa.httpmutator.core.strategy.AllOperatorsStrategy());
        mutants = mutator.mutate(response, "graphql");

        Assert.assertEquals(777L, mutator.getRandomSeed());
        Assert.assertEquals(777L, RandomUtils.getSeed());
        Assert.assertTrue(mutants.stream().noneMatch(node -> node.path("Status Code").asInt() != 200));
        Assert.assertTrue(mutants.stream().anyMatch(node -> node.path("Headers").toString()
                .contains("Content-Type")));
        mutator.close();
    }

    private static Properties loadClasspathProperties(String resourceName) throws Exception {
        Properties properties = new Properties();
        try (InputStream input = PropertyManager.class.getClassLoader().getResourceAsStream(resourceName)) {
            Assert.assertNotNull("Missing classpath resource " + resourceName, input);
            properties.load(input);
        }
        return properties;
    }

    private static Path classpathResourcePath(String resourceName) throws Exception {
        return java.nio.file.Paths.get(PropertyManager.class.getClassLoader()
                .getResource(resourceName)
                .toURI());
    }

    private static Set<String> operatorEnabledKeys(Properties properties) {
        Set<String> keys = new TreeSet<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("operator.") && key.endsWith(".enabled") && key.split("\\.").length > 3) {
                keys.add(key);
            }
        }
        return keys;
    }
}
