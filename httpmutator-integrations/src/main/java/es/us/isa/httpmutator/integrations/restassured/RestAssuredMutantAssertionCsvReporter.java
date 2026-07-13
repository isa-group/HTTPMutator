package es.us.isa.httpmutator.integrations.restassured;

import es.us.isa.httpmutator.core.model.Mutant;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes one REST Assured assertion outcome row per mutant.
 */
public class RestAssuredMutantAssertionCsvReporter {

    public enum Outcome {
        SURVIVED,
        KILLED,
        CONVERSION_FAILED
    }

    public static final class Result {
        private final String requestId;
        private final int mutantIndex;
        private final String mutator;
        private final String operator;
        private final String originalJsonPath;
        private final boolean killed;
        private final Outcome outcome;
        private final String message;

        public Result(String requestId,
                      int mutantIndex,
                      String mutator,
                      String operator,
                      String originalJsonPath,
                      boolean killed,
                      Outcome outcome,
                      String message) {
            this.requestId = requestId;
            this.mutantIndex = mutantIndex;
            this.mutator = mutator;
            this.operator = operator;
            this.originalJsonPath = originalJsonPath;
            this.killed = killed;
            this.outcome = outcome;
            this.message = sanitizeMessage(message);
        }

        public String getRequestId() {
            return requestId;
        }

        public int getMutantIndex() {
            return mutantIndex;
        }

        public String getMutator() {
            return mutator;
        }

        public String getOperator() {
            return operator;
        }

        public String getOriginalJsonPath() {
            return originalJsonPath;
        }

        public boolean isKilled() {
            return killed;
        }

        public Outcome getOutcome() {
            return outcome;
        }

        public String getMessage() {
            return message;
        }
    }

    private final Path csvPath;
    private final List<Result> results = new ArrayList<Result>();

    public RestAssuredMutantAssertionCsvReporter(Path csvPath) {
        this.csvPath = csvPath;
    }

    public void record(String requestId,
                       int mutantIndex,
                       Mutant mutant,
                       boolean killed,
                       Outcome outcome,
                       String message) {
        String mutator = mutant == null ? "" : mutant.getMutatorClassName();
        String operator = mutant == null ? "" : mutant.getOperatorClassName();
        String originalJsonPath = mutant == null ? "" : mutant.getOriginalJsonPath();
        record(new Result(requestId, mutantIndex, mutator, operator, originalJsonPath, killed, outcome, message));
    }

    public void record(Result result) {
        if (result == null) {
            throw new IllegalArgumentException("result must not be null");
        }
        results.add(result);
    }

    public void write() throws IOException {
        Path parent = csvPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(csvPath, StandardCharsets.UTF_8)) {
            writer.write("requestId,mutantIndex,mutator,operator,originalJsonPath,killed,outcome,message");
            writer.newLine();

            for (Result result : results) {
                writer.write(String.join(",",
                        csv(result.getRequestId()),
                        String.valueOf(result.getMutantIndex()),
                        csv(result.getMutator()),
                        csv(result.getOperator()),
                        csv(result.getOriginalJsonPath()),
                        String.valueOf(result.isKilled()),
                        csv(result.getOutcome() == null ? "" : result.getOutcome().name()),
                        csv(result.getMessage())
                ));
                writer.newLine();
            }
        }
    }

    private static String sanitizeMessage(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\r', ' ').replace('\n', ' ');
    }

    private static String csv(String value) {
        if (value == null) {
            return "\"\"";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
