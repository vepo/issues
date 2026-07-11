package dev.vepo.issues.customfield;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_custom_field_status_required")
public class CustomFieldStatusRequired {

    @EmbeddedId
    private CustomFieldStatusRequiredId id;

    @MapsId("customFieldId")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_field_id", nullable = false)
    private CustomField customField;

    public CustomFieldStatusRequired() {}

    public CustomFieldStatusRequired(CustomField customField, Long statusId) {
        this.customField = customField;
        this.id = new CustomFieldStatusRequiredId(customField.getId(), statusId);
    }

    public CustomFieldStatusRequiredId getId() {
        return id;
    }

    public void setId(CustomFieldStatusRequiredId id) {
        this.id = id;
    }

    public CustomField getCustomField() {
        return customField;
    }

    public void setCustomField(CustomField customField) {
        this.customField = customField;
    }

    public Long getStatusId() {
        return id == null ? null : id.getStatusId();
    }
}
