package dev.vepo.issues.customfield;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class CustomFieldStatusRequiredId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "custom_field_id")
    private Long customFieldId;

    @Column(name = "status_id")
    private Long statusId;

    public CustomFieldStatusRequiredId() {}

    public CustomFieldStatusRequiredId(Long customFieldId, Long statusId) {
        this.customFieldId = customFieldId;
        this.statusId = statusId;
    }

    public Long getCustomFieldId() {
        return customFieldId;
    }

    public void setCustomFieldId(Long customFieldId) {
        this.customFieldId = customFieldId;
    }

    public Long getStatusId() {
        return statusId;
    }

    public void setStatusId(Long statusId) {
        this.statusId = statusId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(customFieldId, statusId);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof CustomFieldStatusRequiredId otherId) {
            return Objects.equals(customFieldId, otherId.customFieldId)
                    && Objects.equals(statusId, otherId.statusId);
        }
        return false;
    }
}
