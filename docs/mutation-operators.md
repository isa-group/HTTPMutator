# Mutation Operators

- [Mutation Operators](#mutation-operators)
  - [Status Code Mutation Operators](#status-code-mutation-operators)
    - [R2XX - Replacement with status code 2XX](#r2xx---replacement-with-status-code-2xx)
      - [Applicability / Preconditions](#applicability--preconditions)
      - [Example (code)](#example-code)
    - [R4XX - Replacement with status code 4XX](#r4xx---replacement-with-status-code-4xx)
      - [Applicability / Preconditions](#applicability--preconditions-1)
      - [Example (code)](#example-code-1)
    - [R5XX - Replacement with status code 5XX](#r5xx---replacement-with-status-code-5xx)
      - [Applicability / Preconditions](#applicability--preconditions-2)
      - [Example (code)](#example-code-2)
  - [Header Mutation Operators](#header-mutation-operators)
    - [CTC - Content-Type Change](#ctc---content-type-change)
      - [Applicability / Preconditions](#applicability--preconditions-3)
      - [Example (code)](#example-code-3)
    - [CTD - Content-Type Deletion](#ctd---content-type-deletion)
      - [Applicability / Preconditions](#applicability--preconditions-4)
      - [Example (code)](#example-code-4)
    - [CPC - Charset Property Change](#cpc---charset-property-change)
      - [Applicability / Preconditions](#applicability--preconditions-5)
      - [Example (code)](#example-code-5)
    - [CPD - Charset Property Deletion](#cpd---charset-property-deletion)
      - [Applicability / Preconditions](#applicability--preconditions-6)
      - [Example (code)](#example-code-6)
    - [LHC - Location Header Change](#lhc---location-header-change)
      - [Applicability / Preconditions](#applicability--preconditions-7)
      - [Example (code)](#example-code-7)
    - [LHD - Location Header Deletion](#lhd---location-header-deletion)
      - [Applicability / Preconditions](#applicability--preconditions-8)
      - [Example (code)](#example-code-8)
  - [JSON Mutation Operators](#json-mutation-operators)
    - [AEA - Array Element Addition](#aea---array-element-addition)
      - [Applicability / Preconditions](#applicability--preconditions-9)
      - [Example (code)](#example-code-9)
    - [AER - Array Element Removal](#aer---array-element-removal)
      - [Applicability / Preconditions](#applicability--preconditions-10)
      - [Example (code)](#example-code-10)
    - [AEE - Array Elements Exchange](#aee---array-elements-exchange)
      - [Applicability / Preconditions](#applicability--preconditions-11)
      - [Example (code)](#example-code-11)
    - [EAS - Empty Array Setting](#eas---empty-array-setting)
      - [Applicability / Preconditions](#applicability--preconditions-12)
      - [Example (code)](#example-code-12)
    - [OPA - Object Property Addition](#opa---object-property-addition)
      - [Applicability / Preconditions](#applicability--preconditions-13)
      - [Example (code)](#example-code-13)
    - [OPR - Object Property Removal](#opr---object-property-removal)
      - [Applicability / Preconditions](#applicability--preconditions-14)
      - [Example (code)](#example-code-14)
    - [OTPR - Object-Type Property Removal](#otpr---object-type-property-removal)
      - [Applicability / Preconditions](#applicability--preconditions-15)
      - [Example (code)](#example-code-15)
    - [PTC - Property Type Change](#ptc---property-type-change)
      - [Applicability / Preconditions](#applicability--preconditions-16)
      - [Example (code)](#example-code-16)
    - [BPR - Boolean Property Reverse](#bpr---boolean-property-reverse)
      - [Applicability / Preconditions](#applicability--preconditions-17)
      - [Example (code)](#example-code-17)
    - [NPS - Null Property Setting](#nps---null-property-setting)
      - [Applicability / Preconditions](#applicability--preconditions-18)
      - [Example (code)](#example-code-18)
    - [NPR - Numeric Property Replacement](#npr---numeric-property-replacement)
      - [Applicability / Preconditions](#applicability--preconditions-19)
      - [Example (code)](#example-code-19)
    - [SPR - String Property Replacement](#spr---string-property-replacement)
      - [Applicability / Preconditions](#applicability--preconditions-20)
      - [Example (code)](#example-code-20)
    - [SCA - Special Characters Addition](#sca---special-characters-addition)
      - [Applicability / Preconditions](#applicability--preconditions-21)
      - [Example (code)](#example-code-21)
    - [SRE - String Length Reduction/Extension](#sre---string-length-reductionextension)
      - [Applicability / Preconditions](#applicability--preconditions-22)
      - [Example (code)](#example-code-22)
- [Extending Mutation Operators](#extending-mutation-operators)
  - [Step 1. Implement an operator](#step-1-implement-an-operator)
  - [Step 2. Register the operator in a mutator](#step-2-register-the-operator-in-a-mutator)


This document explains how each mutation operator is applied, what it can act on, and what the resulting mutant looks like.

- **Mutator**: classes that extend `AbstractMutator` and choose which operator to apply for a given JSON node or header component. Examples: `StatusCodeMutator`, `HeaderMutator`, `BodyMutator`, plus type-specific mutators under `httpmutator-core/src/main/java/es/us/isa/httpmutator/core/...`.
- **Operator**: classes that extend `AbstractOperator` and implement `doMutate(...)`. These live under `.../operator/` packages.
- **Flow**: a mutator scans one recorded HTTP response (status, headers, JSON body) and picks the applicable operator(s) for each target part; each operator performs one small change to produce a mutant variant, which is then collected and exported as JSONL (optionally .jsonl.zst shards), HAR, or kept in memory.

## Status Code Mutation Operators
### R2XX - Replacement with status code 2XX
Replaces the status code with a 2XX success code to simulate incorrectly successful responses (code uses `{200, 201, 202, 204}` and does not include 202).

#### Applicability / Preconditions
- Input must be an integer HTTP status code.
#### Example (code)
```java
import es.us.isa.httpmutator.core.sc.operator.StatusCodeReplacementWith20XOperator;

public class Example {
    public static void main(String[] args) throws Exception {
        StatusCodeReplacementWith20XOperator op = new StatusCodeReplacementWith20XOperator();
        int mutated = (int) op.mutate(400);
    }
}
```
Result: returns a different 2xx status code (int) chosen from {200, 201, 204}. Example: 400 -> 200.

### R4XX - Replacement with status code 4XX
Replaces the status code with a 4XX client-error code to simulate incorrect error responses.

#### Applicability / Preconditions
- Input must be an integer HTTP status code.
#### Example (code)
```java
import es.us.isa.httpmutator.core.sc.operator.StatusCodeReplacementWith40XOperator;

public class Example {
    public static void main(String[] args) throws Exception {
        StatusCodeReplacementWith40XOperator op = new StatusCodeReplacementWith40XOperator();
        int mutated = (int) op.mutate(200);
    }
}
```
Result: returns a different 4xx status code (int) chosen from {400, 401, 403, 404, 409}. Example: 200 -> 400.

### R5XX - Replacement with status code 5XX
Replaces the status code with a 5XX server-error code to simulate unexpected failures (code includes 501).

#### Applicability / Preconditions
- Input must be an integer HTTP status code.
#### Example (code)
```java
import es.us.isa.httpmutator.core.sc.operator.StatusCodeReplacementWith50XOperator;

public class Example {
    public static void main(String[] args) throws Exception {
        StatusCodeReplacementWith50XOperator op = new StatusCodeReplacementWith50XOperator();
        int mutated = (int) op.mutate(200);
    }
}
```
Result: returns a different 5xx status code (int) chosen from {500, 501, 502, 503, 504}. Example: 200 -> 503.

## Header Mutation Operators
### CTC - Content-Type Change
Replaces the media type portion of `Content-Type` with another common media type.

#### Applicability / Preconditions
- Input must be a media type string from the Content-Type header (e.g., application/json).
#### Example (code)
```java
import es.us.isa.httpmutator.core.headers.mediaType.operator.MediaTypeReplacementOperator;

public class Example {
    public static void main(String[] args) throws Exception {
        MediaTypeReplacementOperator op = new MediaTypeReplacementOperator();
        String mutated = (String) op.mutate("application/json");
    }
}
```
Result: returns a different media type string (e.g., text/plain) for the Content-Type header. Example: "application/json" -> "text/plain".

### CTD - Content-Type Deletion
Deletes the media type portion of `Content-Type`, leaving only parameters like `charset`.

#### Applicability / Preconditions
- Input must be a media type string from the Content-Type header.
#### Example (code)
```java
import es.us.isa.httpmutator.core.body.value.common.operator.NullOperator;
import es.us.isa.httpmutator.core.headers.mediaType.MediaTypeMutator;

public class Example {
    public static void main(String[] args) throws Exception {
        NullOperator op = new NullOperator(MediaTypeMutator.class);
        Object mutated = op.mutate("application/json");
    }
}
```
Result: returns null to indicate the media type should be removed. Example: "application/json" -> null.

### CPC - Charset Property Change
Replaces the `charset` parameter in `Content-Type`; if `charset` is missing, code may insert one.

#### Applicability / Preconditions
- Input must be a charset string (e.g., utf-8) or null when missing.
#### Example (code)
```java
import es.us.isa.httpmutator.core.headers.charset.operator.CharsetReplacementOperator;

public class Example {
    public static void main(String[] args) throws Exception {
        CharsetReplacementOperator op = new CharsetReplacementOperator();
        String mutated = (String) op.mutate("utf-8");
    }
}
```
Result: returns a different charset string (or inserts one if input is null). Example: "utf-8" -> "UTF-16".

### CPD - Charset Property Deletion
Deletes the `charset` parameter in `Content-Type`.

#### Applicability / Preconditions
- Input must be a charset string from the Content-Type header.
#### Example (code)
```java
import es.us.isa.httpmutator.core.body.value.common.operator.NullOperator;
import es.us.isa.httpmutator.core.headers.charset.CharsetMutator;

public class Example {
    public static void main(String[] args) throws Exception {
        NullOperator op = new NullOperator(CharsetMutator.class);
        Object mutated = op.mutate("utf-8");
    }
}
```
Result: returns null to indicate the charset should be removed. Example: "utf-8" -> null.

### LHC - Location Header Change
Modifies the `Location` header by appending `/<currentTimeMillis>` to the URI path.

#### Applicability / Preconditions
- Input must be a Location header value string parseable as a URI.
#### Example (code)
```java
import es.us.isa.httpmutator.core.headers.location.operator.LocationMutationOperator;

public class Example {
    public static void main(String[] args) throws Exception {
        LocationMutationOperator op = new LocationMutationOperator();
        String mutated = (String) op.mutate("https://example.org/items/1");
    }
}
```
Result: returns a new Location URI string with a modified path. Example: "https://example.org/items/1" -> "https://example.org/items/1/123456789".

### LHD - Location Header Deletion
Deletes the `Location` header.

#### Applicability / Preconditions
- Input must be a Location header value string.
#### Example (code)
```java
import es.us.isa.httpmutator.core.body.value.common.operator.NullOperator;
import es.us.isa.httpmutator.core.headers.location.LocationMutator;

public class Example {
    public static void main(String[] args) throws Exception {
        NullOperator op = new NullOperator(LocationMutator.class);
        Object mutated = op.mutate("https://example.org/items/1");
    }
}
```
Result: returns null to indicate the Location header should be removed. Example: "https://example.org/items/1" -> null.

## JSON Mutation Operators
### AEA - Array Element Addition
Inserts new elements into an existing array.

#### Applicability / Preconditions
- Input must be a JSON array node (ArrayNode).
#### Example (code)
```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import es.us.isa.httpmutator.core.body.array.operator.ArrayAddElementOperator;

public class Example {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode items = (ArrayNode) mapper.readTree("[1]");
        ArrayAddElementOperator op = new ArrayAddElementOperator();
        ArrayNode mutated = (ArrayNode) op.mutate(items);
    }
}
```
Result: returns the ArrayNode with one or more new elements appended. Example: [1] -> [1, 0].

### AER - Array Element Removal
Removes elements from an array; `isApplicable(...)` requires the array size to exceed `operator.array.removedElements.max`.

#### Applicability / Preconditions
- Input must be a JSON array node (ArrayNode).
- Array size must be greater than the configured max removed count.
#### Example (code)
```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import es.us.isa.httpmutator.core.body.array.operator.ArrayRemoveElementOperator;

public class Example {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode items = (ArrayNode) mapper.readTree("[1,2,3,4,5]");
        ArrayRemoveElementOperator op = new ArrayRemoveElementOperator();
        ArrayNode mutated = (ArrayNode) op.mutate(items);
    }
}
```
Result: returns the ArrayNode with one or more elements removed. Example: [1,2,3,4,5] -> [1,2,4,5].

### AEE - Array Elements Exchange
Reorders an array by moving one element to a different position.

#### Applicability / Preconditions
- Input must be a JSON array node (ArrayNode) with size > 1.
#### Example (code)
```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import es.us.isa.httpmutator.core.body.array.operator.ArrayDisorderElementsOperator;

public class Example {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode items = (ArrayNode) mapper.readTree("[\"a\",\"b\"]");
        ArrayDisorderElementsOperator op = new ArrayDisorderElementsOperator();
        ArrayNode mutated = (ArrayNode) op.mutate(items);
    }
}
```
Result: returns the ArrayNode with elements reordered. Example: ["a","b"] -> ["b","a"].

### EAS - Empty Array Setting
Clears all elements from a non-empty array.

#### Applicability / Preconditions
- Input must be a JSON array node (ArrayNode) with size > 0.
#### Example (code)
```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import es.us.isa.httpmutator.core.body.array.operator.ArrayEmptyOperator;

public class Example {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode items = (ArrayNode) mapper.readTree("[1]");
        ArrayEmptyOperator op = new ArrayEmptyOperator();
        ArrayNode mutated = (ArrayNode) op.mutate(items);
    }
}
```
Result: returns the ArrayNode with all elements removed (empty array). Example: [1] -> [].

### OPA - Object Property Addition
Adds new properties to a JSON object.

#### Applicability / Preconditions
- Input must be a JSON object node (ObjectNode).
#### Example (code)
```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import es.us.isa.httpmutator.core.body.object.operator.ObjectAddElementOperator;

public class Example {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode obj = (ObjectNode) mapper.readTree("{\"id\":1}");
        ObjectAddElementOperator op = new ObjectAddElementOperator();
        ObjectNode mutated = (ObjectNode) op.mutate(obj);
    }
}
```
Result: returns the ObjectNode with one or more new properties added. Example: {"id":1} -> {"id":1,"randomLong1":42}.

### OPR - Object Property Removal
Removes existing properties from a JSON object.

#### Applicability / Preconditions
- Input must be a JSON object node (ObjectNode) with at least one property.
#### Example (code)
```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import es.us.isa.httpmutator.core.body.object.operator.ObjectRemoveElementOperator;

public class Example {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode obj = (ObjectNode) mapper.readTree("{\"id\":1,\"name\":\"x\"}");
        ObjectRemoveElementOperator op = new ObjectRemoveElementOperator();
        ObjectNode mutated = (ObjectNode) op.mutate(obj);
    }
}
```
Result: returns the ObjectNode with one or more properties removed. Example: {"id":1,"name":"x"} -> {"id":1}.

### OTPR - Object-Type Property Removal
Removes object-valued properties from a JSON object; current code only keeps the last discovered object-type property as a removal candidate.

#### Applicability / Preconditions
- Input must be a JSON object node (ObjectNode) that contains at least one object-valued property.
#### Example (code)
```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import es.us.isa.httpmutator.core.body.object.operator.ObjectRemoveObjectTypeElementOperator;

public class Example {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode obj = (ObjectNode) mapper.readTree("{\"a\":1,\"details\":{\"x\":2}}");
        ObjectRemoveObjectTypeElementOperator op = new ObjectRemoveObjectTypeElementOperator();
        ObjectNode mutated = (ObjectNode) op.mutate(obj);
    }
}
```
Result: returns the ObjectNode with an object-valued property removed. Example: {"a":1,"details":{"x":2}} -> {"a":1}.

### PTC - Property Type Change
Changes the type of a JSON node to a different type; for root-level object/array bodies, `BodyMutator.resetFirstLevelOperators()` removes this operator so it applies to nested nodes.

#### Applicability / Preconditions
- Input must be a JSON value (array, object, string, number, boolean, or null).
#### Example (code)
```java
import es.us.isa.httpmutator.core.body.value.common.operator.ChangeTypeOperator;

public class Example {
    public static void main(String[] args) throws Exception {
        ChangeTypeOperator op = new ChangeTypeOperator(String.class);
        Object mutated = op.mutate("abc");
    }
}
```
Result: returns a value of a different JSON type than the input. Example: "abc" -> 42.

### BPR - Boolean Property Reverse
Flips a boolean value.

#### Applicability / Preconditions
- Input must be a boolean value.
#### Example (code)
```java
import es.us.isa.httpmutator.core.body.value.boolean0.operator.BooleanMutationOperator;

public class Example {
    public static void main(String[] args) throws Exception {
        BooleanMutationOperator op = new BooleanMutationOperator();
        boolean mutated = (boolean) op.mutate(true);
    }
}
```
Result: returns the negated boolean value. Example: true -> false.

### NPS - Null Property Setting
Replaces a value with JSON null; for root-level object/array bodies, `BodyMutator.resetFirstLevelOperators()` removes this operator so it applies to nested nodes.

#### Applicability / Preconditions
- Input must be a JSON value (array, object, string, number, or boolean).
#### Example (code)
```java
import es.us.isa.httpmutator.core.body.value.common.operator.NullOperator;

public class Example {
    public static void main(String[] args) throws Exception {
        NullOperator op = new NullOperator(String.class);
        Object mutated = op.mutate("abc");
    }
}
```
Result: returns null. Example: "abc" -> null.

### NPR - Numeric Property Replacement
Replaces a numeric value with another numeric value in the configured range.

#### Applicability / Preconditions
- Input must be an integral or floating-point number.
#### Example (code)
```java
import es.us.isa.httpmutator.core.body.value.long0.operator.LongReplacementOperator;
import es.us.isa.httpmutator.core.body.value.double0.operator.DoubleReplacementOperator;

public class Example {
    public static void main(String[] args) throws Exception {
        LongReplacementOperator longOp = new LongReplacementOperator();
        DoubleReplacementOperator doubleOp = new DoubleReplacementOperator();
        long mutatedLong = (long) longOp.mutate(10L);
        double mutatedDouble = (double) doubleOp.mutate(1.5d);
    }
}
```
Result: returns a numeric value within the configured range. Example: 10 -> 42.

### SPR - String Property Replacement
Replaces a string value with a randomly generated string.

#### Applicability / Preconditions
- Input must be a string value.
#### Example (code)
```java
import es.us.isa.httpmutator.core.body.value.string0.operator.StringReplacementOperator;

public class Example {
    public static void main(String[] args) throws Exception {
        StringReplacementOperator op = new StringReplacementOperator();
        String mutated = (String) op.mutate("abc");
    }
}
```
Result: returns a new randomly generated string. Example: "abc" -> "Q9x@L".

### SCA - Special Characters Addition
Inserts special characters into a string value.

#### Applicability / Preconditions
- Input must be a string value (empty strings are allowed).
#### Example (code)
```java
import es.us.isa.httpmutator.core.body.value.string0.operator.StringAddSpecialCharactersMutationOperator;

public class Example {
    public static void main(String[] args) throws Exception {
        StringAddSpecialCharactersMutationOperator op = new StringAddSpecialCharactersMutationOperator();
        String mutated = (String) op.mutate("abc");
    }
}
```
Result: returns the string with special characters inserted. Example: "abc" -> "a/bc".

### SRE - String Length Reduction/Extension
Replaces a string value with boundary cases (empty, min/max length, lowercase/uppercase).

#### Applicability / Preconditions
- Input must be a string value.
#### Example (code)
```java
import es.us.isa.httpmutator.core.body.value.string0.operator.StringBoundaryOperator;

public class Example {
    public static void main(String[] args) throws Exception {
        StringBoundaryOperator op = new StringBoundaryOperator();
        String mutated = (String) op.mutate("abc");
    }
}
```
Result: returns a boundary-case string (empty, min/max length, lowercase, or uppercase). Example: "abc" -> "".

# Extending Mutation Operators
HttpMutator is extensible by design and mutation behavior is extended by adding new mutation operators.
Operators are applied by mutators during traversal of responses and produce concrete mutant values.
This section describes the supported extension path for operators.

## Step 1. Implement an operator

A mutation operator represents one concrete change to a value.
Operators extend `AbstractOperator` and define two required behaviors in this codebase:
- `protected Object doMutate(Object element)` implements the actual mutation and returns the mutated value.
- `public boolean isApplicable(Object element)` restricts where the operator can be applied so that incompatible inputs are skipped.

The `ExampleOperator` below illustrates this contract for string values.
`isApplicable(...)` returns true only when the input is a `String`, so the operator is applied only to string-typed locations discovered by the mutator.
`doMutate(...)` implements the mutation by appending the suffix `"_mut"` to the original value.
A string value such as `"book"` is therefore mutated into `"book_mut"`, while non-string values are ignored by this operator.

```java
import es.us.isa.httpmutator.core.AbstractOperator;

public class ExampleOperator extends AbstractOperator {
    @Override
    protected Object doMutate(Object value) {
        return String.valueOf(value) + "_mut";
    }

    @Override
    public boolean isApplicable(Object value) {
        return value instanceof String;
    }
}
```

## Step 2. Register the operator in a mutator

Mutators select mutation targets and apply mutation operators using an operator map.
An operator becomes available to a mutator after it is inserted into that map via `getOperators().put(...)`.
Registration is typically performed by extending an existing mutator such as StringMutator.

Minimal mutator integration example:
```java
import es.us.isa.httpmutator.core.body.value.string0.StringMutator;
import es.us.isa.httpmutator.core.util.OperatorNames;

public class CustomStringMutator extends StringMutator {
    public CustomStringMutator() {
        super();
        getOperators().put(OperatorNames.REPLACE, new ExampleOperator());
    }
}
```
