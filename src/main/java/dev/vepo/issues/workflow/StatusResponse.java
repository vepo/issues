package dev.vepo.issues.workflow;

public record StatusResponse(long id, String name) {

    public static StatusResponse load(WorkflowStatus status) {
        return new StatusResponse(status.getId(), status.getName());
    }
}
