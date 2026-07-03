package dev.vepo.issues.ticket.history;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.issues.ticket.TicketTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class GetTicketHistoryEndpointTest {

    private TicketTestFixtures fixtures;

    @BeforeEach
    void setup() {
        fixtures = TicketTestFixtures.create();
    }

    @Test
    @DisplayName("It should return structured ticket history in descending order")
    void shouldGetTicketHistoryTest() {
        given().header(fixtures.userAuthenticatedHeader())
               .contentType(ContentType.JSON)
               .accept(ContentType.JSON)
               .when()
               .body("""
                     {
                         "title": "Updated Title for History",
                         "description": "Updated description for history",
                         "categoryId": %d,
                         "priority": "MEDIUM"
                     }""".formatted(fixtures.feature().getId()))
               .post("/api/tickets/" + fixtures.ticket().id())
               .then()
               .statusCode(200);

        List<?> history = given().header(fixtures.userAuthenticatedHeader())
                                 .accept(ContentType.JSON)
                                 .when()
                                 .get("/api/tickets/" + fixtures.ticket().id() + "/history")
                                 .then()
                                 .statusCode(200)
                                 .body("$.size()", greaterThan(0))
                                 .body("action", hasItem("FIELD_CHANGED"))
                                 .extract()
                                 .jsonPath()
                                 .getList("");

        @SuppressWarnings("unchecked")
        var titleChange = history.stream()
                                 .map(entry -> (Map<String, Object>) entry)
                                 .filter(e -> "title".equals(e.get("field")))
                                 .findFirst()
                                 .orElseThrow();
        assertEquals(fixtures.ticket().title(), titleChange.get("oldValue"));
        assertEquals("Updated Title for History", titleChange.get("newValue"));
    }
}
