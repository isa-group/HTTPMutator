package es.us.isa.httpmutator.core.sc;

import static es.us.isa.httpmutator.core.util.PropertyManager.readProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import es.us.isa.httpmutator.core.AbstractMutator;
import es.us.isa.httpmutator.core.AbstractOperator;
import es.us.isa.httpmutator.core.model.Mutant;
import es.us.isa.httpmutator.core.model.MutantGroup;
import es.us.isa.httpmutator.core.sc.operator.StatusCodeReplacementWith20XOperator;
import es.us.isa.httpmutator.core.sc.operator.StatusCodeReplacementWith40XOperator;
import es.us.isa.httpmutator.core.sc.operator.StatusCodeReplacementWith50XOperator;
import es.us.isa.httpmutator.core.util.OperatorNames;

public class StatusCodeMutator extends AbstractMutator {
    public StatusCodeMutator() {
        super();
        prob = Float.parseFloat(readProperty("operator.sc.prob"));
        operators.put(OperatorNames.REPLACE_WITH_20X, new StatusCodeReplacementWith20XOperator());
        operators.put(OperatorNames.REPLACE_WITH_40X, new StatusCodeReplacementWith40XOperator());
        operators.put(OperatorNames.REPLACE_WITH_50X, new StatusCodeReplacementWith50XOperator());
    }

    public void getAllMutants(int statusCode, double probability, Consumer<MutantGroup> consumer) {
        List<Mutant> mutants = new ArrayList<>();
        for (AbstractOperator operator : operators.values()) {
            JsonNode mutant = JsonNodeFactory.instance.numberNode((Integer) operator.mutate(statusCode));
            mutants.add(new Mutant("Status Code", mutant, this.getClass(), operator.getClass()));
        }
        consumer.accept(new MutantGroup("Status Code", mutants));
    }
}
