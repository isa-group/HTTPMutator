package es.us.isa.httpmutator.examples.restassured;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.restassured.RestAssured;
import io.restassured.config.JsonConfig;
import io.restassured.path.json.config.JsonPathConfig;

import static io.restassured.RestAssured.given;
import static io.restassured.config.RedirectConfig.redirectConfig;
import static org.hamcrest.Matchers.equalTo;

/**
 * This class is adapted from an EvoMaster-generated test suite
 *
 * In this examples module, we reuse the same RestAssured tests
 * as a "baseline" version WITHOUT HttpMutator, so that we can contrast it
 * with a minimally modified version that adds HttpMutator support.
 */
public class WithoutHttpMutatorTest {

    private static String baseUrlOfSut = "http://localhost:43103";

    @BeforeAll
    public static void initClass() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.urlEncodingEnabled = false;
        RestAssured.config = RestAssured.config()
                .jsonConfig(JsonConfig.jsonConfig()
                        .numberReturnType(JsonPathConfig.NumberReturnType.DOUBLE))
                .redirect(redirectConfig().followRedirects(false));
    }

    /**
     * Calls:
     * (200) GET:/projects
     */
    @Test
    @Timeout(60)
    public void test_0() {
        given().accept("application/json")
                .get(baseUrlOfSut + "/projects?"
                        + "offset=580&"
                        + "q=7TenZT")
                .then()
                .statusCode(200)
                .assertThat()
                .contentType("application/json")
                .body("size()", equalTo(0));
    }

    /**
     * Calls:
     * (200) GET:/projects
     */
    @Test
    @Timeout(60)
    public void test_1() {
        given().accept("application/json")
                .get(baseUrlOfSut + "/projects?"
                        + "limit=365&"
                        + "q=vmVXdJkjBlSs1E")
                .then()
                .statusCode(200)
                .assertThat()
                .contentType("application/json")
                .body("size()", equalTo(0));
    }

    /**
     * Calls:
     * (200) GET:/projects
     */
    @Test
    @Timeout(60)
    public void test_2() {
        given().accept("application/json")
                .get(baseUrlOfSut + "/projects?"
                        + "limit=228&"
                        + "offset=909&"
                        + "sortBy=Z4Q0lxF")
                .then()
                .statusCode(200)
                .assertThat()
                .contentType("application/json")
                .body("size()", equalTo(0));
    }

    /**
     * Calls:
     * (200) GET:/projects
     */
    @Test
    @Timeout(60)
    public void test_3() {
        given().accept("application/json")
                .get(baseUrlOfSut + "/projects?"
                        + "organizations=&"
                        + "limit=62&"
                        + "offset=673&"
                        + "q=VM2gh")
                .then()
                .statusCode(200)
                .assertThat()
                .contentType("application/json")
                .body("size()", equalTo(0));
    }

    /**
     * Calls:
     * (200) GET:/projects
     */
    @Test
    @Timeout(60)
    public void test_4() {
        given().accept("application/json")
                .get(baseUrlOfSut + "/projects?"
                        + "limit=714&"
                        + "offset=505&"
                        + "sortBy=q28qJ&"
                        + "q=VnqXMFohUX&"
                        + "language=R3cNd6TNB")
                .then()
                .statusCode(200)
                .assertThat()
                .contentType("application/json")
                .body("size()", equalTo(0));
    }
}