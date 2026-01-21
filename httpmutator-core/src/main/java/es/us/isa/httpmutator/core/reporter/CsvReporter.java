package es.us.isa.httpmutator.core.reporter;

import es.us.isa.httpmutator.core.model.HttpExchange;
import es.us.isa.httpmutator.core.model.Mutant;
import es.us.isa.httpmutator.core.model.StandardHttpResponse;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Produces a single CSV containing one row per request-exchange,
 * and one final TOTAL row summarizing all operator usage.
 *
 * CSV structure:
 *
 * id,<mutator:operator>,<mutator:operator>,...
 * ex-1,3,0,1,5
 * ex-2,0,2,0,0
 * TOTAL,3,2,1,5
 */
public class CsvReporter implements MutantReporter {

    /** All distinct operator keys observed globally (mutator:operator). */
    private final Set<String> allOperators = ConcurrentHashMap.newKeySet();

    /** Per ID → (operator → count). */
    private final Map<String, Map<String, Integer>> perIdCounts = new ConcurrentHashMap<>();

    private final Path outputFile;

    public CsvReporter(Path outputFile) {
        this.outputFile = outputFile;
    }

    @Override
    public void onMutant(
            HttpExchange exchange,
            StandardHttpResponse mutated,
            Mutant mutant) {

        String id = exchange.getId();

        String key = mutant.getMutatorClassName() + ":" + mutant.getOperatorClassName();

        allOperators.add(key);

        perIdCounts
                .computeIfAbsent(id, k -> new ConcurrentHashMap<>())
                .merge(key, 1, Integer::sum);
    }

    @Override
    public void onFinished() throws IOException {

        // Sort operator columns alphabetically for stable output
        List<String> sortedOperators = new ArrayList<>(allOperators);
        Collections.sort(sortedOperators);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile.toFile()))) {

            // ----------------------------------------------------
            // 1) Write header
            // ----------------------------------------------------
            bw.write("id");
            for (String op : sortedOperators) {
                bw.write("," + op);
            }
            bw.write("\n");

            // ----------------------------------------------------
            // 2) Write one row per request ID
            // ----------------------------------------------------
            for (Map.Entry<String, Map<String, Integer>> e : perIdCounts.entrySet()) {
                String id = e.getKey();
                Map<String, Integer> opCounts = e.getValue();

                bw.write(id);

                for (String op : sortedOperators) {
                    int count = opCounts.getOrDefault(op, 0);
                    bw.write("," + count);
                }
                bw.write("\n");
            }

            // ----------------------------------------------------
            // 3) Write TOTAL row
            // ----------------------------------------------------
            bw.write("TOTAL");

            for (String op : sortedOperators) {
                int total = 0;
                for (Map<String, Integer> perId : perIdCounts.values()) {
                    total += perId.getOrDefault(op, 0);
                }
                bw.write("," + total);
            }
            bw.write("\n");
        }
    }
}