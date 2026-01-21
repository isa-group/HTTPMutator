package es.us.isa.httpmutator.core.body;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import es.us.isa.httpmutator.core.AbstractMutator;
import es.us.isa.httpmutator.core.AbstractOperator;
import es.us.isa.httpmutator.core.model.Mutant;
import es.us.isa.httpmutator.core.model.MutantGroup;
import es.us.isa.httpmutator.core.body.array.ArrayMutator;
import es.us.isa.httpmutator.core.body.array.operator.ArrayAddElementOperator;
import es.us.isa.httpmutator.core.body.array.operator.ArrayRemoveElementOperator;
import es.us.isa.httpmutator.core.body.object.ObjectMutator;
import es.us.isa.httpmutator.core.body.object.operator.ObjectAddElementOperator;
import es.us.isa.httpmutator.core.body.object.operator.ObjectRemoveElementOperator;
import es.us.isa.httpmutator.core.body.object.operator.ObjectRemoveObjectTypeElementOperator;
import es.us.isa.httpmutator.core.body.value.boolean0.BooleanMutator;
import es.us.isa.httpmutator.core.body.value.double0.DoubleMutator;
import es.us.isa.httpmutator.core.body.value.long0.LongMutator;
import es.us.isa.httpmutator.core.body.value.null0.NullMutator;
import es.us.isa.httpmutator.core.body.value.string0.StringMutator;
import static es.us.isa.httpmutator.core.util.JsonManager.getNodeElement;
import static es.us.isa.httpmutator.core.util.JsonManager.insertElement;
import es.us.isa.httpmutator.core.util.OperatorNames;
import es.us.isa.httpmutator.core.util.PropertyManager;
import static es.us.isa.httpmutator.core.util.PropertyManager.readProperty;
import es.us.isa.httpmutator.core.util.RandomUtils;

/**
 * Class to manage mutation of JSON objects. Also works with JSON arrays.
 *
 * @author Alberto Martin-Lopez
 */
public class BodyMutator {

    private static final Logger logger = LogManager.getLogger(BodyMutator.class.getName());

    private ObjectMapper objectMapper;

    private boolean firstIteration; // True when mutateJSON is called the first time, false when it's called
                                    // recursively
    private int jsonProgress; // For Single Order Mutation (SOM): Size of JSON (sum of all object properties
                              // and array elements)
    private Integer elementIndex; // For SOM: Index of element (counting the whole JSON) to mutate
    private List<Integer> elementIndexes; // For SOM: Index of elements (counting the whole JSON) subject to be mutated
    private boolean mutationApplied; // For SOM: True if the mutation was applied. Used to stop iterating
    private boolean singleOrderActive; // True if single order mutation was used in the previous execution
    private JsonNode rootJson; // For getAllMutants(): root JSON where each property will be mutated in several
                               // ways

    private StringMutator stringMutator;
    private LongMutator longMutator;
    private DoubleMutator doubleMutator;
    private BooleanMutator booleanMutator;
    private NullMutator nullMutator;
    private ObjectMutator objectMutator;
    private ArrayMutator arrayMutator;

    public BodyMutator() {
        objectMapper = new ObjectMapper();
        resetJsonMutator();
        resetMutators();
    }

    // ========== Core method: streaming processing by path ==========

    /**
     * Process mutants by path in streaming fashion: returns all mutants for a given path at a time
     * This is the main public API, memory-safe and efficient
     * 
     * @param jsonNode The JsonNode to mutate
     * @param probability The probability based on which to apply each mutation
     * @param consumer Consumer to process mutants for each path
     */
    public void getAllMutants(JsonNode jsonNode, double probability, 
                                       Consumer<MutantGroup> consumer) {
        
        // Critical: maintain state management logic consistent with original
        boolean firstIterationOccurred = false;
        
        if (firstIteration) {
            setUpSingleOrderMutation(); // Setup single order mutation mode
            firstIteration = false;
            firstIterationOccurred = true;
            rootJson = jsonNode.deepCopy(); // Save root JSON copy
        }

        try {
            // Start recursive processing
            getAllMutants(jsonNode, "", probability, consumer);
        } finally {
            // 🧹 Ensure state is reset after processing completes
            if (firstIterationOccurred) {
                rootJson = null;
                firstIteration = true;
                resetMutators();
            }
        }
    }

    /**
     * String version of path-based processing
     */
    public void getAllMutants(String jsonString, double probability, 
                                       Consumer<MutantGroup> consumer) {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            getAllMutants(jsonNode, probability, consumer);
        } catch (IOException e) {
            logger.warn("The string passed as argument is not a JSON object.");
            throw new IllegalArgumentException("Invalid JSON string provided", e);
            // TODO: For invalid JSON, create a PathMutants containing the original string   
        }
    }

    /**
     * Internal recursive method: process mutants by path
     * This method is the core logic, similar to original getAllMutants but outputs by path grouping
     */
    private void getAllMutants(JsonNode jsonNode, String parentPath, 
                                        double probability, Consumer<MutantGroup> consumer) {
        
        List<Mutant> currentPathMutants = new ArrayList<>();
        AbstractMutator mutator = getMutator(jsonNode);

        // Handle root-level mutations for current path (corresponding to original firstIteration logic)
        if (parentPath.isEmpty() && mutator != null) {
            if (mutator instanceof AbstractObjectOrArrayMutator) {
                ((AbstractObjectOrArrayMutator) mutator).resetFirstLevelOperators();
            } else {
                throw new IllegalStateException("Mutator for root JSON must be an AbstractObjectOrArrayMutator");
            }
            
            for (AbstractOperator operator : mutator.getOperators().values()) {
                JsonNode jsonNodeCopy = jsonNode.deepCopy();
                if (RandomUtils.nextDouble() < probability && operator.isApplicable(jsonNodeCopy)) {
                    JsonNode mutant = (JsonNode) operator.mutate(jsonNodeCopy);
                    currentPathMutants.add(new Mutant(
                        "Body" + parentPath,
                        mutant,
                        mutator.getClass(),
                        operator.getClass()
                    ));
                }
            }
            
            ((AbstractObjectOrArrayMutator) mutator).resetOperators();
                        
            // If root path has mutations, send immediately to consumer
            if (!currentPathMutants.isEmpty()) {
                consumer.accept(new MutantGroup("Body" + parentPath, currentPathMutants));
            }
        }

        // Process child elements - this is the main memory optimization point
        Iterator<JsonNode> jsonIterator = jsonNode.elements();
        int i = 0;
        while (jsonIterator.hasNext()) {
            JsonNode element = jsonIterator.next();
            
            String propertyName = jsonNode.isObject() ? 
                Lists.newArrayList(jsonNode.fieldNames()).get(i) : null;
            Integer index = jsonNode.isArray() ? i : null;
            String currentPath = parentPath + "/" + (index == null ? propertyName : index);
            
            // Generate mutations for current element and process immediately
            List<Mutant> elementMutants = generateMutantsForElement(
                element, parentPath, propertyName, index, probability
            );
            
            if (!elementMutants.isEmpty()) {
                consumer.accept(new MutantGroup("Body" + currentPath, elementMutants));
            }

            // Recursively process container nodes
            if (element.isContainerNode()) {
                getAllMutants(element, currentPath, probability, consumer);
            }

            i++;
        }
    }

    /**
     * Generate mutants for a single element
     * This method is responsible for specific mutation generation logic
     */
    private List<Mutant> generateMutantsForElement(JsonNode element, String parentPath,
                                                  String propertyName, Integer index, 
                                                  double probability) {
        List<Mutant> mutants = new ArrayList<>();
        AbstractMutator mutator = getMutator(element);
        
        if (mutator != null) {
            for (AbstractOperator operator : mutator.getOperators().values()) {
                if (RandomUtils.nextDouble() < probability && operator.isApplicable(getNodeElement(element))) {
                    JsonNode mutatedJson = getMutatedJson(
                        rootJson, // Use saved root JSON copy
                        parentPath, 
                        propertyName, 
                        index, 
                        operator
                    );
                    mutants.add(new Mutant(
                        "Body" + parentPath + "/" + (index == null ? propertyName : index),
                        mutatedJson,
                        mutator.getClass(),
                        operator.getClass()
                    ));
                }
            }
        }
        
        return mutants;
    }

    /**
     * Based on an input JSON, apply all possible single order mutations on it
     * based on a certain probability and return one mutant per mutation (i.e.,
     * one mutant per property per operator).
     * 
     * @deprecated Use {@link #sreamAllMutants(JsonNode, double)} instead.
     *
     * @param jsonNode    The JsonNode to mutate.
     * @param probability The probability based on which to apply each mutation.
     * @return A list of mutated JsonNodes.
     */
    @Deprecated
    public List<Mutant> getAllMutants(JsonNode jsonNode, double probability) {
        return getAllMutants(jsonNode, "", probability);
    }

    @Deprecated
    public List<String> getAllMutants(String jsonString, double probability) {
        try {
            List<Mutant> nodeMutants = getAllMutants(objectMapper.readTree(jsonString), probability);
            return nodeMutants.stream().map(n -> {
                try {
                    return objectMapper.writeValueAsString(n.getMutatedNode());
                } catch (JsonProcessingException e) {
                    logger.warn("Some mutant could not be transformed to a string.");
                    return null;
                }
            })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.warn("The string passed as argument is not a JSON object.");
            return Collections.singletonList(jsonString);
        }
    }

    @Deprecated
    public List<Mutant> getAllMutants(JsonNode jsonNode) {
        return getAllMutants(jsonNode, "", 1);
    }

    @Deprecated
    public List<String> getAllMutants(String jsonString) {
        return getAllMutants(jsonString, 1);
    }

    /**
     *
     * @param jsonNode    JsonNode of which to obtain all possible mutants
     *                    (recursively)
     * @param parentPath  Pointer representing the location of the jsonNode
     *                    within the root (first-level) JSON
     * @param probability The probability with which a mutation is applied
     * @return All mutants generated based on the jsonNode passed
     */
    private List<Mutant> getAllMutants(JsonNode jsonNode, String parentPath, double probability) {
        List<Mutant> mutants = new ArrayList<>();
        AbstractMutator mutator = getMutator(jsonNode);

        boolean firstIterationOccurred = false; // Used to reset JsonMutator for next execution
        if (firstIteration) {
            setUpSingleOrderMutation(); // Mutants are only generated as single order mutations

            firstIteration = false;
            firstIterationOccurred = true;
            rootJson = jsonNode.deepCopy(); // Make a deep copy so that the input object is not altered

            // Get mutants of the first-level JSON
            if (mutator != null) {
                ((AbstractObjectOrArrayMutator) mutator).resetFirstLevelOperators();
                for (AbstractOperator operator : mutator.getOperators().values()) {
                    JsonNode jsonNodeCopy = jsonNode.deepCopy();
                    if (RandomUtils.nextDouble() < probability && operator.isApplicable(jsonNodeCopy)) {
                        JsonNode mutant = (JsonNode) operator.mutate(jsonNodeCopy);
                        mutants.add(
                                new Mutant(
                                        "Body" + parentPath,
                                        mutant,
                                        mutator.getClass(),
                                        operator.getClass()));
                    }
                }
                ((AbstractObjectOrArrayMutator) mutator).resetOperators();
            }
        }

        Iterator<JsonNode> jsonIterator = jsonNode.elements();
        int i = 0;
        while (jsonIterator.hasNext()) {
            JsonNode element = jsonIterator.next();
            mutator = getMutator(element);

            String propertyName = jsonNode.isObject() ? Lists.newArrayList(jsonNode.fieldNames()).get(i) : null;
            Integer index = jsonNode.isArray() ? i : null;

            if (mutator != null) { // Apply all operators, and generate one mutant for each
                for (AbstractOperator operator : mutator.getOperators().values()) {
                    if (operator instanceof ObjectRemoveObjectTypeElementOperator) {
                        System.err.println("");
                    }
                    if (RandomUtils.nextDouble() < probability && operator.isApplicable(getNodeElement(element))) {
                        JsonNode mutatedJson = getMutatedJson(rootJson, parentPath, propertyName, index, operator);
                        mutants.add(
                                new Mutant(
                                        "Body" + parentPath + "/" + (index == null ? propertyName : index),
                                        mutatedJson,
                                        mutator.getClass(),
                                        operator.getClass()));
                    }
                }
            }

            if (element.isContainerNode()) // Iterate over children and add its mutants
            {
                mutants.addAll(
                        getAllMutants(element, parentPath + "/" + (index == null ? propertyName : index), probability));
            }

            i++;
        }

        // At the end of the first iteration, reset JsonMutator for next call
        if (firstIterationOccurred) {
            rootJson = null;
            firstIteration = true;
            resetMutators();
        }

        return mutants;
    }

    /**
     *
     * @param jsonNode     JSON where to mutate some element (can be nested)
     * @param jsonPath     Pointer to the element which is the parent of the element
     *                     to mutate, e.g., "/prop1/arrayProp"
     * @param propertyName Name of the property to mutate, null if is an array
     *                     element
     * @param index        Index of the element to mutate, null if is an object
     *                     property
     * @param operator     Mutation operator to apply to the element
     * @return The modified jsonNode
     */
    private JsonNode getMutatedJson(JsonNode jsonNode, String jsonPath, String propertyName, Integer index,
            AbstractOperator operator) {
        JsonNode jsonNodeCopy = jsonNode.deepCopy();
        JsonNode element = jsonNodeCopy.at(jsonPath + "/" + (index == null ? propertyName : index));
        Object mutatedElement = operator.mutate(getNodeElement(element));
        insertElement(jsonNodeCopy.at(jsonPath), mutatedElement, propertyName, index);

        return jsonNodeCopy;
    }

    /**
     * Perform mutations on a JsonNode, either single or multiple order.
     *
     * @param jsonNode    The JsonNode to mutate.
     * @param singleOrder True if you want to apply only one mutation.
     * @return The mutated JsonNode.
     */
    public JsonNode mutateJson(JsonNode jsonNode, boolean singleOrder) {
        if (singleOrder) {
            if (!singleOrderActive) // If the last call to this function was with singleOrder=false, then...
            {
                setUpSingleOrderMutation(); // ... set up parameters for single order mutation...

            }
            singleOrderActive = true; // ... and keep track of this update for next function call
            return singleOrderMutation(jsonNode);
        } else {
            if (singleOrderActive) // If the last call to this function was with singleOrder=true, then...
            {
                resetMutators(); // ... set up parameters for multiple order mutation

            }
            singleOrderActive = false; // ... and keep track of this update for next function call
            return multipleOrderMutation(jsonNode);
        }
    }

    /**
     * Perform mutations on a JSON string, either single or multiple order.
     *
     * @param jsonString  The stringified JSON to mutate.
     * @param singleOrder True if you want to apply only one mutation.
     * @return The mutated stringified JSON.
     */
    public String mutateJson(String jsonString, boolean singleOrder) {
        try {
            return objectMapper.writeValueAsString(mutateJson(objectMapper.readTree(jsonString), singleOrder));
        } catch (IOException e) {
            logger.warn("The string passed as argument is not a JSON object.");
            return jsonString;
        }
    }

    /**
     * Auxiliary function to set up the JsonMutator for single order mutations.
     * Basically, probabilities of all mutators are set to 1, and for object and
     * array mutators, only one mutation is allowed and only one element can be
     * added or removed. That way, only one change is made at a time.
     */
    private void setUpSingleOrderMutation() {
        if (Boolean.parseBoolean(readProperty("operator.value.string.enabled"))) {
            stringMutator.setProb(1);
        }
        if (Boolean.parseBoolean(readProperty("operator.value.long.enabled"))) {
            longMutator.setProb(1);
        }
        if (Boolean.parseBoolean(readProperty("operator.value.double.enabled"))) {
            doubleMutator.setProb(1);
        }
        if (Boolean.parseBoolean(readProperty("operator.value.boolean.enabled"))) {
            booleanMutator.setProb(1);
        }
        if (Boolean.parseBoolean(readProperty("operator.value.null.enabled"))) {
            nullMutator.setProb(1);
        }
        if (Boolean.parseBoolean(readProperty("operator.object.enabled"))) {
            objectMutator.setProb(1);
            objectMutator.setMinMutations(1);
            objectMutator.setMaxMutations(1);
            ((ObjectAddElementOperator) objectMutator.getOperators().get(OperatorNames.ADD_ELEMENT))
                    .setMinAddedProperties(1);
            ((ObjectAddElementOperator) objectMutator.getOperators().get(OperatorNames.ADD_ELEMENT))
                    .setMaxAddedProperties(1);
            ((ObjectRemoveElementOperator) objectMutator.getOperators().get(OperatorNames.REMOVE_ELEMENT))
                    .setMinRemovedProperties(1);
            ((ObjectRemoveElementOperator) objectMutator.getOperators().get(OperatorNames.REMOVE_ELEMENT))
                    .setMaxRemovedProperties(1);
        }
        if (Boolean.parseBoolean(readProperty("operator.array.enabled"))) {
            arrayMutator.setProb(1);
            arrayMutator.setMinMutations(1);
            arrayMutator.setMaxMutations(1);
            ((ArrayAddElementOperator) arrayMutator.getOperators().get(OperatorNames.ADD_ELEMENT))
                    .setMinAddedElements(1);
            ((ArrayAddElementOperator) arrayMutator.getOperators().get(OperatorNames.ADD_ELEMENT))
                    .setMaxAddedElements(1);
            ((ArrayRemoveElementOperator) arrayMutator.getOperators().get(OperatorNames.REMOVE_ELEMENT))
                    .setMinRemovedElements(1);
            ((ArrayRemoveElementOperator) arrayMutator.getOperators().get(OperatorNames.REMOVE_ELEMENT))
                    .setMaxRemovedElements(1);
        }
    }

    /**
     * Reset all variables used by constructor and singleOrderMutation method.
     */
    private void resetJsonMutator() {
        firstIteration = true;
        jsonProgress = 0;
        singleOrderActive = elementIndex != null && elementIndex != -1;
        elementIndex = null;
        elementIndexes = new ArrayList<>();
        mutationApplied = false;
    }

    /**
     * Auxiliary function to set up the JsonMutator for multiple order
     * mutations. All mutators are re-instantiated, so that their properties are
     * reset according to the properties file.
     */
    private void resetMutators() {
        stringMutator = Boolean.parseBoolean(readProperty("operator.value.string.enabled")) ? new StringMutator()
                : null;
        longMutator = Boolean.parseBoolean(readProperty("operator.value.long.enabled")) ? new LongMutator() : null;
        doubleMutator = Boolean.parseBoolean(readProperty("operator.value.double.enabled")) ? new DoubleMutator()
                : null;
        booleanMutator = Boolean.parseBoolean(readProperty("operator.value.boolean.enabled")) ? new BooleanMutator()
                : null;
        nullMutator = Boolean.parseBoolean(readProperty("operator.value.null.enabled")) ? new NullMutator() : null;
        objectMutator = Boolean.parseBoolean(readProperty("operator.object.enabled")) ? new ObjectMutator() : null;
        arrayMutator = Boolean.parseBoolean(readProperty("operator.array.enabled")) ? new ArrayMutator() : null;
    }

    /**
     * Apply a single mutation to a JSON object. This is done in the following
     * way: First, the JSON is fully iterated over, keeping track of all the
     * elements that are subject to change based on the current configuration of
     * the JSONmutator. Then, a random element is picked. In a second iteration,
     * that element is looked for and mutated.
     *
     * @param jsonNode The JSON to mutate.
     * @return The mutated JSON.
     */
    private JsonNode singleOrderMutation(JsonNode jsonNode) {
        boolean firstIterationOccurred = false; // Used to reset the state of firstIteration attribute
        JsonNode jsonNodeCopy = jsonNode;
        int currentJsonProgress = 0; // Used to locate object property or array element to mutate (within current
                                     // jsonNode)
        if (firstIteration) {
            firstIteration = false;
            firstIterationOccurred = true;
            jsonNodeCopy = jsonNode.deepCopy(); // Make a deep copy so that the input object is not altered
            if (isElementSubjectToChange(jsonNodeCopy)) // If first-level JSON can be changed...
            {
                elementIndexes.add(-1); // ...add it to the list of property indexes

            }
        }

        if (elementIndex != null && elementIndex == -1 && !mutationApplied) { // If what has to be mutated is the actual
                                                                              // first-level JSON
            if (objectMutator != null && jsonNodeCopy.isObject()) {
                jsonNodeCopy = objectMutator.getMutatedNode(jsonNodeCopy);
            } else if (arrayMutator != null && jsonNodeCopy.isArray()) {
                jsonNodeCopy = arrayMutator.getMutatedNode(jsonNodeCopy);
            }
            mutationApplied = true;
        }

        Iterator<JsonNode> jsonIterator = jsonNodeCopy.elements();
        while (jsonIterator.hasNext()) { // Keep iterating the JSON...
            JsonNode subJsonNode = jsonIterator.next();
            if (elementIndex == null) { // If an element to mutate has not been selected yet
                if (isElementSubjectToChange(subJsonNode)) {
                    elementIndexes.add(jsonProgress); // Keep track of all properties that are subject to change

                }
            } else if (elementIndex == jsonProgress) { // If element to mutate is the current one
                if (jsonNodeCopy.isObject()) {
                    mutateElement(jsonNodeCopy, Lists.newArrayList(jsonNodeCopy.fieldNames()).get(currentJsonProgress),
                            null);
                } else if (jsonNodeCopy.isArray()) {
                    mutateElement(jsonNodeCopy, null, currentJsonProgress);
                }
                mutationApplied = true;
            }
            currentJsonProgress++; // Update iteration indexes
            jsonProgress++;
            if (mutationApplied) // If mutation was already applied, stop iterating
            {
                break;
            }
            if (subJsonNode.isContainerNode()) // Iterate over properties that are arrays or objects
            {
                singleOrderMutation(subJsonNode);
            }
        }

        // At the end of the first iteration, all elements subject to change will have
        // been saved, choose one to mutate:
        if (firstIterationOccurred) {
            if (elementIndexes.size() > 0) { // If at least one element can be mutated, do so
                elementIndex = elementIndexes.get(RandomUtils.nextInt(elementIndexes.size()));
                jsonProgress = 0; // Once elementIndex is set, start iterating again, looking for the property
                singleOrderMutation(jsonNodeCopy);
            }
            // Reset variables for the next time this function will be called:
            resetJsonMutator();
        }

        return jsonNodeCopy;
    }

    /**
     * Apply some random mutations to a JSON object. These mutations are applied
     * to sub-objects and sub-arrays recursively: add new properties, remove
     * existing properties, mutate existing properties (numbers, strings,
     * booleans, etc.), make existing properties null, leave existing objects
     * and arrays empty ({} or []), etc.
     *
     * @param jsonNode The JSON to mutate
     * @return The mutated JSON
     */
    private JsonNode multipleOrderMutation(JsonNode jsonNode) {
        boolean firstIterationOccurred = false; // Used to reset the state of firstIteration attribute
        JsonNode jsonNodeCopy = jsonNode;
        if (firstIteration) {
            firstIteration = false; // Set to false so that this block is not entered again when recursively calling
                                    // the function
            firstIterationOccurred = true; // Set to true so that firstIteration is reset to true at the end of this
                                           // call
            jsonNodeCopy = jsonNode.deepCopy(); // Make a deep copy so that the input object is not altered
            if (objectMutator != null && jsonNodeCopy.isObject()) {
                jsonNodeCopy = objectMutator.getMutatedNode(jsonNodeCopy);
            } else if (arrayMutator != null && jsonNodeCopy.isArray()) {
                jsonNodeCopy = arrayMutator.getMutatedNode(jsonNodeCopy);
            }
        }

        if (jsonNodeCopy.isObject()) { // If node is object
            Iterator<String> keysIterator = jsonNodeCopy.fieldNames();
            String propertyName;
            while (keysIterator.hasNext()) { // Iterate over each object property
                propertyName = keysIterator.next();
                mutateElement(jsonNodeCopy, propertyName, null); // (Possibly) mutate each property and...
                if (jsonNodeCopy.get(propertyName).isObject() || jsonNodeCopy.get(propertyName).isArray()) // ...if
                                                                                                           // property
                                                                                                           // is object
                                                                                                           // or
                                                                                                           // array...
                {
                    ((ObjectNode) jsonNodeCopy).replace(propertyName,
                            multipleOrderMutation(jsonNodeCopy.get(propertyName))); // ...recursively call this function

                }
            }
        } else if (jsonNodeCopy.isArray()) { // If node is array
            for (int arrayIndex = 0; arrayIndex < jsonNodeCopy.size(); arrayIndex++) { // Iterate over each array
                                                                                       // element
                mutateElement(jsonNodeCopy, null, arrayIndex); // (Possibly) mutate each element and...
                if (jsonNodeCopy.get(arrayIndex).isObject() || jsonNodeCopy.get(arrayIndex).isArray()) // ...if element
                                                                                                       // is object or
                                                                                                       // array...
                {
                    ((ArrayNode) jsonNodeCopy).set(arrayIndex, multipleOrderMutation(jsonNodeCopy.get(arrayIndex))); // ...recursively
                                                                                                                     // call
                                                                                                                     // this
                                                                                                                     // function

                }
            }
        }

        if (firstIterationOccurred) {
            firstIteration = true; // Reset for the next time this function will be called

        }
        return jsonNodeCopy;
    }

    /**
     * Tells whether a given JSON element (object, array, object property or
     * array element) is subject to change or not.
     *
     * @param element The element to check, passed as a JsonNode
     * @return true if the element can be changed, false otherwise
     */
    private boolean isElementSubjectToChange(JsonNode element) {
        return (longMutator != null && element.isIntegralNumber())
                || (doubleMutator != null && element.isFloatingPointNumber())
                || (stringMutator != null && element.isTextual())
                || (booleanMutator != null && element.isBoolean())
                || (nullMutator != null && element.isNull())
                || (objectMutator != null && element.isObject())
                || (arrayMutator != null && element.isArray());
    }

    /**
     * Receives an ObjectNode or ArrayNode and the property name or index
     * (respectively) of an element, (possibly) mutates the value of the element
     * and inserts the mutated value in the same position.
     */
    private void mutateElement(JsonNode jsonNode, String propertyName, Integer index) {
        boolean isObj = index == null; // If index==null, jsonNode is an object, otherwise it is an array
        JsonNode element = isObj ? jsonNode.get(propertyName) : jsonNode.get(index);
        if (longMutator != null && element.isIntegralNumber()) {
            if (isObj) {
                longMutator.mutate((ObjectNode) jsonNode, propertyName);
            } else {
                longMutator.mutate((ArrayNode) jsonNode, index);
            }
        } else if (doubleMutator != null && element.isFloatingPointNumber()) {
            if (isObj) {
                doubleMutator.mutate((ObjectNode) jsonNode, propertyName);
            } else {
                doubleMutator.mutate((ArrayNode) jsonNode, index);
            }
        } else if (stringMutator != null && element.isTextual()) {
            if (isObj) {
                stringMutator.mutate((ObjectNode) jsonNode, propertyName);
            } else {
                stringMutator.mutate((ArrayNode) jsonNode, index);
            }
        } else if (booleanMutator != null && element.isBoolean()) {
            if (isObj) {
                booleanMutator.mutate((ObjectNode) jsonNode, propertyName);
            } else {
                booleanMutator.mutate((ArrayNode) jsonNode, index);
            }
        } else if (nullMutator != null && element.isNull()) {
            if (isObj) {
                nullMutator.mutate((ObjectNode) jsonNode, propertyName);
            } else {
                nullMutator.mutate((ArrayNode) jsonNode, index);
            }
        } else if (objectMutator != null && element.isObject()) {
            if (isObj) {
                objectMutator.mutate((ObjectNode) jsonNode, propertyName);
            } else {
                objectMutator.mutate((ArrayNode) jsonNode, index);
            }
        } else if (arrayMutator != null && element.isArray()) {
            if (isObj) {
                arrayMutator.mutate((ObjectNode) jsonNode, propertyName);
            } else {
                arrayMutator.mutate((ArrayNode) jsonNode, index);
            }
        }
    }

    private AbstractMutator getMutator(JsonNode jsonNode) {
        if (jsonNode.isIntegralNumber()) {
            return longMutator;
        } else if (jsonNode.isFloatingPointNumber()) {
            return doubleMutator;
        } else if (jsonNode.isTextual()) {
            return stringMutator;
        } else if (jsonNode.isBoolean()) {
            return booleanMutator;
        } else if (jsonNode.isNull()) {
            return nullMutator;
        } else if (jsonNode.isObject()) {
            return objectMutator;
        } else if (jsonNode.isArray()) {
            return arrayMutator;
        } else {
            return null;
        }
    }

    /**
     * @param propertyName  Name of the property in the json-mutation.properties
     *                      file, e.g., "operator.value.double.enabled"
     * @param propertyValue Value to set that property with
     */
    public void setProperty(String propertyName, String propertyValue) {
        PropertyManager.setProperty(propertyName, propertyValue);
        resetMutators();
    }

    /**
     * Resets properties to the ones defined in json-mutation.properties
     */
    public void resetProperties() {
        PropertyManager.resetProperties();
        resetMutators();
    }
}
