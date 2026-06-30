package es.us.isa.httpmutator.core;

import es.us.isa.httpmutator.core.util.PropertyManager;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

public class ConfigurationPropertiesCoverageTest {

    private static final Set<String> ACTIVE_PROPERTIES = new TreeSet<>(Arrays.asList(
            "operator.sc.enabled",
            "operator.header.enabled",
            "operator.body.enabled",

            "operator.value.long.enabled",
            "operator.value.long.min",
            "operator.value.long.max",
            "operator.value.long.prob",
            "operator.value.long.replace.enabled",
            "operator.value.long.null.enabled",
            "operator.value.long.changeType.enabled",
            "operator.value.long.weight.replace",
            "operator.value.long.weight.null",
            "operator.value.long.weight.changeType",

            "operator.value.double.enabled",
            "operator.value.double.min",
            "operator.value.double.max",
            "operator.value.double.prob",
            "operator.value.double.replace.enabled",
            "operator.value.double.null.enabled",
            "operator.value.double.changeType.enabled",
            "operator.value.double.weight.replace",
            "operator.value.double.weight.null",
            "operator.value.double.weight.changeType",

            "operator.value.string.enabled",
            "operator.value.string.includeLetters",
            "operator.value.string.includeNumbers",
            "operator.value.string.includeAscii",
            "operator.value.string.length.min",
            "operator.value.string.length.max",
            "operator.value.string.uppercase",
            "operator.value.string.lowercase",
            "operator.value.string.prob",
            "operator.value.string.replace.enabled",
            "operator.value.string.addSpecialCharacters.enabled",
            "operator.value.string.boundary.enabled",
            "operator.value.string.null.enabled",
            "operator.value.string.changeType.enabled",
            "operator.value.string.weight.replace",
            "operator.value.string.weight.addSpecialCharacters",
            "operator.value.string.weight.boundary",
            "operator.value.string.weight.null",
            "operator.value.string.weight.changeType",

            "operator.value.boolean.enabled",
            "operator.value.boolean.prob",
            "operator.value.boolean.mutate.enabled",
            "operator.value.boolean.null.enabled",
            "operator.value.boolean.changeType.enabled",
            "operator.value.boolean.weight.mutate",
            "operator.value.boolean.weight.null",
            "operator.value.boolean.weight.changeType",

            "operator.value.null.enabled",
            "operator.value.null.prob",
            "operator.value.null.changeType.enabled",
            "operator.value.null.weight.changeType",

            "operator.object.enabled",
            "operator.object.addedElements.min",
            "operator.object.addedElements.max",
            "operator.object.removedElements.min",
            "operator.object.removedElements.max",
            "operator.object.removeObjectElement.min",
            "operator.object.removeObjectElement.max",
            "operator.object.mutations.min",
            "operator.object.mutations.max",
            "operator.object.prob",
            "operator.object.removeElement.enabled",
            "operator.object.removeObjectElement.enabled",
            "operator.object.addElement.enabled",
            "operator.object.null.enabled",
            "operator.object.changeType.enabled",
            "operator.object.weight.addElement",
            "operator.object.weight.removeElement",
            "operator.object.weight.removeObjectElement",
            "operator.object.weight.null",
            "operator.object.weight.changeType",

            "operator.array.enabled",
            "operator.array.addedElements.min",
            "operator.array.addedElements.max",
            "operator.array.removedElements.min",
            "operator.array.removedElements.max",
            "operator.array.mutations.min",
            "operator.array.mutations.max",
            "operator.array.prob",
            "operator.array.removeElement.enabled",
            "operator.array.empty.enabled",
            "operator.array.addElement.enabled",
            "operator.array.disorderElements.enabled",
            "operator.array.null.enabled",
            "operator.array.changeType.enabled",
            "operator.array.weight.addElement",
            "operator.array.weight.removeElement",
            "operator.array.weight.empty",
            "operator.array.weight.null",
            "operator.array.weight.changeType",
            "operator.array.weight.disorderElements",

            "operator.sc.prob",
            "operator.sc.replaceWith20x.enabled",
            "operator.sc.replaceWith40x.enabled",
            "operator.sc.replaceWith50x.enabled",
            "operator.sc.weight.replaceWith20x",
            "operator.sc.weight.replaceWith40x",
            "operator.sc.weight.replaceWith50x",

            "operator.header.mediaType.enabled",
            "operator.header.mediaType.prob",
            "operator.header.mediaType.replace.enabled",
            "operator.header.mediaType.null.enabled",
            "operator.header.mediaType.weight.replace",
            "operator.header.mediaType.weight.null",

            "operator.header.charset.enabled",
            "operator.header.charset.prob",
            "operator.header.charset.replace.enabled",
            "operator.header.charset.null.enabled",
            "operator.header.charset.weight.replace",
            "operator.header.charset.weight.null",

            "operator.header.location.enabled",
            "operator.header.location.prob",
            "operator.header.location.mutate.enabled",
            "operator.header.location.null.enabled",
            "operator.header.location.weight.mutate",
            "operator.header.location.weight.null"
    ));

    @Test
    public void defaultConfigurationContainsOnlyActiveProperties() throws Exception {
        Properties defaults = load("http-mutation.properties");

        Assert.assertEquals(ACTIVE_PROPERTIES, new TreeSet<>(defaults.stringPropertyNames()));
    }

    @Test
    public void restAndGraphqlConfigurationsUseSamePropertyKeysAsDefault() throws Exception {
        Set<String> defaults = new TreeSet<>(load("http-mutation.properties").stringPropertyNames());

        Assert.assertEquals(defaults, new TreeSet<>(load("rest-mutation.properties").stringPropertyNames()));
        Assert.assertEquals(defaults, new TreeSet<>(load("graphql-mutation.properties").stringPropertyNames()));
    }

    private Properties load(String resourceName) throws Exception {
        Properties properties = new Properties();
        try (InputStream input = PropertyManager.class.getClassLoader().getResourceAsStream(resourceName)) {
            Assert.assertNotNull("Missing classpath resource " + resourceName, input);
            properties.load(input);
        }
        return properties;
    }
}
