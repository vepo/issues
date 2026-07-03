package dev.vepo.issues.ticket.search.saved;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.ticket.TicketTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class SavedQueryEndpointTest {

    private TicketTestFixtures fixtures;

    @BeforeEach
    void setup() {
        fixtures = TicketTestFixtures.create();
    }

    @Test
    @DisplayName("Should create, list, and delete saved query for owner")
    void shouldManageSavedQueryAsOwner() {
        var created = given().header(fixtures.pmAuthenticatedHeader())
                             .contentType(ContentType.JSON)
                             .accept(ContentType.JSON)
                             .body("""
                                   {
                                       "name": "My open tickets",
                                       "query": "title ~ \\"Test\\"",
                                       "showAtHome": true
                                   }""")
                             .when()
                             .post("/api/saved-queries")
                             .then()
                             .statusCode(201)
                             .body("name", equalTo("My open tickets"))
                             .body("showAtHome", equalTo(true))
                             .extract()
                             .as(SavedQueryResponse.class);

        given().header(fixtures.pmAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .get("/api/saved-queries")
               .then()
               .statusCode(200)
               .body("$.size()", org.hamcrest.Matchers.greaterThan(0));

        given().header(fixtures.pmAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .get("/api/saved-queries/by-slug/" + created.slug())
               .then()
               .statusCode(200)
               .body("savedQuery.slug", equalTo(created.slug()))
               .body("tickets", notNullValue());

        given().header(fixtures.pmAuthenticatedHeader())
               .when()
               .delete("/api/saved-queries/" + created.id())
               .then()
               .statusCode(204);
    }

    @Test
    @DisplayName("Should clone another user saved query")
    void shouldCloneSavedQueryFromOtherUser() {
        var created = given().header(fixtures.pmAuthenticatedHeader())
                             .contentType(ContentType.JSON)
                             .body("""
                                   {
                                       "name": "Shared query",
                                       "query": "title ~ \\"Test\\"",
                                       "showAtHome": false
                                   }""")
                             .when()
                             .post("/api/saved-queries")
                             .then()
                             .statusCode(201)
                             .extract()
                             .as(SavedQueryResponse.class);

        given().header(fixtures.userAuthenticatedHeader())
               .accept(ContentType.JSON)
               .when()
               .post("/api/saved-queries/" + created.id() + "/clone")
               .then()
               .statusCode(201)
               .body("name", equalTo("Shared query (cópia)"))
               .body("ownerId", notNullValue());
    }
}
