# HttpMutator CLI

HttpMutator includes a small command-line interface in the `httpmutator-core` module. It reads HTTP exchanges from JSONL or HAR, mutates the responses, and writes mutated outputs to files.

## Build the CLI

The CLI is packaged as a fat JAR via the Maven assembly plugin.

```bash
mvn -pl httpmutator-core -am package
```

Artifact location:
- `httpmutator-core/target/httpmutator.jar`

## Usage

```bash
java -jar httpmutator-core/target/httpmutator.jar [options]
```

There are no subcommands; all flags are passed to the main entry point.

## Options

- `-i, --input <file>` (required, path) Input file containing HTTP exchanges (JSONL or HAR).
- `-f, --format <jsonl|har>` (optional) Input format. If omitted, the CLI infers it from the input file extension (`.jsonl`, `.har`, or `.har.json`).
- `-o, --output <dir>` (optional, default: `hm-output`) Output directory.
- `-s, --strategy <name>` (optional, default: `random`) Mutation strategy. Supported values: `random`, `exhaustive`, or `all` (alias for `exhaustive`).
- `--seed <long>` (optional, default: `42`) Random seed for the mutation strategy.
- `--includeMeta` (optional, flag) Include mutation metadata fields in JSONL output.
- `--writeJsonl` (optional, flag) Write JSONL output. If no output flags are provided, JSONL output is enabled by default.
- `--writeHar` (optional, flag) Write HAR output.
- `--reporter <name>` (optional, repeatable) Reporter name. Supported values: `csv`, `none`, `null`.
- `-h, --help` Show help and exit.

## Output files

Output files are created under the output directory, using the input filename (with the last extension removed) as the base name:

- JSONL: `<baseName>-mutants.jsonl`
- HAR: `<baseName>-mutants.har`
- CSV report (when `--reporter csv`): `<baseName>-report.csv`

If you pass both `--writeJsonl` and `--writeHar`, the CLI writes both outputs.

## Configuration

The CLI uses the library defaults from `httpmutator-core/src/main/resources/json-mutation.properties`. There are no CLI flags for overriding those properties in the current implementation.

## Examples

Minimal mutation run (JSONL input, default JSONL output):

```bash
java -jar httpmutator-core/target/httpmutator.jar \
  -i httpmutator-core/src/test/resources/httpmutatorInput.jsonl \
  -o hm-output
```

Write only HAR output:

```bash
java -jar httpmutator-core/target/httpmutator.jar \
  -i httpmutator-core/src/test/resources/httpmutatorInput.jsonl \
  -o hm-output \
  --writeHar
```

Write JSONL and HAR output with mutation metadata and a CSV report:

```bash
java -jar httpmutator-core/target/httpmutator.jar \
  -i httpmutator-core/src/test/resources/httpmutatorInput.jsonl \
  -o hm-output \
  -s all \
  --includeMeta \
  --writeJsonl \
  --writeHar \
  --reporter csv
```
