package dev.vepo.issues.project;

import java.util.List;
import java.util.Objects;

import dev.vepo.issues.workflow.Workflow;
import dev.vepo.issues.workflow.WorkflowStatus;
import dev.vepo.issues.workflow.WorkflowTransition;
import dev.vepo.issues.workflow.WorkflowWipLimit;

public record ProjectStatusResponse(long id,
                                    String name,
                                    boolean start,
                                    List<Long> moveable,
                                    Integer wipLimit) {

    public static ProjectStatusResponse load(WorkflowStatus status, Workflow workflow) {
        Integer limit = workflow.getWipLimits()
                                .stream()
                                .filter(wip -> Objects.equals(wip.getStatus().getId(), status.getId()))
                                .map(WorkflowWipLimit::getWipLimit)
                                .findFirst()
                                .orElse(null);
        return new ProjectStatusResponse(status.getId(),
                                         status.getName(),
                                         workflow.getStart().equals(status),
                                         workflow.getTransitions().stream()
                                                 .filter(t -> t.getFrom().equals(status))
                                                 .map(WorkflowTransition::getTo)
                                                 .map(WorkflowStatus::getId)
                                                 .toList(),
                                         limit);
    }
}
