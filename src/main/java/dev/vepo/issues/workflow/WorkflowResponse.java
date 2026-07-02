package dev.vepo.issues.workflow;

import java.util.Comparator;
import java.util.List;

public record WorkflowResponse(long id,
                               String name,
                               List<String> statuses,
                               String start,
                               List<TransitionResponse> transitions) {
    public static WorkflowResponse load(Workflow workflow) {
        return new WorkflowResponse(workflow.getId(),
                                    workflow.getName(),
                                    workflow.getStatuses()
                                            .stream()
                                            .sorted(Comparator.comparing(WorkflowStatus::getId))
                                            .map(WorkflowStatus::getName)
                                            .toList(),
                                    workflow.getStart()
                                            .getName(),
                                    workflow.getTransitions()
                                            .stream()
                                            .map(transition -> new TransitionResponse(transition.getFrom().getName(),
                                                                                      transition.getTo().getName()))
                                            .toList());
    }
}