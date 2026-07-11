package dev.vepo.issues.customfield;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ProjectTicketTemplateCustomValueId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "custom_field_id")
    private Long customFieldId;

    public ProjectTicketTemplateCustomValueId() {}

    public ProjectTicketTemplateCustomValueId(Long projectId, Long customFieldId) {
        this.projectId = projectId;
        this.customFieldId = customFieldId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getCustomFieldId() {
        return customFieldId;
    }

    public void setCustomFieldId(Long customFieldId) {
        this.customFieldId = customFieldId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId, customFieldId);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof ProjectTicketTemplateCustomValueId otherId) {
            return Objects.equals(projectId, otherId.projectId)
                    && Objects.equals(customFieldId, otherId.customFieldId);
        }
        return false;
    }
}
