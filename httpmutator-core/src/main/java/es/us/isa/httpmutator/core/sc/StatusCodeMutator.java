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
        addOperatorIfEnabled(
                "operator.sc.replaceWith20x.enabled",
                OperatorNames.REPLACE_WITH_20X,
                StatusCodeReplacementWith20XOperator::new);
        addOperatorIfEnabled(
                "operator.sc.replaceWith40x.enabled",
                OperatorNames.REPLACE_WITH_40X,
                StatusCodeReplacementWith40XOperator::new);
        addOperatorIfEnabled(
                "operator.sc.replaceWith50x.enabled",
                OperatorNames.REPLACE_WITH_50X,
                StatusCodeReplacementWith50XOperator::new);
    }

    public void getAllMutants(int statusCode, double probability, Consumer<MutantGroup> consumer) {
        List<Mutant> mutants = new ArrayList<>();
        for (AbstractOperator operator : operators.values()) {
            JsonNode mutant = JsonNodeFactory.instance.numberNode((Integer) operator.mutate(statusCode));
            mutants.add(new Mutant("Status Code", mutant, this.getClass(), operator.getClass()));
        }
        if (!mutants.isEmpty()) {
            consumer.accept(new MutantGroup("Status Code", mutants));
        }
    }
}
