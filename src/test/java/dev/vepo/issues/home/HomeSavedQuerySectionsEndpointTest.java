package dev.vepo.issues.home;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.ticket.TicketTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class HomeSavedQuerySectionsEndpointTest {

    private TicketTestFixtures fixtures;

    @BeforeEach
    void setup() {
        fixtures = TicketTestFixtures.create();
    }

    @Test
    @DisplayName("Should return show-at-home saved query sections in one response")
    void shouldListHomeSavedQuerySections() {
        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Home section query",
                         "query": "title ~ \\"Test\\"",
                         "showAtHome": true
                     }""")
               .when()
               .post("/api/saved-queries")
               .then()
               .statusCode(201);

        given().header(fixtures.pmAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .get("/api/home/saved-queries")
               .then()
               .statusCode(200)
               .body("$.size()", greaterThanOrEqualTo(1))
               .body("[0].savedQuery.showAtHome", equalTo(true))
               .body("[0].tickets", notNullValue());
    }

    @Test
    @DisplayName("Should exclude saved queries without show-at-home flag")
    void shouldExcludeQueriesNotMarkedForHome() {
        given().header(fixtures.pmAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .body("""
                     {
                         "name": "Hidden query",
                         "query": "title ~ \\"Test\\"",
                         "showAtHome": false
                     }""")
               .when()
               .post("/api/saved-queries")
               .then()
               .statusCode(201);

        given().header(fixtures.pmAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .get("/api/home/saved-queries")
               .then()
               .statusCode(200)
               .body("findAll { it.savedQuery.name == 'Hidden query' }.size()", equalTo(0));
    }
}
