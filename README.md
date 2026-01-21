<h1 align="left">
  <img src="docs/logo.jpeg" alt="HttpMutator" width="220"/><br/>
</h1>

HttpMutator is a black-box mutation testing tool for web APIs that generates faulty yet realistic variants of HTTP responses to assess the fault-detection capability of API testing tools and test oracles.

Unlike traditional mutation testing that injects faults into source code, HttpMutator **mutates observable HTTP response elements — status codes, headers, and JSON payloads** — to simulate the effects of functional bugs in the underlying implementation. The resulting mutated responses can be replayed against existing test suites or API testing tools, making the approach applicable even **without access to the source code**. It comes with **23** built-in HTTP-level mutation operators across status codes, headers, and JSON bodies; see [docs/mutation-operators.md](docs/mutation-operators.md) for more details.

## Highlights
- **True black-box mutations** on status codes, headers, and JSON bodies to model real API faults without source access.
- **Realistic, replayable mutants** that let you compare tools and oracles on the same set of response variants.
- **REST-assured filter integration** to reuse existing REST-assured assertions and run mutation checks with minimal code changes ([docs/restassured-integration.md](docs/restassured-integration.md)).

## Install

### Build from source
Java Development Kit (JDK) 8 or later and Apache Maven are required.  

```bash
mvn clean install -DskipTests
```

This builds all modules and installs `1.0-SNAPSHOT` artifacts to your local Maven repository.

### Use from another Maven project (local SNAPSHOT)
Core library:
```xml
<dependency>
  <groupId>es.us.isa.httpmutator</groupId>
  <artifactId>httpmutator-core</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

## Quickstart
You can embed HttpMutator in Java tests, or run the HttpMutator CLI to generate mutated outputs for offline pipelines.

### Java API (in-process)
Minimal mutation example (plug mutants into your existing assertions or test logic):
```java
import com.fasterxml.jackson.databind.ObjectMapper;
import es.us.isa.httpmutator.core.HttpMutator;
import es.us.isa.httpmutator.core.model.StandardHttpResponse;
import es.us.isa.httpmutator.core.strategy.RandomSingleStrategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BasicExample {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> headers = new HashMap<>();
        headers.put("content-type", "application/json");

        StandardHttpResponse response = StandardHttpResponse.of(
                200,
                headers,
                mapper.readTree("{\"id\":1,\"name\":\"book\"}")
        );

        HttpMutator mutator = new HttpMutator()
                .withMutationStrategy(new RandomSingleStrategy());

        List<StandardHttpResponse> mutants = mutator.mutate(response);
        for (StandardHttpResponse mutated : mutants) {
            System.out.println(mutated.toJsonString());
        }
    }
}
```

### CLI (offline mutation outputs)
```bash
mvn -pl httpmutator-core -am package
java -jar httpmutator-core/target/httpmutator.jar \
  -i httpmutator-core/src/test/resources/httpmutatorInput.jsonl \
  -o hm-output
```

For the full list of CLI options, see [docs/cli.md](docs/cli.md).

## REST-assured integration
REST-assured is widely used for API testing in Java. HttpMutator provides a REST-assured filter that captures responses, reuses your assertions as mutation oracles, and produces per-request mutation outcomes.

Minimal usage:
```java
import es.us.isa.httpmutator.integrations.restassured.HttpMutatorRestAssuredFilter;
import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

HttpMutatorRestAssuredFilter filter = new HttpMutatorRestAssuredFilter();
RestAssured.filters(filter);

given().when().get("/items/2").then().statusCode(200);
filter.addAssertionsForLastRequest(resp -> resp.statusCode(200).body("id", equalTo(2)));

filter.runAllMutations();
```

For setup details and report outputs, see [docs/restassured-integration.md](docs/restassured-integration.md).

## Configuration
HttpMutator is configurable: you can enable/disable mutation categories and tune value ranges used by certain operators.
Defaults are loaded from `httpmutator-core/src/main/resources/json-mutation.properties` via `PropertyManager`.

Common adjustments:
- Enable/disable mutation categories: status code, headers, and JSON body.
- Tune numeric and string ranges used by value-level operators (e.g., min/max length, min/max numeric values).
- Enable/disable specific header-related mutations (e.g., media type, charset).

Programmatic override:
```java
import es.us.isa.httpmutator.core.util.PropertyManager;

PropertyManager.setProperty("operator.body.enabled", "true");
PropertyManager.setProperty("operator.value.string.length.max", "256");
```
Reset to defaults:

```java
import es.us.isa.httpmutator.core.util.PropertyManager;

PropertyManager.resetProperties();
```

## Extending HttpMutator
HttpMutator is designed for extension: add new operators, customize mutation strategies, and plug in reporters or writers for your own outputs. The core API exposes extension points in `AbstractOperator`, `AbstractMutator`, `MutationStrategy`, `MutantWriter`, and `MutantReporter`.

See [docs/extending-httpmutator.md](docs/extending-httpmutator.md) for extension patterns and minimal examples.
