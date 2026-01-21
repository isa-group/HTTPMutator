# Output and Reporting

HttpMutator takes recorded HTTP responses, applies mutation operators, and produces mutated responses (mutants).
Outputs store these mutants in standard formats so they can be inspected, archived, or processed by other tools (for example, by running test assertions against each mutant to perform mutation testing).
Reporting summarizes mutation activity during generation and provides traceability across runs.

## Outputs

HttpMutator supports multiple output formats, including JSONL, HAR, and Zstd-sharded JSONL. The output layer is extensible via `MutantWriter`, which produces outputs in the selected format.
Additional output formats are added by implementing `MutantWriter` and registering the writer on HttpMutator via `addWriter(...)` or `withWriters(...)`.

- **JSONL** is the default output format for most HttpMutator workflows. It is designed for scalable, line-delimited pipelines and offline analysis.
- **HAR** is designed for compatibility with existing HAR-based tooling and preserves the standard HTTP Archive structure used by browsers and traffic analysis tools.
- **Zstd-sharded JSONL** is optimized for very large mutation corpora by compressing JSONL output and splitting it into multiple shards to reduce storage and disk I/O overhead.

A minimal example of enabling all three output formats:
```java
import com.fasterxml.jackson.databind.ObjectMapper;
import es.us.isa.httpmutator.core.HttpMutator;
import es.us.isa.httpmutator.core.model.StandardHttpResponse;
import es.us.isa.httpmutator.core.strategy.RandomSingleStrategy;
import es.us.isa.httpmutator.core.writer.HarMutantWriter;
import es.us.isa.httpmutator.core.writer.JsonlMutantWriter;
import es.us.isa.httpmutator.core.writer.ShardedZstdJsonlMutantWriter;

import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllWritersExample {
    public static void main(String[] args) throws Exception {
        // Build a small example response.
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> headers = new HashMap<>();
        headers.put("content-type", "application/json");
        StandardHttpResponse response = StandardHttpResponse.of(
                200,
                headers,
                mapper.readTree("{\"id\":1,\"name\":\"book\"}")
        );

        // Writers target files on disk.
        Writer jsonlOut = Files.newBufferedWriter(Paths.get("mutants.jsonl"));
        Writer harOut = Files.newBufferedWriter(Paths.get("mutants.har"));

        // Zstd writer writes sharded .jsonl.zst files to a directory.
        ShardedZstdJsonlMutantWriter zstdWriter =
                new ShardedZstdJsonlMutantWriter(Paths.get("zstd-out"), "mutants");

        // HttpMutator will call every writer for each selected mutant.
        try (HttpMutator mutator = new HttpMutator(1234L)
                .withMutationStrategy(new RandomSingleStrategy())
                .addWriter(new JsonlMutantWriter(jsonlOut, true)) // JSONL + metadata
                .addWriter(new HarMutantWriter(harOut))           // HAR output
                .addWriter(zstdWriter)) {                         // Compressed shards

            List<StandardHttpResponse> mutants = mutator.mutate(response);

            for (StandardHttpResponse mutated : mutants) {
                System.out.println(mutated.toJsonString());
            }
        }
    }
}

```

Writers take effect during mutation execution. For each mutant selected by the configured `MutationStrategy`, HttpMutator invokes every registered writer once and passes the original input identifier, the mutated response, and the mutation metadata. Writer outputs are finalized when HttpMutator completes.


## Reporting

Reporting provides run-level summaries of mutation activity during generation.
The reporting layer is extensible via `MutantReporter`. Reporters are registered on `HttpMutator` via `addReporter(...)` or `withReporters(...)`.
HttpMutator includes a built-in `CsvReporter` that summarizes operator usage per input response and in total, and writes the result as a single report.
The following example enables `CsvReporter` for HttpMutator.

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import es.us.isa.httpmutator.core.HttpMutator;
import es.us.isa.httpmutator.core.model.StandardHttpResponse;
import es.us.isa.httpmutator.core.reporter.CsvReporter;
import es.us.isa.httpmutator.core.strategy.RandomSingleStrategy;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportingExample {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> headers = new HashMap<>();
        headers.put("content-type", "application/json");

        StandardHttpResponse response = StandardHttpResponse.of(
                200,
                headers,
                mapper.readTree("{\"id\":1,\"name\":\"book\"}")
        );

        try (HttpMutator mutator = new HttpMutator(1234L)
                .withMutationStrategy(new RandomSingleStrategy())
                .addReporter(new CsvReporter(Paths.get("report.csv")))) {

            List<StandardHttpResponse> mutants = mutator.mutate(response);

            for (StandardHttpResponse mutated : mutants) {
                System.out.println(mutated.toJsonString());
            }
        }
    }
}
```
Reporters take effect during mutation execution. For each mutant selected by the configured `MutationStrategy`, HttpMutator invokes every registered reporter once and provides the original input identifier, the mutated response, and the mutation metadata. Report outputs are finalized when HttpMutator completes.
