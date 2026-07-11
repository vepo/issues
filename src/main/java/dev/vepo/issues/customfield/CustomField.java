package dev.vepo.issues.customfield;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_custom_fields")
public class CustomField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "field_key", nullable = false, length = 32)
    private String key;

    @Column(nullable = false, length = 128)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false, length = 16)
    private CustomFieldType type;

    @Column(nullable = false)
    private boolean required;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "workflow_id")
    private Long workflowId;

    @Column(name = "string_max_length")
    private Integer stringMaxLength;

    @Column(name = "integer_min")
    private Integer integerMin;

    @Column(name = "integer_max")
    private Integer integerMax;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @OneToMany(mappedBy = "customField", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private List<CustomFieldEnumOption> enumOptions = new ArrayList<>();

    @OneToMany(mappedBy = "customField", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<CustomFieldStatusRequired> statusRequired = new HashSet<>();

    public CustomField() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public CustomFieldType getType() {
        return type;
    }

    public void setType(CustomFieldType type) {
        this.type = type;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(Long workflowId) {
        this.workflowId = workflowId;
    }

    public Integer getStringMaxLength() {
        return stringMaxLength;
    }

    public void setStringMaxLength(Integer stringMaxLength) {
        this.stringMaxLength = stringMaxLength;
    }

    public Integer getIntegerMin() {
        return integerMin;
    }

    public void setIntegerMin(Integer integerMin) {
        this.integerMin = integerMin;
    }

    public Integer getIntegerMax() {
        return integerMax;
    }

    public void setIntegerMax(Integer integerMax) {
        this.integerMax = integerMax;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public List<CustomFieldEnumOption> getEnumOptions() {
        return enumOptions;
    }

    public void setEnumOptions(List<CustomFieldEnumOption> enumOptions) {
        this.enumOptions = enumOptions;
    }

    public Set<CustomFieldStatusRequired> getStatusRequired() {
        return statusRequired;
    }

    public void setStatusRequired(Set<CustomFieldStatusRequired> statusRequired) {
        this.statusRequired = statusRequired;
    }

    public boolean isProjectOwned() {
        return projectId != null;
    }

    public boolean isWorkflowOwned() {
        return workflowId != null;
    }
}
