package dev.vepo.issues.workflow;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "tb_workflow_status", uniqueConstraints = @UniqueConstraint(name = "tb_workflow_status_UK", columnNames = "name"))
public class WorkflowStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    public WorkflowStatus() {}

    public WorkflowStatus(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (Objects.nonNull(other) && other instanceof WorkflowStatus otherStatus) {
            return Objects.equals(this.id, otherStatus.id);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "WorkflowStatus [id=%d, name=%s]".formatted(id, name);
    }

}