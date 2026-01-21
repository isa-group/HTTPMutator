package es.us.isa.httpmutator.core.util;

/**
 * Name of all the possible mutation operators to apply to the JSON elements.
 * NOTE: These names should be the same as those used in the properties file
 * when assigning weights to the mutation operators of a mutator (e.g.
 * "operator.value.string.weight.replace").
 */
public class OperatorNames {
    public static final String REPLACE = "replace";
    public static final String MUTATE = "mutate";
    public static final String CHANGE_TYPE = "changeType";
    public static final String NULL = "null";
    public static final String BOUNDARY = "boundary";
    public static final String ADD_ELEMENT = "addElement";
    public static final String REMOVE_ELEMENT = "removeElement";
    public static final String REMOVE_OBJECT_ELEMENT = "removeObjectElement";
    public static final String EMPTY = "empty";
    public static final String DISORDER_ELEMENTS = "disorderElements";
    public static final String ADD_SPECIAL_CHARACTERS = "addSpecialCharacters";
    // status code operators
    public static final String REPLACE_WITH_20X = "replaceWith20x";
    public static final String REPLACE_WITH_40X = "replaceWith40x";
    public static final String REPLACE_WITH_50X = "replaceWith50x";

}
