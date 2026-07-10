package dev.vepo.issues.workflow;

public record StatusWipResponse(String status, int wipLimit) {

    public static StatusWipResponse load(WorkflowWipLimit wipLimit) {
        return new StatusWipResponse(wipLimit.getStatus().getName(), wipLimit.getWipLimit());
    }
}
