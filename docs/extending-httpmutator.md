# Extending HttpMutator

HttpMutator is built from small, composable pieces: operators mutate values, mutators group operators, strategies choose which mutants to keep, and reporters/writers emit outputs. This page summarizes the extension points that are implemented in the codebase.

## Custom operator

Implement `AbstractOperator` and register it in an existing mutator (or your own).

```java
import es.us.isa.httpmutator.core.AbstractOperator;

// Example: replace any string with a fixed marker
public final class RedactStringOperator extends AbstractOperator {
    public RedactStringOperator() { setWeight(0.5f); }

    @Override
    protected Object doMutate(Object element) {
        return "[REDACTED]";
    }

    @Override
    public boolean isApplicable(Object element) {
        return element instanceof String;
    }
}
```

Register the operator in a mutator before running mutations:

```java
import es.us.isa.httpmutator.core.body.value.string0.StringMutator;

StringMutator mutator = new StringMutator();
mutator.getOperators().put("redact", new RedactStringOperator());
```

## Custom mutation strategy

Implement `MutationStrategy` to control how many mutants are selected from each `MutantGroup`.

```java
import es.us.isa.httpmutator.core.model.Mutant;
import es.us.isa.httpmutator.core.model.MutantGroup;
import es.us.isa.httpmutator.core.strategy.MutationStrategy;

import java.util.Collections;
import java.util.List;

public final class FirstOnlyStrategy implements MutationStrategy {
    @Override
    public List<Mutant> selectMutants(MutantGroup group) {
        return group.getMutants().isEmpty()
                ? Collections.emptyList()
                : Collections.singletonList(group.getMutants().get(0));
    }
}
```

Use it with `HttpMutator`:

```java
import es.us.isa.httpmutator.core.HttpMutator;

HttpMutator mutator = new HttpMutator()
        .withMutationStrategy(new FirstOnlyStrategy());
```

## Custom reporter or writer

Reporters observe mutants for metrics; writers emit mutated outputs.

```java
import es.us.isa.httpmutator.core.model.HttpExchange;
import es.us.isa.httpmutator.core.model.Mutant;
import es.us.isa.httpmutator.core.model.StandardHttpResponse;
import es.us.isa.httpmutator.core.reporter.MutantReporter;

public final class CountingReporter implements MutantReporter {
    private int count = 0;

    @Override
    public void onMutant(HttpExchange exchange, StandardHttpResponse mutated, Mutant mutant) {
        count++;
    }

    @Override
    public void onFinished() {
        System.out.println("Total mutants: " + count);
    }
}
```

Attach reporters or writers to `HttpMutator`:

```java
import es.us.isa.httpmutator.core.HttpMutator;

HttpMutator mutator = new HttpMutator()
        .withMutationStrategy(new FirstOnlyStrategy())
        .addReporter(new CountingReporter());
```

## Custom converters

Integrations can implement `BidirectionalConverter<T>` to map client responses to `StandardHttpResponse` and back. The REST-assured integration uses this approach to convert `io.restassured.response.Response`.

## Configuration overrides

Mutation toggles live in `json-mutation.properties`. You can override them programmatically:

```java
import es.us.isa.httpmutator.core.util.PropertyManager;

PropertyManager.setProperty("operator.body.enabled", "true");
PropertyManager.setProperty("operator.value.string.length.max", "256");
```

Call `PropertyManager.resetProperties()` to restore defaults.
