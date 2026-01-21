package es.us.isa.httpmutator.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import es.us.isa.httpmutator.core.body.BodyMutator;
import es.us.isa.httpmutator.core.body.value.boolean0.BooleanMutator;
import es.us.isa.httpmutator.core.body.value.double0.DoubleMutator;
import es.us.isa.httpmutator.core.body.value.long0.LongMutator;
import es.us.isa.httpmutator.core.body.value.null0.NullMutator;
import es.us.isa.httpmutator.core.body.value.string0.StringMutator;
import es.us.isa.httpmutator.core.headers.HeaderMutator;
import es.us.isa.httpmutator.core.model.Mutant;
import es.us.isa.httpmutator.core.model.MutantGroup;
import es.us.isa.httpmutator.core.sc.StatusCodeMutator;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Reads JSONL test cases from /httpmutatorInput.jsonl (one JSON per line).
 * For each case:
 *  - Runs HttpMutator#getAllMutants(...)
 *  - Enumerates expected JSON node paths (WITH array indices)
 *  - Asserts each expected path has at least one mutant
 *  - Writes JSONL report: target/test-output/mutants-per-path.jsonl
 * Prints a unified report:
 *   - expected ∧ produced: "path"
 *   - expected ∧ !produced: "path [MISSING]"
 *   - produced ∧ !expected: "path [Unexpected]"
 */
public class HttpMutatorEngineTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String RESOURCE_PATH = "/httpmutatorInput.jsonl";

    // Where to write JSONL output
    private static final Path OUTPUT_DIR  = Paths.get("target", "test-output");
    private static final Path OUTPUT_FILE = OUTPUT_DIR.resolve("mutants-per-path.jsonl");

    @Test
    public void testEachJsonlCaseAllPathsHaveMutants() throws Exception {
        // Prepare output file
        Files.createDirectories(OUTPUT_DIR);
        if (Files.exists(OUTPUT_FILE)) {
            Files.delete(OUTPUT_FILE);
        }
        BufferedWriter jsonl = Files.newBufferedWriter(OUTPUT_FILE, StandardCharsets.UTF_8);

        try {
            List<String> lines = readLines(RESOURCE_PATH);
            Assert.assertFalse("Input JSONL is empty: " + RESOURCE_PATH, lines.isEmpty());

            int caseIndex = 0;
            for (String line : lines) {
                caseIndex++;
                if (line == null || line.trim().isEmpty()) continue;

                JsonNode root = MAPPER.readTree(line);

                // Force-enable all mutators (ignore external props)
                HttpMutatorEngine hm = new HttpMutatorEngine();
                forceEnableAllMutators(hm);

                // Collect full mutants (not just paths)
                final List<Mutant> allMutants = new ArrayList<Mutant>();
                hm.getAllMutants(root, new java.util.function.Consumer<MutantGroup>() {
                    @Override
                    public void accept(MutantGroup mg) {
                        allMutants.addAll(mg.getMutants());
                    }
                });

                // Build set of produced paths (normalized)
                final Set<String> mutatedPaths = new HashSet<String>();
                for (Mutant m : allMutants) {
                    String p = safePath(m.getOriginalJsonPath());
                    mutatedPaths.add(trimTrailingSlash(p));
                }

                // Enumerate expected paths (WITH array indices)
                final Set<String> expectedPaths = listAllNodePaths(root);

                // Unified one-line-per-path report to console
                printUnifiedPathReport(caseIndex, line, expectedPaths, mutatedPaths);

                // Write JSONL: one line per path (expected ∪ produced)
                writeJsonlPerPath(jsonl, caseIndex, expectedPaths, mutatedPaths, allMutants);

                // Assert all expected are covered (produced path equals/starts-with expected)
                Assert.assertTrue("Case #" + caseIndex + " has missing paths",
                        allExpectedCovered(expectedPaths, mutatedPaths));
            }
        } finally {
            jsonl.flush();
            jsonl.close();
            System.out.println("JSONL written to: " + OUTPUT_FILE.toAbsolutePath());
        }
    }

    @Test
    public void testEmptyHeadersHandledGracefully() throws Exception {
        // Construct: Status Code and Body present, Headers is an empty object {}
        String json = "{\n" +
                "  \"Status Code\": 200,\n" +
                "  \"Headers\": {},\n" +
                "  \"Body\": {\"a\": 1}\n" +
                "}";
        JsonNode root = MAPPER.readTree(json);

        // List expected paths (with indices)
        Set<String> expectedPaths = listAllNodePaths(root);

        // Assert: no Headers/* paths
        for (String p : expectedPaths) {
            Assert.assertFalse("Should not contain header path when Headers is empty: " + p,
                    p.startsWith("Headers/"));
        }

        // Assert: other key paths still exist
        Assert.assertTrue(expectedPaths.contains("Status Code"));
        // Body is an object: collectPathsWithIndices includes the container and leaves
        Assert.assertTrue(expectedPaths.contains("Body"));
        Assert.assertTrue(expectedPaths.contains("Body/a"));

        // Force-enable all mutators
        HttpMutatorEngine hm = new HttpMutatorEngine();
        forceEnableAllMutators(hm);

        // Collect all mutants
        final List<Mutant> allMutants = new ArrayList<>();
        hm.getAllMutants(root, new java.util.function.Consumer<MutantGroup>() {
            @Override
            public void accept(MutantGroup mg) {
                allMutants.addAll(mg.getMutants());
            }
        });

        // Produced path set (normalized)
        final Set<String> mutatedPaths = new HashSet<>();
        for (Mutant m : allMutants) {
            mutatedPaths.add(trimTrailingSlash(safePath(m.getOriginalJsonPath())));
        }

        // Assert: no mutants should be produced for Headers/*
        for (String mp : mutatedPaths) {
            Assert.assertFalse("No header mutants expected when Headers is empty: " + mp,
                    mp.startsWith("Headers/"));
        }

        // Assert: non-header expected paths should be covered (equal or prefix)
        // Filter expected to remove Headers-related paths (none in this case)
        Set<String> expectedNonHeader = new HashSet<>();
        for (String p : expectedPaths) {
            if (!p.startsWith("Headers/")) expectedNonHeader.add(p);
        }
        Assert.assertTrue("Non-header expected paths should be covered",
                allExpectedCovered(expectedNonHeader, mutatedPaths));
    }

    @Test
    public void testNullBodyHandledGracefully() throws Exception {
        String json = "{\n" +
                "  \"Status Code\": 200,\n" +
                "  \"Headers\": {},\n" +
                "  \"Body\": null\n" +
                "}";
        JsonNode root = MAPPER.readTree(json);

        // Force-enable all mutators
        HttpMutatorEngine hm = new HttpMutatorEngine();

        final List<Mutant> allMutants = new ArrayList<>();
        hm.getAllMutants(root, new java.util.function.Consumer<MutantGroup>() {
            @Override
            public void accept(MutantGroup mg) {
                allMutants.addAll(mg.getMutants());
            }
        });

    }


    /* ====================== JSONL writer ====================== */

    /**
     * For each path in (expected ∪ produced), write one JSON line:
     * {
     *   "caseIndex": 1,
     *   "path": "Body/...",
     *   "expected": true|false,
     *   "produced": true|false,
     *   "missing": true|false,
     *   "mutantCount": N,
     *   "mutants": [
     *     {
     *       "mutatorClass": "...",
     *       "operatorClass": "...",
     *       "originalPath": "...",
     *       "mutatedNode": { ... } // or primitive/null
     *     },
     *     ...
     *   ]
     * }
     */
    private static void writeJsonlPerPath(
            BufferedWriter jsonl,
            int caseIndex,
            Set<String> expectedPaths,
            Set<String> mutatedPaths,
            List<Mutant> allMutants) throws Exception {

        // Union (sorted deterministically)
        SortedSet<String> union = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        union.addAll(expectedPaths);
        union.addAll(mutatedPaths);

        for (String path : union) {
            boolean expected = expectedPaths.contains(path);
            boolean produced = containsProduced(mutatedPaths, path);
            boolean missing  = expected && !produced;

            // Collect mutants that belong to this path (prefix match)
            List<Map<String, Object>> mutantsForPath = new ArrayList<Map<String, Object>>();
            for (Mutant m : allMutants) {
                String mp = trimTrailingSlash(safePath(m.getOriginalJsonPath()));
                if (mp.equals(path) || mp.startsWith(path + "/")) {
                    Map<String, Object> one = new LinkedHashMap<String, Object>();
                    one.put("mutatorClass", m.getMutatorClass() == null ? null : m.getMutatorClass().getName());
                    one.put("operatorClass", m.getOperatorClass() == null ? null : m.getOperatorClass().getName());
                    one.put("originalPath", mp);
                    // serialize mutated node compactly
                    JsonNode node = m.getMutatedNode();
                    if (node == null || node instanceof NullNode) {
                        one.put("mutatedNode", null);
                    } else {
                        // keep as arbitrary JSON (stringify then parse to generic Object for stable JSONL)
                        String json = MAPPER.writeValueAsString(node);
                        Object asObj = MAPPER.readValue(json, Object.class);
                        one.put("mutatedNode", asObj);
                    }
                    mutantsForPath.add(one);
                }
            }

            Map<String, Object> line = new LinkedHashMap<String, Object>();
            line.put("caseIndex", caseIndex);
            line.put("path", path);
            line.put("expected", expected);
            line.put("produced", produced);
            line.put("missing", missing);
            line.put("mutantCount", mutantsForPath.size());
            line.put("mutants", mutantsForPath);

            // Write one JSON line
            jsonl.write(MAPPER.writeValueAsString(line));
            jsonl.write("\n");
        }
    }

    /* ====================== Unified report helpers ====================== */

    private static void printUnifiedPathReport(
            int caseIndex,
            String inputJson,
            Set<String> expectedPaths,
            Set<String> mutatedPaths) {

        SortedSet<String> union = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        union.addAll(expectedPaths);
        union.addAll(mutatedPaths);

        System.out.println("\n=== Case #" + caseIndex + " unified path report ===");
        System.out.println("Input JSON: " + inputJson);
        System.out.println("Paths (" + union.size() + "):");

        for (String p : union) {
            boolean isExpected = expectedPaths.contains(p);
            boolean isProduced = containsProduced(mutatedPaths, p);
            if (isExpected && isProduced) {
                System.out.println("  " + p);
            } else if (isExpected) {
                System.out.println("  " + p + " [MISSING]");
            } else {
                System.out.println("  " + p + " [Unexpected]");
            }
        }
    }

    /** A produced path covers an expected path if equal or deeper with the expected as prefix. */
    private static boolean containsProduced(Set<String> mutated, String expectedPrefix) {
        String exp = trimTrailingSlash(expectedPrefix);
        for (String mp : mutated) {
            String m = trimTrailingSlash(mp);
            if (m.equals(exp) || m.startsWith(exp + "/")) return true;
        }
        return false;
    }

    private static boolean allExpectedCovered(Set<String> expected, Set<String> mutated) {
        for (String p : expected) {
            if (!containsProduced(mutated, p)) return false;
        }
        return true;
    }

    private static String trimTrailingSlash(String s) {
        if (s == null || s.isEmpty()) return "";
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }

    private static String safePath(String p) {
        return p == null ? "" : p;
    }

    /* ====================== I/O & reflection helpers ====================== */

    private static List<String> readLines(String resourcePath) throws Exception {
        InputStream in = HttpMutatorEngineTest.class.getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IllegalStateException("Resource not found: " + resourcePath);
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        List<String> lines = new ArrayList<String>();
        String s;
        while ((s = br.readLine()) != null) {
            lines.add(s);
        }
        br.close();
        return lines;
    }

    private static void forceEnableAllMutators(HttpMutatorEngine hm) throws Exception {
        setPrivate(hm, "statusCodeMutator", new StatusCodeMutator());
        setPrivate(hm, "headerMutator",     new HeaderMutator());
        setPrivate(hm, "bodyMutator",       new BodyMutator());

        setPrivate(hm, "booleanMutator", new BooleanMutator());
        setPrivate(hm, "doubleMutator",  new DoubleMutator());
        setPrivate(hm, "longMutator",    new LongMutator());
        setPrivate(hm, "stringMutator",  new StringMutator());
        setPrivate(hm, "nullMutator",    new NullMutator());
    }

    private static void setPrivate(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    /* ====================== Expected path enumeration (WITH indices) ====================== */

    /**
     * Enumerate JSON node paths expected to produce mutants (WITH array indices):
     * - "Status Code"
     * - "Headers/<key>" for each header key
     * - For "Body":
     *   * If null or scalar: "Body"
     *   * If object: recurse into fields
     *   * If array: include container path AND each element as "<path>/<index>" and recurse
     */
    public static Set<String> listAllNodePaths(JsonNode root) {
        Set<String> paths = new LinkedHashSet<String>();

        // Status Code
        if (root.has("Status Code")) {
            paths.add("Status Code");
        }

        // Headers
        if (root.has("Headers") && root.get("Headers").isObject()) {
            JsonNode headers = root.get("Headers");
            Iterator<String> it = headers.fieldNames();
            while (it.hasNext()) {
                String key = it.next();
                paths.add("Headers/" + key);
            }
        }

        // Body
        if (root.has("Body")) {
            JsonNode body = root.get("Body");
            if (body == null || body.isNull() || !body.isContainerNode()) {
                paths.add("Body");
            } else {
                collectPathsWithIndices("Body", body, paths);
            }
        }

        return paths;
    }

    /**
     * Recursively collect paths with array indices:
     * - Objects: add leaf paths, recurse into containers
     * - Arrays: add container path and each "<index>" element, recurse where needed
     */
    private static void collectPathsWithIndices(String prefix, JsonNode node, Set<String> out) {
        if (node == null) {
            out.add(prefix);
            return;
        }

        if (node.isObject()) {
            out.add(prefix); // include the object node itself (useful for container-level mutants)
            Iterator<String> it = node.fieldNames();
            while (it.hasNext()) {
                String key = it.next();
                JsonNode child = node.get(key);
                String p = prefix + "/" + key;

                if (child == null || child.isNull()) {
                    out.add(p);
                } else if (child.isContainerNode()) {
                    if (child.isArray()) {
                        out.add(p); // array container
                        for (int i = 0; i < child.size(); i++) {
                            JsonNode elem = child.get(i);
                            String idxPath = p + "/" + i;
                            out.add(idxPath); // element node itself
                            if (elem == null || elem.isNull()) {
                                // leaf null
                            } else if (elem.isContainerNode()) {
                                collectPathsWithIndices(idxPath, elem, out);
                            } else {
                                // scalar element -> idxPath is the leaf
                            }
                        }
                    } else {
                        // nested object
                        collectPathsWithIndices(p, child, out);
                    }
                } else {
                    // leaf scalar
                    out.add(p);
                }
            }
        } else if (node.isArray()) {
            out.add(prefix); // array container
            for (int i = 0; i < node.size(); i++) {
                JsonNode elem = node.get(i);
                String idxPath = prefix + "/" + i;
                out.add(idxPath);
                if (elem == null || elem.isNull()) {
                    // leaf null
                } else if (elem.isContainerNode()) {
                    collectPathsWithIndices(idxPath, elem, out);
                } else {
                    // scalar element -> idxPath is the leaf
                }
            }
        } else {
            // scalar
            out.add(prefix);
        }
    }
}
