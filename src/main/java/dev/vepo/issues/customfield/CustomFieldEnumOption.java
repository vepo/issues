package dev.vepo.issues.customfield;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_custom_field_enum_options")
public class CustomFieldEnumOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_field_id", nullable = false)
    private CustomField customField;

    @Column(name = "option_value", nullable = false, length = 128)
    private String value;

    @Column(nullable = false, length = 128)
    private String label;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public CustomFieldEnumOption() {}

    public CustomFieldEnumOption(CustomField customField, String value, String label, int sortOrder) {
        this.customField = customField;
        this.value = value;
        this.label = label;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CustomField getCustomField() {
        return customField;
    }

    public void setCustomField(CustomField customField) {
        this.customField = customField;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
