package es.us.isa.httpmutator.core.headers;

import java.util.List;

import org.junit.Test;

import es.us.isa.httpmutator.core.model.Mutant;

public class HeaderMutatorTest {
    private final HeaderMutator headerMutator = new HeaderMutator();

    // @Test
    // public void test1() {
    //     System.out.println("Test case with not-empty media type and not-empty charset");
    //     String inputJson1 = "{\"Content-Type\": \"text/html; charset=UTF-8\"}";
    //     System.out.println("Input: " + inputJson1);
    //     List<Mutant> mutants = headerMutator.getAllMutants(inputJson1);
    //     for (Mutant mutant : mutants) {
    //         System.out.println("Mutant: " + mutant);
    //     }
    // }

    // @Test
    // public void test2() {
    //     System.out.println("Test case with not-empty media type and empty charset");
    //     String inputJson2 = "{\"Content-Type\": \"text/html\"}";
    //     System.out.println("Input: " + inputJson2);
    //     List<Mutant> mutants = headerMutator.getAllMutants(inputJson2);
    //     for (Mutant mutant : mutants) {
    //         System.out.println("Mutant: " + mutant);
    //     }
    // }

    // @Test
    // public void test3() {
    //     System.out.println("Test case with empty media type and not-empty charset");
    //     String inputJson3 = "{\"Content-Type\": \"charset=UTF-8\"}";
    //     System.out.println("Input: " + inputJson3);
    //     List<Mutant> mutants = headerMutator.getAllMutants(inputJson3);
    //     for (Mutant mutant : mutants) {
    //         System.out.println("Mutant: " + mutant);
    //     }
    // }

    // @Test
    // public void test4() {
    //     System.out.println("Test case with additional info in content type");
    //     String inputJson4 = "{\"Content-Type\": \"text/html;version=1;charset=utf8\"}";
    //     System.out.println("Input: " + inputJson4);
    //     List<Mutant> mutants = headerMutator.getAllMutants(inputJson4);
    //     for (Mutant mutant : mutants) {
    //         System.out.println("Mutant: " + mutant);
    //     }
    // }

    // @Test
    // public void test5() {
    //     System.out.println("Test case with location");
    //     String inputJson5 = "{\"Location\": \"http://www.google.com\"}";
    //     System.out.println("Input: " + inputJson5);
    //     List<Mutant> mutants = headerMutator.getAllMutants(inputJson5);
    //     for (Mutant mutant : mutants) {
    //         System.out.println("Mutant: " + mutant);
    //     }
    // }

    // @Test
    // public void test6() {
    //     System.out.println("Test case with content type and location");
    //     String inputJson6 = "{\"Content-Type\": \"text/html; charset=UTF-8\", \"Location\": \"http://www.google.com\"}";
    //     System.out.println("Input: " + inputJson6);
    //     List<Mutant> mutants = headerMutator.getAllMutants(inputJson6);
    //     for (Mutant mutant : mutants) {
    //         System.out.println("Mutant: " + mutant);
    //     }
    // }
}
