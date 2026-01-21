package es.us.isa.httpmutator.core.strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import es.us.isa.httpmutator.core.RandomManager;
import es.us.isa.httpmutator.core.model.Mutant;
import es.us.isa.httpmutator.core.model.MutantGroup;
import es.us.isa.httpmutator.core.util.RandomUtils;

/**
 * Strategy that picks exactly one random mutation per unique combination of
 * original JSON path and mutator class.
 * <p>
 * This ensures each location-mutator pair is represented by at most one Mutant.
 * Groups input mutants by their {@code originalJsonPath} and {@code mutatorClass},
 * then selects one at random from each group.
 *
 * @author Lixin Xu
 */
public class RandomSingleStrategy implements MutationStrategy {

    /**
     * Selects one random mutant
     */
    @Override
    public List<Mutant> selectMutants(MutantGroup group) {
        Objects.requireNonNull(group, "MutantGroup must not be null");
        if (group.getMutants().isEmpty()) {
            return new ArrayList<>(); // Return empty list if no mutants available
        }
        int idx = RandomUtils.nextInt(group.getMutants().size());
        return Collections.singletonList(group.getMutants().get(idx));
    }
}
