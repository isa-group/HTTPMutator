package es.us.isa.httpmutator.core.strategy;

import java.util.ArrayList;
import java.util.List;

import es.us.isa.httpmutator.core.model.Mutant;
import es.us.isa.httpmutator.core.model.MutantGroup;

/**
 * Strategy that returns all candidate mutants without filtering.
 * <p>
 * This strategy is useful when you want to apply every possible mutation.
 * It creates a defensive copy of the input list to prevent external
 * modification.
 *
 * @author Lixin Xu
 */
public class AllOperatorsStrategy implements MutationStrategy {

    /**
     * Returns a new list containing all provided mutants.
     * 
     * @param group complete list of candidate mutants; must not be null
     * @return a defensive copy of the input list of mutants
     * @throws NullPointerException if {@code allMutants} is null
     */
    @Override
    public List<Mutant> selectMutants(MutantGroup group) {
        return new ArrayList<>(group.getMutants());
    }

}
