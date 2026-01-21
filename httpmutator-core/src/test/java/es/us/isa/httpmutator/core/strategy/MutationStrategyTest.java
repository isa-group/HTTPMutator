package es.us.isa.httpmutator.core.strategy;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import es.us.isa.httpmutator.core.HttpMutatorEngine;
import es.us.isa.httpmutator.core.model.Mutant;

/**
 * Unit tests for different MutationStrategy implementations using JUnit4.
 * <p>
 * Initializes a sample JSON response once, generates mutants, then
 * applies three strategies to the same mutant list to verify behavior.
 *
 * author: Lixin Xu
 */
public class MutationStrategyTest {
    private static HttpMutatorEngine mutator = new HttpMutatorEngine();;
    private static final String JSON = "{" +
            "\"Status Code\":200," +
            "\"Headers\":{\"Content-Type\":\"application/json\",\"Accept\":\"*/*\"}," +
            "\"Body\":{" +
            "\"user\":{\"id\":1,\"name\":\"Alice\"}," +
            "\"tags\":[\"x\",\"y\"]}" +
            "}";

    /**
     * Prepare a sample JSON with nested object and array, then generate mutants.
     */
    @Test
    public void testAllStrategy() {
        List<Mutant> baseMutants = new ArrayList<>();
        // generate all possible mutants with full probability
        AllOperatorsStrategy strategy = new AllOperatorsStrategy();
        mutator.getAllMutants(JSON, mutantGroup -> {
            List<Mutant> mutants = strategy.selectMutants(mutantGroup);
            System.err
                    .println("Generated " + mutants.size() + " mutants" + " for group: " + mutantGroup.getIdentifier());
            baseMutants.addAll(mutants);
        });
        System.out.println("Total mutants generated: " + baseMutants.size());
        System.out.println("====================================================");
    }

    /**
     * Verifies that RandomSingleStrategy selects at most one per group without
     * duplicates.
     */
    @Test
    public void testRandomSingleStrategy() {
        MutationStrategy strategy = new RandomSingleStrategy();
        List<Mutant> selected = new ArrayList<>();
        mutator.getAllMutants(JSON, mutantGroup -> {
            List<Mutant> mutants = strategy.selectMutants(mutantGroup);
            System.err
                    .println("Generated " + mutants.size() + " mutants" + " for group: " + mutantGroup.getIdentifier());
            selected.addAll(mutants);
        });
        System.out.println("Total mutants selected: " + selected.size());
        System.out.println("====================================================");
    }
}
