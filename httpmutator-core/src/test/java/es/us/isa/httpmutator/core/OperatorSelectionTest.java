package es.us.isa.httpmutator.core;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import es.us.isa.httpmutator.core.body.array.ArrayMutator;
import es.us.isa.httpmutator.core.body.object.ObjectMutator;
import es.us.isa.httpmutator.core.body.value.boolean0.BooleanMutator;
import es.us.isa.httpmutator.core.body.value.double0.DoubleMutator;
import es.us.isa.httpmutator.core.body.value.long0.LongMutator;
import es.us.isa.httpmutator.core.body.value.null0.NullMutator;
import es.us.isa.httpmutator.core.body.value.string0.StringMutator;
import es.us.isa.httpmutator.core.headers.charset.CharsetMutator;
import es.us.isa.httpmutator.core.headers.location.LocationMutator;
import es.us.isa.httpmutator.core.headers.mediaType.MediaTypeMutator;
import es.us.isa.httpmutator.core.sc.StatusCodeMutator;
import es.us.isa.httpmutator.core.util.PropertyManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class OperatorSelectionTest {

    @After
    public void resetProperties() {
        PropertyManager.resetProperties();
    }

    @Test
    public void statusCodeOperatorsCanAllBeDisabled() {
        assertAllOperatorsCanBeDisabled(
                StatusCodeMutator::new,
                "operator.sc.replaceWith20x.enabled",
                "operator.sc.replaceWith40x.enabled",
                "operator.sc.replaceWith50x.enabled");
    }

    @Test
    public void headerOperatorsCanAllBeDisabled() {
        assertAllOperatorsCanBeDisabled(
                MediaTypeMutator::new,
                "operator.header.mediaType.replace.enabled",
                "operator.header.mediaType.null.enabled");
        assertAllOperatorsCanBeDisabled(
                CharsetMutator::new,
                "operator.header.charset.replace.enabled",
                "operator.header.charset.null.enabled");
        assertAllOperatorsCanBeDisabled(
                LocationMutator::new,
                "operator.header.location.mutate.enabled",
                "operator.header.location.null.enabled");
    }

    @Test
    public void primitiveValueOperatorsCanAllBeDisabled() {
        assertAllOperatorsCanBeDisabled(
                LongMutator::new,
                "operator.value.long.replace.enabled",
                "operator.value.long.null.enabled",
                "operator.value.long.changeType.enabled");
        assertAllOperatorsCanBeDisabled(
                DoubleMutator::new,
                "operator.value.double.replace.enabled",
                "operator.value.double.null.enabled",
                "operator.value.double.changeType.enabled");
        assertAllOperatorsCanBeDisabled(
                StringMutator::new,
                "operator.value.string.replace.enabled",
                "operator.value.string.addSpecialCharacters.enabled",
                "operator.value.string.boundary.enabled",
                "operator.value.string.null.enabled",
                "operator.value.string.changeType.enabled");
        assertAllOperatorsCanBeDisabled(
                BooleanMutator::new,
                "operator.value.boolean.mutate.enabled",
                "operator.value.boolean.null.enabled",
                "operator.value.boolean.changeType.enabled");
        assertAllOperatorsCanBeDisabled(
                NullMutator::new,
                "operator.value.null.changeType.enabled");
    }

    @Test
    public void containerOperatorsCanAllBeDisabled() {
        assertAllOperatorsCanBeDisabled(
                ObjectMutator::new,
                "operator.object.removeElement.enabled",
                "operator.object.removeObjectElement.enabled",
                "operator.object.addElement.enabled",
                "operator.object.null.enabled",
                "operator.object.changeType.enabled");
        assertAllOperatorsCanBeDisabled(
                ArrayMutator::new,
                "operator.array.removeElement.enabled",
                "operator.array.empty.enabled",
                "operator.array.addElement.enabled",
                "operator.array.disorderElements.enabled",
                "operator.array.null.enabled",
                "operator.array.changeType.enabled");
    }

    @Test
    public void disablingOneOperatorLeavesOtherOperatorsEnabled() {
        PropertyManager.setProperty("operator.sc.replaceWith20x.enabled", "false");

        StatusCodeMutator mutator = new StatusCodeMutator();

        Assert.assertFalse(mutator.getOperators().containsKey("replaceWith20x"));
        Assert.assertTrue(mutator.getOperators().containsKey("replaceWith40x"));
        Assert.assertTrue(mutator.getOperators().containsKey("replaceWith50x"));
    }

    @Test
    public void statusCodeMutatorDoesNotEmitEmptyGroup() {
        disable(
                "operator.sc.replaceWith20x.enabled",
                "operator.sc.replaceWith40x.enabled",
                "operator.sc.replaceWith50x.enabled");
        StatusCodeMutator mutator = new StatusCodeMutator();
        AtomicBoolean invoked = new AtomicBoolean();

        mutator.getAllMutants(200, 1.0, group -> invoked.set(true));

        Assert.assertFalse(invoked.get());
    }

    @Test
    public void disabledContainerOperatorsLeaveRootContainersUnchanged() {
        disable(
                "operator.object.removeElement.enabled",
                "operator.object.removeObjectElement.enabled",
                "operator.object.addElement.enabled",
                "operator.object.null.enabled",
                "operator.object.changeType.enabled");
        ObjectMutator objectMutator = new ObjectMutator();
        ObjectNode object = JsonNodeFactory.instance.objectNode().put("id", 1);

        Assert.assertEquals(object, objectMutator.getMutatedNode(object.deepCopy()));

        PropertyManager.resetProperties();
        disable(
                "operator.array.removeElement.enabled",
                "operator.array.empty.enabled",
                "operator.array.addElement.enabled",
                "operator.array.disorderElements.enabled",
                "operator.array.null.enabled",
                "operator.array.changeType.enabled");
        ArrayMutator arrayMutator = new ArrayMutator();
        ArrayNode array = JsonNodeFactory.instance.arrayNode().add(1);

        Assert.assertEquals(array, arrayMutator.getMutatedNode(array.deepCopy()));
    }

    private void assertAllOperatorsCanBeDisabled(
            Supplier<? extends AbstractMutator> mutatorSupplier,
            String... enabledProperties) {
        PropertyManager.resetProperties();
        disable(enabledProperties);

        AbstractMutator mutator = mutatorSupplier.get();

        Assert.assertTrue(
                "Expected no operators after disabling " + String.join(", ", enabledProperties),
                mutator.getOperators().isEmpty());
    }

    private void disable(String... enabledProperties) {
        for (String property : enabledProperties) {
            PropertyManager.setProperty(property, "false");
        }
    }
}
