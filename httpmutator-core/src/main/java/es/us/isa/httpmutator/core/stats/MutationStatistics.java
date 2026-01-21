package es.us.isa.httpmutator.core.stats;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import es.us.isa.httpmutator.core.headers.HeaderMutator;
import es.us.isa.httpmutator.core.headers.charset.CharsetMutator;
import es.us.isa.httpmutator.core.headers.location.LocationMutator;
import es.us.isa.httpmutator.core.headers.mediaType.MediaTypeMutator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import es.us.isa.httpmutator.core.model.Mutant;

/**
 * Simple mutation statistics collector - Singleton pattern, memory-efficient
 * <p>
 * Tracks usage counts of mutator-operator combinations, doesn't store complete Mutant objects
 * to avoid memory explosion. Supports both per-test-ID grouping and global statistics.
 * <p>
 * Usage example:
 * <pre>
 *   MutationStatistics stats = MutationStatistics.getInstance();
 *   stats.record("test1", mutant);
 *   stats.recordBatch("test2", mutantsList);
 *   stats.exportGlobalCsv(Path.of("global-stats.csv"));
 *   stats.exportDetailedCsv(Path.of("detailed-stats.csv"));
 * </pre>
 */
public class MutationStatistics {
    private static final MutationStatistics INSTANCE = new MutationStatistics();

    // Global statistics: "MutatorName-OperatorName" -> count
    private final Map<String, Long> globalCombinationCounts = new LinkedHashMap<>();

    // Per-test statistics: testId -> ("MutatorName-OperatorName" -> count)
    private final Map<String, Map<String, Long>> combinationCountsByTest = new LinkedHashMap<>();

    public MutationStatistics() {}

    public static MutationStatistics getInstance() {
        return INSTANCE;
    }

    /**
     * Creates a combined key from mutator and operator class names
     */
    private String createCombinationKey(String mutatorName, String operatorName) {
        return mutatorName + "-" + operatorName;
    }

    /**
     * Records statistics for a single mutant
     */
    public void record(String testId, Mutant mutant) {
        if (testId == null || testId.trim().isEmpty()) {
            throw new IllegalArgumentException("testId cannot be null or empty");
        }
        if (mutant == null) {
            throw new NullPointerException("mutant cannot be null");
        }

        String mutantorName = mutant.getMutatorClassName();
        String jsonNodePath = mutant.getOriginalJsonPath();
        String operatorName = mutant.getOperatorClassName();
        if (mutantorName.equals(HeaderMutator.class.getSimpleName())) {
            if (jsonNodePath.equals("Headers/location")) {
                mutantorName = LocationMutator.class.getSimpleName();
            } else if (jsonNodePath.equals("Headers/content-type/mediaType")) {
                mutantorName = MediaTypeMutator.class.getSimpleName();
            } else if (jsonNodePath.equals("Headers/content-type/charset")) {
                mutantorName = CharsetMutator.class.getSimpleName();
            } else {
                throw new IllegalArgumentException("Unknown header path for HeaderMutator: " + jsonNodePath);
            }
        }
        String combinationKey = createCombinationKey(
                mutantorName,
                operatorName
        );

        // Update global counts
        globalCombinationCounts.merge(combinationKey, 1L, Long::sum);

        // Update per-test counts
        combinationCountsByTest.computeIfAbsent(testId, k -> new LinkedHashMap<>())
                .merge(combinationKey, 1L, Long::sum);
    }

    /**
     * Records a batch of mutants
     */
    public void recordBatch(String testId, List<Mutant> mutants) {
        if (mutants == null) {
            throw new NullPointerException("mutants cannot be null");
        }
        for (Mutant mutant : mutants) {
            record(testId, mutant);
        }
    }

    /**
     * Gets global mutator-operator combination statistics
     */
    public Map<String, Long> getGlobalCombinationCounts() {
        return new LinkedHashMap<>(globalCombinationCounts);
    }

    /**
     * Gets mutator-operator combination statistics for a specific test
     */
    public Map<String, Long> getCombinationCounts(String testId) {
        Map<String, Long> counts = combinationCountsByTest.get(testId);
        return counts == null ? new HashMap<>() : new LinkedHashMap<>(counts);
    }

    /**
     * Gets all test IDs
     */
    public java.util.Set<String> getAllTestIds() {
        return combinationCountsByTest.keySet();
    }

    /**
     * Gets total mutant count across all tests
     */
    public long getTotalMutantCount() {
        return globalCombinationCounts.values().stream()
                .mapToLong(Long::longValue)
                .sum();
    }

    /**
     * Gets total number of unique mutator-operator combinations
     */
    public int getUniqueCombinationCount() {
        return globalCombinationCounts.size();
    }

    /**
     * Exports global combination statistics to CSV
     * Format: mutatorName,operatorName,count
     */
    public void exportGlobalCsv(Path file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file);
             CSVPrinter printer = new CSVPrinter(writer,
                     CSVFormat.DEFAULT.withHeader("mutatorName", "operatorName", "count"))) {

            for (Map.Entry<String, Long> entry : globalCombinationCounts.entrySet()) {
                String combination = entry.getKey();
                Long count = entry.getValue();

                String[] parts = combination.split("-", 2);
                String mutatorName = parts[0];
                String operatorName = parts.length > 1 ? parts[1] : "";

                printer.printRecord(mutatorName, operatorName, count);
            }
        }
    }

    /**
     * Exports detailed statistics to CSV (grouped by test ID)
     * Format: testId,mutatorName,operatorName,count
     */
    public void exportDetailedCsv(Path file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file);
             CSVPrinter printer = new CSVPrinter(writer,
                     CSVFormat.DEFAULT.withHeader("testId", "mutatorName", "operatorName", "count"))) {

            for (String testId : getAllTestIds()) {
                Map<String, Long> combinationCounts = combinationCountsByTest.get(testId);

                for (Map.Entry<String, Long> entry : combinationCounts.entrySet()) {
                    String combination = entry.getKey();
                    Long count = entry.getValue();

                    String[] parts = combination.split("-", 2);
                    String mutatorName = parts[0];
                    String operatorName = parts.length > 1 ? parts[1] : "";

                    printer.printRecord(testId, mutatorName, operatorName, count);
                }
            }
        }
    }

    /**
     * Clears all statistics
     */
    public void clear() {
        globalCombinationCounts.clear();
        combinationCountsByTest.clear();
    }

    /**
     * Clears statistics for a specific test
     */
    public void clearTest(String testId) {
        combinationCountsByTest.remove(testId);
        // Note: Don't clear global statistics as they are cumulative
    }

    @Override
    public String toString() {
        return String.format("MutationStatistics{testCount=%d, totalMutants=%d, uniqueCombinations=%d}",
                getAllTestIds().size(),
                getTotalMutantCount(),
                getUniqueCombinationCount());
    }
}