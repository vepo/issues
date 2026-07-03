package dev.vepo.issues.workflow;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_workflow_finish_statuses")
public class WorkflowFinishStatus {

    @EmbeddedId
    private WorkflowFinishStatusId id;

    @MapsId("workflowId")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @MapsId("statusId")
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "status_id", nullable = false)
    private WorkflowStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FinishOutcome outcome;

    public WorkflowFinishStatus() {}

    public WorkflowFinishStatus(Workflow workflow, WorkflowStatus status, FinishOutcome outcome) {
        this.workflow = workflow;
        this.status = status;
        this.outcome = outcome;
        this.id = new WorkflowFinishStatusId(workflow.getId(), status.getId());
    }

    public WorkflowFinishStatusId getId() {
        return id;
    }

    public void setId(WorkflowFinishStatusId id) {
        this.id = id;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowStatus status) {
        this.status = status;
    }

    public FinishOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(FinishOutcome outcome) {
        this.outcome = outcome;
    }
}
