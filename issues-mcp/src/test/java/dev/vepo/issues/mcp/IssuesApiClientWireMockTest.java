package dev.vepo.issues.mcp;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@QuarkusTestResource(IssuesApiWireMockResource.class)
class IssuesApiClientWireMockTest {

    @Inject
    @RestClient
    IssuesApiClient issuesApiClient;

    @BeforeEach
    void resetStubs() {
        IssuesApiWireMockResource.server.resetAll();
    }

    @Test
    void shouldForwardBearerTokenWhenGettingTicketContext() {
        IssuesApiWireMockResource.server.stubFor(get(urlEqualTo("/tickets/42/context"))
                                                                                       .withHeader("Authorization", equalTo("Bearer iss_pat_test"))
                                                                                       .willReturn(aResponse()
                                                                                                              .withStatus(200)
                                                                                                              .withHeader("Content-Type", "application/json")
                                                                                                              .withBody("{\"ticket\":{\"id\":42}}")));

        var body = issuesApiClient.getTicketContext("Bearer iss_pat_test", 42L);

        assertTrue(body.contains("\"id\":42"));
    }

    @Test
    void shouldPostCommentBody() {
        IssuesApiWireMockResource.server.stubFor(post(urlEqualTo("/tickets/7/comments"))
                                                                                        .withHeader("Authorization", equalTo("Bearer iss_sat_bot"))
                                                                                        .willReturn(aResponse()
                                                                                                               .withStatus(201)
                                                                                                               .withHeader("Content-Type", "application/json")
                                                                                                               .withBody("{\"id\":1,\"content\":\"hello\"}")));

        var body = issuesApiClient.addComment("Bearer iss_sat_bot", 7L, "{\"content\":\"hello\"}");

        assertTrue(body.contains("hello"));
        IssuesApiWireMockResource.server.verify(postRequestedFor(urlPathEqualTo("/tickets/7/comments"))
                                                                                                      .withHeader("Authorization",
                                                                                                                  equalTo("Bearer iss_sat_bot")));
    }
}
