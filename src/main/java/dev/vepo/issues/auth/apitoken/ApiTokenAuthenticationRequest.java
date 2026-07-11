package dev.vepo.issues.auth.apitoken;

import io.quarkus.security.identity.request.BaseAuthenticationRequest;

public class ApiTokenAuthenticationRequest extends BaseAuthenticationRequest {

    private final String token;

    public ApiTokenAuthenticationRequest(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
