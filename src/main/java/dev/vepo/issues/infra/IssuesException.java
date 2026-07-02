package dev.vepo.issues.infra;

public class IssuesException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public IssuesException(String message) {
        super(message);
    }

    public IssuesException(String message, Throwable cause) {
        super(message, cause);
    }

    public IssuesException(Throwable cause) {
        super(cause);
    }

}
