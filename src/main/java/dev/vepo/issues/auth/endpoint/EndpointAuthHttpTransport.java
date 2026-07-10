package dev.vepo.issues.auth.endpoint;

/**
 * Thin HTTP transport for the external credential endpoint (mockable in unit
 * tests).
 */
public interface EndpointAuthHttpTransport {

    /**
     * @return response body when status is 200
     * @throws EndpointAuthHttpException when status is not 200 or the call fails
     */
    String postJson(String url, String jsonBody, int connectTimeoutMs, int readTimeoutMs);
}
