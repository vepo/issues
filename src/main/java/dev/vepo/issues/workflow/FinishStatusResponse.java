package dev.vepo.issues.workflow;

public record FinishStatusResponse(String status, FinishOutcome outcome) {
    public static FinishStatusResponse load(WorkflowFinishStatus finishStatus) {
        return new FinishStatusResponse(finishStatus.getStatus().getName(), finishStatus.getOutcome());
    }
}
