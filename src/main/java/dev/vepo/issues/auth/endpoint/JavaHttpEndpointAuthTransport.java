package dev.vepo.issues.auth.endpoint;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class JavaHttpEndpointAuthTransport implements EndpointAuthHttpTransport {

    private static final Logger logger = LoggerFactory.getLogger(JavaHttpEndpointAuthTransport.class);

    @Override
    public String postJson(String url, String jsonBody, int connectTimeoutMs, int readTimeoutMs) {
        try {
            var client = HttpClient.newBuilder()
                                   .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                                   .build();
            var request = HttpRequest.newBuilder()
                                     .uri(URI.create(url))
                                     .timeout(Duration.ofMillis(readTimeoutMs))
                                     .header("Content-Type", "application/json")
                                     .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                                     .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                logger.debug("Endpoint auth returned status={}", response.statusCode());
                throw new EndpointAuthHttpException(response.statusCode(),
                                                    "Endpoint auth failed with status %d".formatted(response.statusCode()));
            }
            return response.body() == null ? "" : response.body();
        } catch (EndpointAuthHttpException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EndpointAuthHttpException("Endpoint auth interrupted", e);
        } catch (Exception e) {
            throw new EndpointAuthHttpException("Endpoint auth call failed", e);
        }
    }
}
