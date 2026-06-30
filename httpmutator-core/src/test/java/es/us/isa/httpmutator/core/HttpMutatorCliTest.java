package es.us.isa.httpmutator.core;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class HttpMutatorCliTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void parsesPropertiesFileArgument() throws Exception {
        Path input = temporaryFolder.newFile("traffic.jsonl").toPath();
        Path properties = temporaryFolder.newFile("custom.properties").toPath();
        Files.write(properties,
                "operator.sc.replaceWith40x.enabled=false\n".getBytes(StandardCharsets.UTF_8));

        Object config = parse(
                "--input", input.toString(),
                "--format", "jsonl",
                "--properties", properties.toString());

        Assert.assertEquals(properties, getPathField(config, "propertiesFile"));
    }

    @Test
    public void rejectsUnreadablePropertiesFileArgument() throws Exception {
        Path input = temporaryFolder.newFile("traffic.jsonl").toPath();
        Path missingProperties = temporaryFolder.getRoot().toPath().resolve("missing.properties");

        try {
            parse(
                    "--input", input.toString(),
                    "--format", "jsonl",
                    "--properties", missingProperties.toString());
            Assert.fail("Expected missing properties file to be rejected");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("--properties"));
            Assert.assertTrue(e.getMessage().contains(missingProperties.toString()));
        }
    }

    private static Object parse(String... args) throws Exception {
        Class<?> cliConfigClass = Class.forName("es.us.isa.httpmutator.core.HttpMutatorCli$CliConfig");
        Method parse = cliConfigClass.getDeclaredMethod("parse", String[].class);
        parse.setAccessible(true);
        try {
            return parse.invoke(null, (Object) args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) cause;
            }
            throw e;
        }
    }

    private static Path getPathField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (Path) field.get(target);
    }
}
