package dev.vepo.issues.auth;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * Forces ENDPOINT provider so password ops and capabilities can be asserted
 * without a live IdP.
 */
public class EndpointAuthTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("auth.provider", "endpoint",
                      "auth.endpoint.url", "http://127.0.0.1:9");
    }
}
