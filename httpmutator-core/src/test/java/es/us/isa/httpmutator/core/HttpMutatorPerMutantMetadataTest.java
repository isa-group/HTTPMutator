package es.us.isa.httpmutator.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.us.isa.httpmutator.core.model.Mutant;
import es.us.isa.httpmutator.core.model.StandardHttpResponse;
import es.us.isa.httpmutator.core.strategy.AllOperatorsStrategy;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HttpMutatorPerMutantMetadataTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void mutateWithBiConsumerProvidesMutatedResponseAndMutantMetadata() throws Exception {
        JsonNode body = MAPPER.readTree("{\"id\": 7, \"name\": \"alpha\", \"ok\": true}");
        StandardHttpResponse original = StandardHttpResponse.of(200, new HashMap<String, Object>(), body);

        HttpMutator mutator = new HttpMutator(42L).withMutationStrategy(new AllOperatorsStrategy());

        final List<StandardHttpResponse> mutatedResponses = new ArrayList<StandardHttpResponse>();
        final List<Mutant> mutants = new ArrayList<Mutant>();

        mutator.mutate(original, "metadata-request", (mutated, mutant) -> {
            mutatedResponses.add(mutated);
            mutants.add(mutant);
        });
        mutator.close();

        Assert.assertFalse("Expected at least one mutated response", mutatedResponses.isEmpty());
        Assert.assertEquals("Each mutated response should have matching mutant metadata",
                mutatedResponses.size(), mutants.size());

        Mutant first = mutants.get(0);
        Assert.assertNotNull("Mutator class should be available", first.getMutatorClassName());
        Assert.assertNotNull("Operator class should be available", first.getOperatorClassName());
        Assert.assertNotNull("Original JSON path should be available", first.getOriginalJsonPath());
        Assert.assertFalse("Operator class name should not be empty",
                first.getOperatorClassName().trim().isEmpty());
    }
}
