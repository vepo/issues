package dev.vepo.issues.auth.endpoint;

/**
 * Raised when the external credential HTTP call does not succeed with 200.
 */
public class EndpointAuthHttpException extends RuntimeException {

    private final int statusCode;

    public EndpointAuthHttpException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public EndpointAuthHttpException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
    }

    public int statusCode() {
        return statusCode;
    }
}
