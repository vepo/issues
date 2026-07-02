package dev.vepo.issues.workflow;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "tb_workflow_transitions", uniqueConstraints = @UniqueConstraint(name = "tb_workflow_transition_UK", columnNames = { "workflow_id", "from_id", "to_id" }))
public class WorkflowTransition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "from_id", nullable = false)
    private WorkflowStatus from;

    @ManyToOne
    @JoinColumn(name = "to_id", nullable = false)
    private WorkflowStatus to;

    public WorkflowTransition() {}

    public WorkflowTransition(WorkflowStatus from, WorkflowStatus to) {
        this.from = from;
        this.to = to;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WorkflowStatus getFrom() {
        return from;
    }

    public void setFrom(WorkflowStatus from) {
        this.from = from;
    }

    public WorkflowStatus getTo() {
        return to;
    }

    public void setTo(WorkflowStatus to) {
        this.to = to;
    }

}