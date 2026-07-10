package dev.vepo.issues.workflow;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_workflow_wip_limits")
public class WorkflowWipLimit {

    @EmbeddedId
    private WorkflowWipLimitId id;

    @MapsId("workflowId")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @MapsId("statusId")
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "status_id", nullable = false)
    private WorkflowStatus status;

    @Column(name = "wip_limit", nullable = false)
    private int wipLimit;

    public WorkflowWipLimit() {}

    public WorkflowWipLimit(Workflow workflow, WorkflowStatus status, int wipLimit) {
        this.workflow = workflow;
        this.status = status;
        this.wipLimit = wipLimit;
        this.id = new WorkflowWipLimitId(workflow.getId(), status.getId());
    }

    public WorkflowWipLimitId getId() {
        return id;
    }

    public void setId(WorkflowWipLimitId id) {
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

    public int getWipLimit() {
        return wipLimit;
    }

    public void setWipLimit(int wipLimit) {
        this.wipLimit = wipLimit;
    }
}
