# Operator Configuration

HttpMutator loads its default mutation settings from `http-mutation.properties`. Category switches such as `operator.sc.enabled` can disable an entire response component, while the switches below control individual operators. A custom properties file can be passed through the Java API or CLI and is loaded as an override on top of the default file. A missing individual operator switch defaults to enabled for backward compatibility. Explicit values must be `true` or `false`.

Random seed and mutation strategy are intentionally separate from this file:

- Java API: `new HttpMutator(seed)` and `withMutationStrategy(...)`
- CLI: `--seed` and `--strategy random|exhaustive`

## Operator switches

| Paper operator | Implementation scope | Enabled property |
| --- | --- | --- |
| R2XX | Status replacement with 2XX | `operator.sc.replaceWith20x.enabled` |
| R4XX | Status replacement with 4XX | `operator.sc.replaceWith40x.enabled` |
| R5XX | Status replacement with 5XX | `operator.sc.replaceWith50x.enabled` |
| CTC | Content-Type media type replacement | `operator.header.mediaType.replace.enabled` |
| CTD | Content-Type media type deletion | `operator.header.mediaType.null.enabled` |
| CPC | Charset replacement | `operator.header.charset.replace.enabled` |
| CPD | Charset deletion | `operator.header.charset.null.enabled` |
| LHC | Location header change | `operator.header.location.mutate.enabled` |
| LHD | Location header deletion | `operator.header.location.null.enabled` |
| AEA | Array element addition | `operator.array.addElement.enabled` |
| AER | Array element removal | `operator.array.removeElement.enabled` |
| AEE | Array element exchange | `operator.array.disorderElements.enabled` |
| EAS | Empty array setting | `operator.array.empty.enabled` |
| OPA | Object property addition | `operator.object.addElement.enabled` |
| OPR | Object property removal | `operator.object.removeElement.enabled` |
| OTPR | Object-type property removal | `operator.object.removeObjectElement.enabled` |
| PTC | Property type change | `operator.value.long.changeType.enabled`, `operator.value.double.changeType.enabled`, `operator.value.string.changeType.enabled`, `operator.value.boolean.changeType.enabled`, `operator.value.null.changeType.enabled`, `operator.object.changeType.enabled`, `operator.array.changeType.enabled` |
| BPR | Boolean property reverse | `operator.value.boolean.mutate.enabled` |
| NPS | Null property setting | `operator.value.long.null.enabled`, `operator.value.double.null.enabled`, `operator.value.string.null.enabled`, `operator.value.boolean.null.enabled`, `operator.object.null.enabled`, `operator.array.null.enabled` |
| NPR | Numeric property replacement | `operator.value.long.replace.enabled`, `operator.value.double.replace.enabled` |
| SPR | String property replacement | `operator.value.string.replace.enabled` |
| SCA | Special characters addition | `operator.value.string.addSpecialCharacters.enabled` |
| SRE | String length reduction/extension | `operator.value.string.boundary.enabled` |

## Example Configurations

Two complete configurations are included:

- `rest-mutation.properties` enables every currently supported operator.
- `graphql-mutation.properties` disables R2XX, R4XX, and R5XX because GraphQL application errors commonly retain HTTP status 200. All header and JSON payload operators remain enabled.

To use one, pass the file path through the Java API or CLI:

```java
HttpMutator mutator = new HttpMutator(java.nio.file.Paths.get("graphql-mutation.properties"));
```

```bash
java -jar httpmutator.jar -i traffic.jsonl --properties graphql-mutation.properties
```

The GraphQL configuration deliberately keeps `operator.sc.enabled=true` and disables the three status operators individually. This demonstrates the operator-selection mechanism while producing no status-code mutants.
