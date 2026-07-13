package es.us.isa.httpmutator.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.us.isa.httpmutator.core.model.Mutant;
import es.us.isa.httpmutator.core.model.StandardHttpResponse;
import es.us.isa.httpmutator.core.strategy.AllOperatorsStrategy;
import es.us.isa.httpmutator.core.util.PropertyManager;
import es.us.isa.httpmutator.core.util.RandomUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class BodyPathIgnoreTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @After
    public void resetProperties() {
        PropertyManager.resetProperties();
        RandomUtils.setSeed(42L);
    }

    @Test
    public void ignoredBodyPathSkipsThatSubtreeOnly() throws Exception {
        StandardHttpResponse original = response("{\"user\":{\"id\":1,\"name\":\"Ada\"},\"status\":\"active\"}");
        HttpMutator mutator = new HttpMutator(42L)
                .withMutationStrategy(new AllOperatorsStrategy())
                .withIgnoredBodyPaths(Collections.singleton("Body/user"));

        List<String> paths = mutantPaths(mutator, original);

        Assert.assertFalse("Ignored subtree should not produce mutants",
                containsPathOrDescendant(paths, "Body/user"));
        Assert.assertTrue("Sibling body field should still produce mutants",
                paths.contains("Body/status"));
    }

    @Test
    public void ignoredLeafPathDoesNotSkipSiblings() throws Exception {
        StandardHttpResponse original = response("{\"items\":[{\"id\":1,\"name\":\"alpha\"}],\"other\":true}");
        HttpMutator mutator = new HttpMutator(42L)
                .withMutationStrategy(new AllOperatorsStrategy())
                .addIgnoredBodyPath("/items/0/id");

        List<String> paths = mutantPaths(mutator, original);

        Assert.assertFalse("Ignored leaf should not produce mutants",
                containsPathOrDescendant(paths, "Body/items/0/id"));
        Assert.assertTrue("Sibling leaf should still produce mutants",
                paths.contains("Body/items/0/name"));
    }

    @Test
    public void ignoredBodyRootSkipsOnlyBodyMutants() throws Exception {
        StandardHttpResponse original = response("{\"id\":1,\"name\":\"Ada\"}");
        HttpMutator mutator = new HttpMutator(42L)
                .withMutationStrategy(new AllOperatorsStrategy())
                .addIgnoredBodyPath("Body");

        List<String> paths = mutantPaths(mutator, original);

        Assert.assertFalse("No body mutants should be produced",
                paths.stream().anyMatch(path -> path.equals("Body") || path.startsWith("Body/")));
        Assert.assertTrue("Status code mutants should still be produced",
                paths.contains("Status Code"));
    }

    @Test
    public void ignoredBodyPathsAreNormalized() {
        HttpMutator mutator = new HttpMutator(42L)
                .withIgnoredBodyPaths(Arrays.asList("Body/user", "/token", "items/0/id", "  ", null));

        Assert.assertEquals(Arrays.asList("Body/user", "Body/token", "Body/items/0/id"),
                new ArrayList<String>(mutator.getIgnoredBodyPaths()));
    }

    @Test
    public void propertiesFileConfiguresIgnoredBodyPaths() throws Exception {
        Path override = temporaryFolder.newFile("ignore.properties").toPath();
        Files.write(override,
                "mutation.body.ignore.paths=/user\n".getBytes(StandardCharsets.UTF_8));
        StandardHttpResponse original = response("{\"user\":{\"id\":1},\"status\":\"active\"}");
        HttpMutator mutator = new HttpMutator(42L, override)
                .withMutationStrategy(new AllOperatorsStrategy());

        List<String> paths = mutantPaths(mutator, original);

        Assert.assertEquals(Collections.singletonList("Body/user"),
                new ArrayList<String>(mutator.getIgnoredBodyPaths()));
        Assert.assertFalse("Ignored path from properties should be applied",
                containsPathOrDescendant(paths, "Body/user"));
        Assert.assertTrue("Unignored sibling should still be mutated",
                paths.contains("Body/status"));
    }

    @Test
    public void javaApiCanOverrideAndAppendPropertiesIgnoredBodyPaths() throws Exception {
        Path override = temporaryFolder.newFile("ignore.properties").toPath();
        Files.write(override,
                "mutation.body.ignore.paths=/user\n".getBytes(StandardCharsets.UTF_8));
        HttpMutator mutator = new HttpMutator(42L, override)
                .withIgnoredBodyPaths(Collections.singleton("status"))
                .addIgnoredBodyPath("items/0/id");

        Assert.assertEquals(Arrays.asList("Body/status", "Body/items/0/id"),
                new ArrayList<String>(mutator.getIgnoredBodyPaths()));
    }

    private static StandardHttpResponse response(String bodyJson) throws Exception {
        JsonNode body = MAPPER.readTree(bodyJson);
        return StandardHttpResponse.of(200, new HashMap<String, Object>(), body);
    }

    private static List<String> mutantPaths(HttpMutator mutator, StandardHttpResponse original) throws Exception {
        final List<String> paths = new ArrayList<String>();
        mutator.mutate(original, "body-ignore-test", (mutated, mutant) -> paths.add(mutant.getOriginalJsonPath()));
        mutator.close();
        return paths;
    }

    private static boolean containsPathOrDescendant(List<String> paths, String ignoredPath) {
        for (String path : paths) {
            if (path.equals(ignoredPath) || path.startsWith(ignoredPath + "/")) {
                return true;
            }
        }
        return false;
    }
}
