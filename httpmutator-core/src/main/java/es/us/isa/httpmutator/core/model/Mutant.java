package es.us.isa.httpmutator.core.model;

import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.us.isa.httpmutator.core.AbstractMutator;
import es.us.isa.httpmutator.core.AbstractOperator;

/**
 * Represents a single mutation applied to a JSON document.
 * Immutable: all fields are final and cannot be changed after construction.
 *
 * Stores the JSONPath of the original node and the classes of the mutator and
 * operator used.
 *
 * author: Lixin Xu
 */
public final class Mutant {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** JSONPath string pointing to the original node before mutation. */
    private final String originalJsonPath;

    /** The mutated JSON node after mutation. */
    private final JsonNode mutatedNode;

    /** The class of the mutator that generated this mutation. */
    private final Class<? extends AbstractMutator> mutatorClass;

    /** The class of the operator used to perform the mutation. */
    private final Class<? extends AbstractOperator> operatorClass;

    /**
     * Constructs a new Mutant instance.
     *
     * @param originalJsonPath JSONPath of the node before mutation
     * @param mutatedNode      the resulting JSON node after mutation
     * @param mutatorClass     the class of the mutator used
     * @param operatorClass    the class of the operator used
     */
    public Mutant(String originalJsonPath,
            JsonNode mutatedNode,
            Class<? extends AbstractMutator> mutatorClass,
            Class<? extends AbstractOperator> operatorClass) {
        this.originalJsonPath = Objects.requireNonNull(originalJsonPath, "originalJsonPath must not be null");
        this.mutatedNode = Objects.requireNonNull(mutatedNode, "mutatedNode must not be null");
        this.mutatorClass = Objects.requireNonNull(mutatorClass, "mutatorClass must not be null");
        this.operatorClass = Objects.requireNonNull(operatorClass, "operatorClass must not be null");
    }

    /** @return the JSONPath of the node before mutation */
    public String getOriginalJsonPath() {
        return originalJsonPath;
    }

    /** @return the JSON node after mutation */
    public JsonNode getMutatedNode() {
        return mutatedNode;
    }

    public String getMutatedNodeAsString() {
        try {
            return MAPPER.writeValueAsString(mutatedNode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert mutated node to string", e);
        }
    }

    /** @return the class of the mutator that generated this mutation */
    public Class<? extends AbstractMutator> getMutatorClass() {
        return mutatorClass;
    }

    public String getMutatorClassName() {
        return mutatorClass.getSimpleName();
    }

    public String getOperatorClassName() {
        return operatorClass.getSimpleName();
    }

    /** @return the class of the operator used to perform this mutation */
    public Class<? extends AbstractOperator> getOperatorClass() {
        return operatorClass;
    }

    public Mutant deepCopy() {
        JsonNode nodeCopy = mutatedNode.deepCopy();
        return new Mutant(originalJsonPath, nodeCopy, mutatorClass, operatorClass);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Mutant))
            return false;
        Mutant that = (Mutant) o;
        return originalJsonPath.equals(that.originalJsonPath) &&
                mutatedNode.equals(that.mutatedNode) &&
                mutatorClass.equals(that.mutatorClass) &&
                operatorClass.equals(that.operatorClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalJsonPath, mutatedNode, mutatorClass, operatorClass);
    }

    @Override
    public String toString() {
        return "Mutant{" +
                "originalJsonPath='" + originalJsonPath + '\'' +
                ", mutatorClass=" + mutatorClass.getSimpleName() +
                ", operatorClass=" + operatorClass.getSimpleName() +
                '}';
    }
}
