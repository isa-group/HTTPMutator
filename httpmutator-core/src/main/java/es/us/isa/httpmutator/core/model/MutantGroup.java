package es.us.isa.httpmutator.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A container for a group of related mutants with a common identifier.
 * This is the core data structure for grouped/streaming mutation processing.
 * 
 * Used across different mutator types:
 * - BodyMutator: groups mutants by JSON path (e.g., "Body/user/email")
 * - HeaderMutator: groups mutants by header component (e.g., "Headers/content-type/mediaType") 
 * - StatusCodeMutator: groups mutants by component (e.g., "StatusCode")
 * 
 * @author lixin
 */
public class MutantGroup {
    private final String identifier;
    private final List<Mutant> mutants;

    /**
     * Creates a new MutantGroup with the given identifier and mutants.
     * 
     * @param identifier the identifier for this group (e.g., JSON path, component name)
     * @param mutants the list of mutants in this group
     */
    public MutantGroup(String identifier, List<Mutant> mutants) {
        this.identifier = Objects.requireNonNull(identifier, "Identifier cannot be null");
        this.mutants = new ArrayList<>(Objects.requireNonNull(mutants, "Mutants list cannot be null"));
    }

     /**
     * Gets the identifier for this group.
     * 
     * @return the group identifier (e.g., JSON path, component name)
     */
    public String getIdentifier() {
        return identifier;
    }
    
     /**
     * Gets an unmodifiable view of the mutants in this group.
     * 
     * @return unmodifiable list of mutants
     */
    public List<Mutant> getMutants() {
        return Collections.unmodifiableList(mutants);
    }
}
