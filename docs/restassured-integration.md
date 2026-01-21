# REST Assured Integration
## Introduction

This module provides a REST Assured **filter implementation** – `HttpMutatorRestAssuredFilter` – that lets developers use `HttpMutator` to easily evaluate the **mutation-based adequacy** of their test suites and the **strength of their assertions**. The integration is non-intrusive: if you do not trigger mutation explicitly, your modified tests behave exactly like the original ones.

At a high level, you use the filter in three steps:

1. **Construction** – Create an instance of `HttpMutatorRestAssuredFilter` and register it in the global REST Assured filter chain (e.g., `RestAssured.filters(filter)`). This only makes the filter observe requests and responses; it does **not** change test behavior on its own.

2. **Attach assertions** – After executing a request, call `filter.addAssertionsForLastRequest(resp -> resp ... )`. This method lets you describe how the original response should look using the standard REST Assured `ValidatableResponse` API. The filter **stores these assertions**, automatically applies them to the captured original response (so you do not need separate baseline assertions in your test body), and later **reuses the exact same assertions** as the oracle for each mutant.

3. **Run mutations (optional)** – Finally, you may call `MutationSummary summary = filter.runAllMutations();` to generate mutants for the captured responses, replay them through the stored assertions, and obtain a summary of killed vs. surviving mutants. This step is completely **optional**: if you do **not** call `runAllMutations()`, no mutation is performed, there is no extra runtime cost, and your tests’ functional behavior and logic remain identical to the original suite.

By separating these three steps, `HttpMutatorRestAssuredFilter` allows you to turn existing REST Assured tests into mutation-aware suites when you want it, while keeping them indistinguishable from plain tests when you don’t call `runAllMutations()`.

## Dependency
```xml
<dependency>
  <groupId>es.us.isa.httpmutator</groupId>
  <artifactId>httpmutator-integrations</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

## Full Filter Workflow
`HttpMutatorRestAssuredFilter` observes requests, attaches assertions, and replays mutants back through the same assertions to compute mutant kills.

```java
import es.us.isa.httpmutator.core.strategy.RandomSingleStrategy;
import es.us.isa.httpmutator.integrations.restassured.HttpMutatorRestAssuredFilter;
import es.us.isa.httpmutator.integrations.restassured.HttpMutatorRestAssuredFilter.MutationSummary;
import es.us.isa.httpmutator.integrations.restassured.HttpMutatorRestAssuredFilter.OriginalAssertionFailurePolicy;
import io.restassured.RestAssured;

HttpMutatorRestAssuredFilter filter = new HttpMutatorRestAssuredFilter();
RestAssured.filters(filter);

// Execute a request (filter stores the last response)
RestAssured.given()
        .baseUri("http://localhost:8080")
        .get("/items/2");

// Attach assertions for the last response. 
// stores your REST Assured assertions and
// automatically applies them to the original response—no need to write a separate baseline assertion. 
// The same assertions are reused for all mutants.
filter.addAssertionsForLastRequest(resp -> resp
        .statusCode(200)
        .body("id", org.hamcrest.Matchers.equalTo(2)));

MutationSummary summary = filter.runAllMutations();
summary.getPerRequestResults().forEach(result ->
        System.out.printf("%s -> %s (%d/%d killed)%n",
                result.getLabel(),
                result.getStatus(),
                result.getKilledMutants(),
                result.getTotalMutants()));
```

## Example Comparison Test Cases
You can refer to the `httpmutator-examples` module for complete, runnable REST Assured test files that compare plain tests with `HttpMutator`-enhanced mutation checks, including how baseline assertions are applied and how mutants are evaluated.