package dev.vepo.issues.auth.endpoint;

import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.vepo.issues.auth.AuthFailures;
import dev.vepo.issues.auth.AuthProvider;
import dev.vepo.issues.auth.CredentialAuthenticator;
import dev.vepo.issues.auth.VerifiedIdentity;
import dev.vepo.issues.user.Role;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotAuthorizedException;

@ApplicationScoped
public class EndpointCredentialAuthenticator implements CredentialAuthenticator {

    private static final Logger logger = LoggerFactory.getLogger(EndpointCredentialAuthenticator.class);

    private final EndpointAuthHttpTransport httpTransport;
    private final ObjectMapper objectMapper;
    private final String endpointUrl;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    @Inject
    public EndpointCredentialAuthenticator(EndpointAuthHttpTransport httpTransport,
                                           ObjectMapper objectMapper,
                                           @ConfigProperty(name = "auth.endpoint.url") Optional<String> endpointUrl,
                                           @ConfigProperty(name = "auth.endpoint.connect-timeout-ms", defaultValue = "5000") int connectTimeoutMs,
                                           @ConfigProperty(name = "auth.endpoint.read-timeout-ms", defaultValue = "10000") int readTimeoutMs) {
        this.httpTransport = httpTransport;
        this.objectMapper = objectMapper;
        this.endpointUrl = endpointUrl.orElse("");
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    EndpointCredentialAuthenticator(EndpointAuthHttpTransport httpTransport,
                                    ObjectMapper objectMapper,
                                    String endpointUrl) {
        this.httpTransport = httpTransport;
        this.objectMapper = objectMapper;
        this.endpointUrl = endpointUrl == null ? "" : endpointUrl;
        this.connectTimeoutMs = 5000;
        this.readTimeoutMs = 10000;
    }

    @Override
    public AuthProvider provider() {
        return AuthProvider.ENDPOINT;
    }

    @Override
    public VerifiedIdentity authenticate(String email, String password) {
        if (endpointUrl == null || endpointUrl.isBlank()) {
            logger.warn("ENDPOINT authentication attempted but auth.endpoint.url is empty");
            throw AuthFailures.invalidCredentials();
        }
        try {
            var requestJson = objectMapper.writeValueAsString(new EndpointCredentialRequest(email, password));
            var body = httpTransport.postJson(endpointUrl, requestJson, connectTimeoutMs, readTimeoutMs);
            var response = objectMapper.readValue(body, EndpointCredentialResponse.class);
            if (response.email() == null || response.email().isBlank()) {
                throw AuthFailures.invalidCredentials();
            }
            return new VerifiedIdentity(response.email(),
                                        response.name(),
                                        response.username(),
                                        Set.of(Role.USER),
                                        AuthProvider.ENDPOINT,
                                        false);
        } catch (NotAuthorizedException e) {
            throw e;
        } catch (EndpointAuthHttpException e) {
            logger.debug("Endpoint authentication failed for email={} status={}", email, e.statusCode());
            throw AuthFailures.invalidCredentials();
        } catch (Exception e) {
            logger.debug("Endpoint authentication failed for email={}", email, e);
            throw AuthFailures.invalidCredentials();
        }
    }
}
