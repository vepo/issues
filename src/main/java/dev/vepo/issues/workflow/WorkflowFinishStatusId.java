package dev.vepo.issues.workflow;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Embeddable;

@Embeddable
public class WorkflowFinishStatusId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long workflowId;
    private Long statusId;

    public WorkflowFinishStatusId() {}

    public WorkflowFinishStatusId(Long workflowId, Long statusId) {
        this.workflowId = workflowId;
        this.statusId = statusId;
    }

    public Long getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(Long workflowId) {
        this.workflowId = workflowId;
    }

    public Long getStatusId() {
        return statusId;
    }

    public void setStatusId(Long statusId) {
        this.statusId = statusId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(workflowId, statusId);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof WorkflowFinishStatusId otherId) {
            return Objects.equals(workflowId, otherId.workflowId) && Objects.equals(statusId, otherId.statusId);
        }
        return false;
    }
}
