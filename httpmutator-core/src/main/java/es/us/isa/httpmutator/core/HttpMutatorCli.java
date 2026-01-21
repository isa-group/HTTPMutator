package es.us.isa.httpmutator.core;

import es.us.isa.httpmutator.core.reader.HarExchangeReader;
import es.us.isa.httpmutator.core.reader.HttpExchangeReader;
import es.us.isa.httpmutator.core.reader.JsonlExchangeReader;
import es.us.isa.httpmutator.core.reporter.CsvReporter;
import es.us.isa.httpmutator.core.reporter.MutantReporter;
import es.us.isa.httpmutator.core.strategy.AllOperatorsStrategy;
import es.us.isa.httpmutator.core.strategy.MutationStrategy;
import es.us.isa.httpmutator.core.strategy.RandomSingleStrategy;
import es.us.isa.httpmutator.core.writer.HarMutantWriter;
import es.us.isa.httpmutator.core.writer.JsonlMutantWriter;
import es.us.isa.httpmutator.core.writer.MutantWriter;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple CLI entry point for HttpMutator.
 *
 * Responsibilities:
 * - Parse basic command-line arguments
 * - Open input file as Reader
 * - Create appropriate HttpExchangeReader based on input format
 * - Create MutantWriter(s) that write to files under an output directory
 * - Wire everything into a configured HttpMutator and run mutateStream(...)
 *
 * This class intentionally keeps the CLI logic separate from the core mutation
 * engine, so that the core API remains reusable from tests, libraries, and
 * other tools.
 */
public final class HttpMutatorCli {

    private HttpMutatorCli() {
        // utility class
    }

    public static void main(String[] args) {
        try {
            CliConfig config = CliConfig.parse(args);
            run(config);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            printUsage();
            System.exit(1);
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(2);
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(99);
        }
    }

    // ============================================================
    // CLI main logic
    // ============================================================

    private static void run(CliConfig config) throws IOException {
        // 1) Prepare input Reader
        Path input = config.inputFile;
        if (!Files.exists(input)) {
            throw new IllegalArgumentException("Input file does not exist: " + input);
        }

        Files.createDirectories(config.outputDir);

        HttpExchangeReader exchangeReader = createExchangeReader(config);
        List<MutantWriter> writers = createWriters(config);
        List<MutantReporter> reporters = createReporters(config);

        MutationStrategy strategy = createStrategy(config);

        try (Reader in = Files.newBufferedReader(input, StandardCharsets.UTF_8);
             HttpMutator mutator = new HttpMutator(config.randomSeed)
                     .withMutationStrategy(strategy)
                     .withWriters(writers)
                     .withReporters(reporters)) {

            mutator.mutateStream(exchangeReader, in);
        }
    }

    // ============================================================
    // Factory helpers
    // ============================================================

    private static HttpExchangeReader createExchangeReader(CliConfig config) {
        switch (config.format) {
            case JSONL:
                return new JsonlExchangeReader();
            case HAR:
                return new HarExchangeReader();
            default:
                throw new IllegalArgumentException("Unsupported format: " + config.format);
        }
    }

    private static MutationStrategy createStrategy(CliConfig config) {
        switch (config.strategy) {
            case EXHAUSTIVE:
                return new AllOperatorsStrategy();

            case RANDOM:
                return new RandomSingleStrategy();

            default:
                throw new IllegalArgumentException("Unsupported strategy: " + config.strategy);
        }
    }

    /**
     * Output selection rules:
     * - If neither --writeHar nor --writeJsonl is provided -> default JSONL output.
     * - If only --writeHar is provided -> only HAR output.
     * - If only --writeJsonl is provided -> only JSONL output.
     * - If both are provided -> output both.
     */
    private static List<MutantWriter> createWriters(CliConfig config) throws IOException {
        List<MutantWriter> writers = new ArrayList<>();

        boolean anySpecified = config.writeHar || config.writeJsonl;
        boolean writeJsonl = !anySpecified || config.writeJsonl;
        boolean writeHar = config.writeHar;

        if (writeJsonl) {
            Path jsonlOut = config.outputDir.resolve(config.baseName + "-mutants.jsonl");
            Writer jsonlWriter = Files.newBufferedWriter(
                    jsonlOut,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
            writers.add(new JsonlMutantWriter(jsonlWriter, config.includeMeta));
        }

        if (writeHar) {
            Path harOut = config.outputDir.resolve(config.baseName + "-mutants.har");
            Writer harWriter = Files.newBufferedWriter(
                    harOut,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
            writers.add(new HarMutantWriter(harWriter));
        }

        return writers;
    }

    private static List<MutantReporter> createReporters(CliConfig config) {
        List<MutantReporter> reporters = new ArrayList<>();

        if (config.reporterNames == null || config.reporterNames.isEmpty()) {
            return reporters; // no reporters by default
        }

        for (String name : config.reporterNames) {
            switch (name) {
                case "none":
                case "null":
                    break;

                case "csv": {
                    Path out = config.outputDir.resolve(config.baseName + "-report.csv");
                    reporters.add(new CsvReporter(out));
                    break;
                }

                default:
                    throw new IllegalArgumentException("Unknown reporter: " + name);
            }
        }

        return reporters;
    }

    // ============================================================
    // CLI config & argument parsing
    // ============================================================

    private enum InputFormat {
        JSONL,
        HAR
    }

    // strategies exposed by CLI
    private enum StrategyName {
        RANDOM,
        EXHAUSTIVE
    }

    private static final class CliConfig {
        final Path inputFile;
        final InputFormat format;
        final Path outputDir;
        final String baseName;
        final boolean includeMeta;
        final long randomSeed;
        final List<String> reporterNames;

        final StrategyName strategy;

        // Output toggles
        final boolean writeHar;
        final boolean writeJsonl;

        private CliConfig(Path inputFile,
                          InputFormat format,
                          Path outputDir,
                          String baseName,
                          boolean includeMeta,
                          long randomSeed,
                          List<String> reporterNames,
                          StrategyName strategy,
                          boolean writeHar,
                          boolean writeJsonl) {
            this.inputFile = inputFile;
            this.format = format;
            this.outputDir = outputDir;
            this.baseName = baseName;
            this.includeMeta = includeMeta;
            this.randomSeed = randomSeed;
            this.reporterNames = reporterNames;
            this.strategy = strategy;
            this.writeHar = writeHar;
            this.writeJsonl = writeJsonl;
        }

        static CliConfig parse(String[] args) {
            if (args == null || args.length == 0) {
                throw new IllegalArgumentException("No arguments provided");
            }

            Path input = null;
            InputFormat format = null;
            Path outputDir = Paths.get("hm-output");
            String baseName = null;
            List<String> reporterNames = new ArrayList<>();
            boolean includeMeta = false;
            long randomSeed = 42L;
            StrategyName strategy = StrategyName.RANDOM;

            // Output flags (default selection implemented in createWriters)
            boolean writeHar = false;
            boolean writeJsonl = false;

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--input":
                    case "-i":
                        if (i + 1 >= args.length) {
                            throw new IllegalArgumentException("--input requires a file path");
                        }
                        input = Paths.get(args[++i]);
                        if (baseName == null) {
                            baseName = stripExtension(input.getFileName().toString());
                        }
                        break;

                    case "--format":
                    case "-f":
                        if (i + 1 >= args.length) {
                            throw new IllegalArgumentException("--format requires 'jsonl' or 'har'");
                        }
                        String fmt = args[++i].trim().toLowerCase();
                        if ("jsonl".equals(fmt)) {
                            format = InputFormat.JSONL;
                        } else if ("har".equals(fmt)) {
                            format = InputFormat.HAR;
                        } else {
                            throw new IllegalArgumentException("Unknown format: " + fmt);
                        }
                        break;

                    case "--output":
                    case "-o":
                        if (i + 1 >= args.length) {
                            throw new IllegalArgumentException("--output requires a directory path");
                        }
                        outputDir = Paths.get(args[++i]);
                        break;

                    case "--seed":
                        if (i + 1 >= args.length) {
                            throw new IllegalArgumentException("--seed requires a long value");
                        }
                        randomSeed = Long.parseLong(args[++i]);
                        break;

                    case "--strategy":
                    case "-s":
                        if (i + 1 >= args.length) {
                            throw new IllegalArgumentException("--strategy requires a name");
                        }
                        strategy = parseStrategy(args[++i]);
                        break;

                    case "--includeMeta":
                        includeMeta = true;
                        break;

                    case "--reporter":
                        if (i + 1 >= args.length) {
                            throw new IllegalArgumentException("--reporter requires a name");
                        }
                        reporterNames.add(args[++i].toLowerCase());
                        break;

                    // Output toggles
                    case "--writeHar":
                        writeHar = true;
                        break;

                    case "--writeJsonl":
                        writeJsonl = true;
                        break;

                    case "--help":
                    case "-h":
                        printUsage();
                        System.exit(0);
                        break;

                    default:
                        throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }

            if (input == null) {
                throw new IllegalArgumentException("Missing required --input argument");
            }
            if (format == null) {
                String name = input.getFileName().toString().toLowerCase();
                if (name.endsWith(".jsonl")) {
                    format = InputFormat.JSONL;
                } else if (name.endsWith(".har") || name.endsWith(".har.json")) {
                    format = InputFormat.HAR;
                } else {
                    throw new IllegalArgumentException(
                            "Cannot infer format from file extension, please specify --format jsonl|har");
                }
            }
            if (baseName == null) {
                baseName = "mutants";
            }

            return new CliConfig(
                    input, format, outputDir, baseName,
                    includeMeta, randomSeed, reporterNames, strategy,
                    writeHar, writeJsonl
            );
        }

        private static StrategyName parseStrategy(String raw) {
            String v = raw == null ? "" : raw.trim().toLowerCase();
            switch (v) {
                case "exhaustive":
                case "all":
                    return StrategyName.EXHAUSTIVE;
                case "random":
                    return StrategyName.RANDOM;
                default:
                    throw new IllegalArgumentException("Unknown strategy: " + raw + " (supported: all, random)");
            }
        }

        private static String stripExtension(String fileName) {
            int dot = fileName.lastIndexOf('.');
            if (dot <= 0) {
                return fileName;
            }
            return fileName.substring(0, dot);
        }
    }

    private static void printUsage() {
        System.err.println("Usage: java -jar httpmutator.jar [options]");
        System.err.println();
        System.err.println("Required:");
        System.err.println("  -i, --input <file>        Input file (JSONL or HAR)");
        System.err.println();
        System.err.println("Optional:");
        System.err.println("  -f, --format <fmt>        Input format: jsonl | har");
        System.err.println("  -o, --output <dir>        Output directory (default: hm-output)");
        System.err.println("  -s, --strategy <name>     Mutation strategy (default: random)");
        System.err.println("        Supported: exhaustive(all), random");
        System.err.println("      --includeMeta         Include mutation metadata fields in JSONL output");
        System.err.println("      --seed <long>         Random seed (default: 42)");
        System.err.println("      --writeJsonl          Write JSONL output (default if no output flags are specified)");
        System.err.println("      --writeHar            Write HAR output");
        System.err.println("  -h, --help                Show this help and exit");
        System.err.println();
        System.err.println("Reporters:");
        System.err.println("  --reporter csv            Per-request operator counts");
        System.err.println("  --reporter none           Disable reporters");
        System.err.println();
        System.err.println("Examples:");
        System.err.println("  # Default: JSONL output");
        System.err.println("  java -jar httpmutator.jar -i traffic.jsonl -f jsonl -o out -s random");
        System.err.println();
        System.err.println("  # Only HAR output");
        System.err.println("  java -jar httpmutator.jar -i traffic.jsonl -f jsonl -o out -s all --writeHar");
        System.err.println();
        System.err.println("  # Both JSONL + HAR output");
        System.err.println("  java -jar httpmutator.jar -i traffic.jsonl -f jsonl -o out -s all --writeJsonl --writeHar");
    }
}
