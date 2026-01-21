package es.us.isa.httpmutator.core.body;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.us.isa.httpmutator.core.model.Mutant;

import static junit.framework.TestCase.assertEquals;

public class BodyMutatorTest {

    private JsonNode jsonNode = null;
    private String jsonString = null;
    private ObjectMapper objectMapper;
    private BodyMutator jsonMutator;

    @Before
    public void setUp() {
        objectMapper = new ObjectMapper();

        // Read JSON file and create JSON node and JSON string
        try {
            jsonNode = objectMapper.readTree(new String(Files.readAllBytes(Paths.get("src/test/resources/test-object.json"))));
            jsonString = objectMapper.writeValueAsString(jsonNode);
        } catch (IOException e) {
            System.out.println("Unable to read JSON");
            e.printStackTrace();
            fail();
        }

        // Create mutator
        jsonMutator = new BodyMutator();
    }

    @Test
    @Ignore
    public void test() throws IOException {
        System.out.println("Original JSON object:\n.\n.\n.\n");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(jsonNode.toString(), Object.class)));
        JsonNode mutatedJsonNode1 = jsonMutator.mutateJson(jsonNode, true);
        System.out.println(".\n.\n.\nSingle order mutation:\n.\n.\n.\n");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(mutatedJsonNode1.toString(), Object.class)));
        JsonNode mutatedJsonNode2 = jsonMutator.mutateJson(jsonNode, false);
        System.out.println(".\n.\n.\nMultiple order mutation:\n.\n.\n.\n");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(mutatedJsonNode2.toString(), Object.class)));
        JsonNode mutatedJsonNode3 = jsonMutator.mutateJson(jsonNode, true);
        System.out.println(".\n.\n.\nSingle order mutation:\n.\n.\n.\n");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(mutatedJsonNode3.toString(), Object.class)));
    }

    @Test
    public void singleOrderMutation() throws IOException {
        jsonMutator.setProperty("operator.value.long.enabled", "true"); // Make sure at least one mutator is enabled
        JsonNode mutatedJsonNode1 = jsonMutator.mutateJson(jsonNode, true);
//        System.out.println("Original JSON object:\n.\n.\n.\n");
//        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(jsonNode.toString(), Object.class)));
//        System.out.println(".\n.\n.\nSingle order mutation:\n.\n.\n.\n");
//        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(mutatedJsonNode1.toString(), Object.class)));
        assertNotEquals("The mutated JSON should be different to the original JSON", jsonNode, mutatedJsonNode1);
    }

    @Test
    public void multipleOrderMutation() throws IOException {
        jsonMutator.setProperty("operator.value.long.enabled", "true"); // Make sure at least one mutator is enabled and its probability is set to 1
        jsonMutator.setProperty("operator.value.long.prob", "1");
        JsonNode mutatedJsonNode1 = jsonMutator.mutateJson(jsonNode, false);
//        System.out.println("Original JSON object:\n.\n.\n.\n");
//        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(jsonNode.toString(), Object.class)));
//        System.out.println(".\n.\n.\Multiple order mutation:\n.\n.\n.\n");
//        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(mutatedJsonNode1.toString(), Object.class)));
        assertNotEquals("The mutated JSON should be different to the original JSON", jsonNode, mutatedJsonNode1);
    }

    @Test
    public void singleOrderMutationCannotBeApplied() throws IOException {
        deactivateAllMutators(); // Deactivate all mutators
        JsonNode mutatedJsonNode1 = jsonMutator.mutateJson(jsonNode, true);
//        System.out.println("Original JSON object:\n.\n.\n.\n");
//        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(jsonNode.toString(), Object.class)));
//        System.out.println(".\n.\n.\nSingle order mutation:\n.\n.\n.\n");
//        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(mutatedJsonNode1.toString(), Object.class)));
        assertEquals("The mutated JSON should be equal to the original JSON", jsonNode, mutatedJsonNode1);
    }

    @Test
    public void multipleOrderMutationCannotBeApplied() throws IOException {
        deactivateAllMutators(); // Deactivate all mutators
        JsonNode mutatedJsonNode1 = jsonMutator.mutateJson(jsonNode, false);
//        System.out.println("Original JSON object:\n.\n.\n.\n");
//        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(jsonNode.toString(), Object.class)));
//        System.out.println(".\n.\n.\Multiple order mutation:\n.\n.\n.\n");
//        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(mutatedJsonNode1.toString(), Object.class)));
        assertEquals("The mutated JSON should be equal to the original JSON", jsonNode, mutatedJsonNode1);
    }

    @Test
    public void resetProperties() {
        deactivateAllMutators(); // Deactivate all mutators
        jsonMutator.resetProperties();
        JsonNode mutatedJsonNode1 = jsonMutator.mutateJson(jsonNode, false);
//        System.out.println("Original JSON object:\n.\n.\n.\n");
//        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(jsonNode.toString(), Object.class)));
//        System.out.println(".\n.\n.\Multiple order mutation:\n.\n.\n.\n");
//        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(mutatedJsonNode1.toString(), Object.class)));
        assertNotEquals("The mutated JSON should be different to the original JSON", jsonNode, mutatedJsonNode1);
    }

    @Test
    public void singleOrderMutationAsString() throws IOException {
        jsonMutator.setProperty("operator.value.long.enabled", "true"); // Make sure at least one mutator is enabled
        String mutatedJsonString1 = jsonMutator.mutateJson(jsonString, true);
//        System.out.println("Original JSON object:\n.\n.\n.\n");
//        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(jsonNode.toString(), Object.class)));
//        System.out.println(".\n.\n.\nSingle order mutation:\n.\n.\n.\n");
//        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(mutatedJsonNode1.toString(), Object.class)));
        assertNotEquals("The mutated JSON should be different to the original JSON", jsonString, mutatedJsonString1);
    }

    @Test
    public void multipleOrderMutationAsString() throws IOException {
        jsonMutator.setProperty("operator.value.long.enabled", "true"); // Make sure at least one mutator is enabled and its probability is set to 1
        jsonMutator.setProperty("operator.value.long.prob", "1");
        String mutatedJsonString1 = jsonMutator.mutateJson(jsonString, false);
//        System.out.println("Original JSON object:\n.\n.\n.\n");
//        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(jsonNode.toString(), Object.class)));
//        System.out.println(".\n.\n.\Multiple order mutation:\n.\n.\n.\n");
//        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(mutatedJsonNode1.toString(), Object.class)));
        assertNotEquals("The mutated JSON should be different to the original JSON", jsonString, mutatedJsonString1);
    }

    @Test
    public void singleOrderMutationCannotBeAppliedAsString() throws IOException {
        deactivateAllMutators(); // Deactivate all mutators
        String mutatedJsonString1 = jsonMutator.mutateJson(jsonString, true);
//        System.out.println("Original JSON object:\n.\n.\n.\n");
//        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(jsonNode.toString(), Object.class)));
//        System.out.println(".\n.\n.\nSingle order mutation:\n.\n.\n.\n");
//        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(mutatedJsonNode1.toString(), Object.class)));
        assertEquals("The mutated JSON should be equal to the original JSON", jsonString, mutatedJsonString1);
    }

    @Test
    public void multipleOrderMutationCannotBeAppliedAsString() throws IOException {
        deactivateAllMutators(); // Deactivate all mutators
        String mutatedJsonString1 = jsonMutator.mutateJson(jsonString, false);
//        System.out.println("Original JSON object:\n.\n.\n.\n");
//        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(jsonNode.toString(), Object.class)));
//        System.out.println(".\n.\n.\Multiple order mutation:\n.\n.\n.\n");
//        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(mutatedJsonNode1.toString(), Object.class)));
        assertEquals("The mutated JSON should be equal to the original JSON", jsonString, mutatedJsonString1);
    }

    @Test
    public void resetPropertiesAsString() {
        deactivateAllMutators(); // Deactivate all mutators
        jsonMutator.resetProperties();
        String mutatedJsonString1 = jsonMutator.mutateJson(jsonString, false);
//        System.out.println("Original JSON object:\n.\n.\n.\n");
//        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(jsonNode.toString(), Object.class)));
//        System.out.println(".\n.\n.\Multiple order mutation:\n.\n.\n.\n");
//        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(mutatedJsonNode1.toString(), Object.class)));
        assertNotEquals("The mutated JSON should be different to the original JSON", jsonString, mutatedJsonString1);
    }

    @Test
    public void badJsonString() {
        String badString = "bad string";
        String mutatedString = jsonMutator.mutateJson(badString, true);
        assertEquals("The mutated string should be equal to the original string", badString, mutatedString);
    }

    @Test
    public void testAlwaysMutateEmptyArray() {
        activateAllMutators();

        String originalObjectString = "[]";

        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
    }

    @Test
    public void testAlwaysMutateEmptyObject() {
        activateAllMutators();

        String originalObjectString = "{}";

        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
    }

    @Test
    public void testAlwaysMutateObject() {
        activateAllMutators();

        String originalObjectString = "{\"prop1\": \"val1\", \"prop2\": [1, true, {}]}";

        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
        assertNotEquals(originalObjectString, jsonMutator.mutateJson(originalObjectString, true));
    }

    @Test
    public void getAllMutants() {
        activateAllMutators();
        List<Mutant> mutants = jsonMutator.getAllMutants(jsonNode);
        assertEquals("The number of generated mutants does not match", 192, mutants.size());
    }

    @Test
    public void getAllMutantsAsString() {
        activateAllMutators();
        List<String> mutants = jsonMutator.getAllMutants(jsonString);
        assertEquals("The number of generated mutants does not match", 192, mutants.size());
    }

    @Test
    public void getAllMutantsProbZero() {
        activateAllMutators();
        List<Mutant> mutants = jsonMutator.getAllMutants(jsonNode, 0);
        assertEquals("The number of generated mutants does not match", 0, mutants.size());
    }

    @Test
    public void getAllMutantsProbZeroAsString() {
        activateAllMutators();
        List<String> mutants = jsonMutator.getAllMutants(jsonString, 0);
        assertEquals("The number of generated mutants does not match", 0, mutants.size());
    }

    private void deactivateAllMutators() {
        jsonMutator.setProperty("operator.value.long.enabled", "false");
        jsonMutator.setProperty("operator.value.double.enabled", "false");
        jsonMutator.setProperty("operator.value.string.enabled", "false");
        jsonMutator.setProperty("operator.value.boolean.enabled", "false");
        jsonMutator.setProperty("operator.value.null.enabled", "false");
        jsonMutator.setProperty("operator.object.enabled", "false");
        jsonMutator.setProperty("operator.array.enabled", "false");
    }

    private void activateAllMutators() {
        jsonMutator.setProperty("operator.value.long.enabled", "true");
        jsonMutator.setProperty("operator.value.double.enabled", "true");
        jsonMutator.setProperty("operator.value.string.enabled", "true");
        jsonMutator.setProperty("operator.value.boolean.enabled", "true");
        jsonMutator.setProperty("operator.value.null.enabled", "true");
        jsonMutator.setProperty("operator.object.enabled", "true");
        jsonMutator.setProperty("operator.array.enabled", "true");
    }
}
