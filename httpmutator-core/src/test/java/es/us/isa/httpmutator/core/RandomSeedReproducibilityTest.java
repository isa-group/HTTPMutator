package es.us.isa.httpmutator.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.us.isa.httpmutator.core.model.Mutant;
import es.us.isa.httpmutator.core.model.MutantGroup;
import es.us.isa.httpmutator.core.strategy.RandomSingleStrategy;
import es.us.isa.httpmutator.core.util.RandomUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests that the random seed controls all sources of randomness in the mutation system.
 * Verifies reproducibility after unifying RandomManager into RandomUtils.
 */
public class RandomSeedReproducibilityTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String SAMPLE_RESPONSE = "{\n" +
            "  \"Status Code\": 200,\n" +
            "  \"Headers\": {\"Content-Type\": \"application/json; charset=utf-8\", \"Location\": \"/api/v1/resource\"},\n" +
            "  \"Body\": {\n" +
            "    \"id\": 42,\n" +
            "    \"name\": \"test\",\n" +
            "    \"score\": 3.14,\n" +
            "    \"active\": true,\n" +
            "    \"tags\": [\"alpha\", \"beta\", \"gamma\"],\n" +
            "    \"metadata\": {\"key1\": \"value1\", \"key2\": 99}\n" +
            "  }\n" +
            "}";

    /**
     * Collects all mutant identifiers from a run.
     */
    private List<String> collectMutantIds(JsonNode responseNode, long seed) {
        RandomUtils.setSeed(seed);
        HttpMutatorEngine engine = new HttpMutatorEngine();
        List<String> ids = new ArrayList<>();
        engine.getAllMutants(responseNode, group -> {
            for (Mutant m : group.getMutants()) {
                try {
                    ids.add(m.getOriginalJsonPath() + "|" +
                            MAPPER.writeValueAsString(m.getMutatedNode()));
                } catch (Exception e) {
                    ids.add(m.getOriginalJsonPath() + "|ERROR");
                }
            }
        });
        return ids;
    }

    /**
     * Same seed must produce identical mutation results across runs.
     */
    @Test
    public void testSameSeedProducesSameResults() throws Exception {
        JsonNode responseNode = MAPPER.readTree(SAMPLE_RESPONSE);

        List<String> run1 = collectMutantIds(responseNode, 42L);
        List<String> run2 = collectMutantIds(responseNode, 42L);

        Assert.assertEquals("Same seed should produce same number of mutants",
                run1.size(), run2.size());
        Assert.assertEquals("Same seed should produce identical mutants",
                run1, run2);
    }

    /**
     * Different seeds must produce different mutation results.
     */
    @Test
    public void testDifferentSeedProducesDifferentResults() throws Exception {
        JsonNode responseNode = MAPPER.readTree(SAMPLE_RESPONSE);

        List<String> run1 = collectMutantIds(responseNode, 42L);
        List<String> run2 = collectMutantIds(responseNode, 12345L);

        Assert.assertEquals("Different seeds should produce same number of mutants",
                run1.size(), run2.size());
        Assert.assertNotEquals("Different seeds should produce different mutants",
                run1, run2);
    }

    /**
     * Tests reproducibility via the high-level HttpMutator API (which sets seed via constructor).
     */
    @Test
    public void testHttpMutatorSeedReproducibility() throws Exception {
        JsonNode responseNode = MAPPER.readTree(SAMPLE_RESPONSE);

        // Run 1 with seed 42
        HttpMutator mutator1 = new HttpMutator(42L);
        mutator1.withMutationStrategy(new RandomSingleStrategy());
        List<JsonNode> result1 = mutator1.mutate(responseNode, "test1");
        mutator1.close();

        // Run 2 with seed 42
        HttpMutator mutator2 = new HttpMutator(42L);
        mutator2.withMutationStrategy(new RandomSingleStrategy());
        List<JsonNode> result2 = mutator2.mutate(responseNode, "test2");
        mutator2.close();

        Assert.assertEquals("Same seed via HttpMutator should produce same number of results",
                result1.size(), result2.size());

        // Compare serialized forms
        for (int i = 0; i < result1.size(); i++) {
            Assert.assertEquals("Mutant #" + i + " should be identical",
                    MAPPER.writeValueAsString(result1.get(i)),
                    MAPPER.writeValueAsString(result2.get(i)));
        }
    }

    /**
     * Tests that withRandomSeed() changes all random behavior (not just RandomUtils).
     */
    @Test
    public void testWithRandomSeedAffectsAllBehavior() throws Exception {
        JsonNode responseNode = MAPPER.readTree(SAMPLE_RESPONSE);

        HttpMutator mutator = new HttpMutator(42L);
        mutator.withMutationStrategy(new RandomSingleStrategy());
        List<JsonNode> result42 = mutator.mutate(responseNode, "seed42");

        mutator.withRandomSeed(999L);
        List<JsonNode> result999 = mutator.mutate(responseNode, "seed999");
        mutator.close();

        Assert.assertEquals("Both seeds should produce same number of results",
                result42.size(), result999.size());
        Assert.assertNotEquals("Different seeds should produce different results",
                serializeList(result42),
                serializeList(result999));
    }

    /**
     * Specifically tests that header mutations are reproducible.
     * This was the key bug: HeaderMutator had an unseeded Random instance.
     */
    @Test
    public void testHeaderMutationsAreReproducible() throws Exception {
        String responseWithHeaders = "{\n" +
                "  \"Status Code\": 200,\n" +
                "  \"Headers\": {\"Content-Type\": \"application/json; charset=utf-8\", \"Location\": \"/api/test\"},\n" +
                "  \"Body\": {\"data\": \"value\"}\n" +
                "}";
        JsonNode responseNode = MAPPER.readTree(responseWithHeaders);

        List<String> run1 = collectMutantIds(responseNode, 777L);
        List<String> run2 = collectMutantIds(responseNode, 777L);

        // Filter only header-related mutants
        List<String> headerMutants1 = new ArrayList<>();
        List<String> headerMutants2 = new ArrayList<>();
        for (String id : run1) {
            if (id.startsWith("Headers/")) headerMutants1.add(id);
        }
        for (String id : run2) {
            if (id.startsWith("Headers/")) headerMutants2.add(id);
        }

        Assert.assertEquals("Header mutants count should match",
                headerMutants1.size(), headerMutants2.size());
        Assert.assertEquals("Header mutants should be identical across runs with same seed",
                headerMutants1, headerMutants2);
        Assert.assertFalse("There should be header mutants to test",
                headerMutants1.isEmpty());
    }

    /**
     * Tests multiple consecutive runs with different seeds all produce different results.
     */
    @Test
    public void testMultipleSeedsAllDifferent() throws Exception {
        JsonNode responseNode = MAPPER.readTree(SAMPLE_RESPONSE);

        List<String> previous = null;
        for (long seed = 1; seed <= 10; seed++) {
            List<String> current = collectMutantIds(responseNode, seed);
            if (previous != null) {
                Assert.assertNotEquals(
                        "Seed " + seed + " should differ from seed " + (seed - 1),
                        previous, current);
            }
            previous = current;
        }
    }

    private String serializeList(List<JsonNode> nodes) {
        StringBuilder sb = new StringBuilder();
        for (JsonNode n : nodes) {
            try {
                sb.append(MAPPER.writeValueAsString(n)).append("\n");
            } catch (Exception e) {
                sb.append("ERROR\n");
            }
        }
        return sb.toString();
    }
}
