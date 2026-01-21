package es.us.isa.httpmutator.core.strategy;

import java.util.List;

import es.us.isa.httpmutator.core.model.Mutant;
import es.us.isa.httpmutator.core.model.MutantGroup;

/**
 * Defines a strategy for selecting mutants to apply from a set of candidates.
 * @author Lixin Xu
 */
public interface MutationStrategy {

    /**
     * Selects a subset of mutants to apply from the provided list of all possible
     * mutants.
     *
     * @param group the complete list of available mutants
     * @return a list of mutants chosen for application
     */
    List<Mutant> selectMutants(MutantGroup group);
}