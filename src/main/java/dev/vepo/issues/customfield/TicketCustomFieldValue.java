package dev.vepo.issues.customfield;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_ticket_custom_field_values")
public class TicketCustomFieldValue {

    @EmbeddedId
    private TicketCustomFieldValueId id;

    @MapsId("customFieldId")
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "custom_field_id", nullable = false)
    private CustomField customField;

    @Column(name = "string_value", length = 255)
    private String stringValue;

    @Column(name = "text_value", columnDefinition = "TEXT")
    private String textValue;

    @Column(name = "integer_value")
    private Integer integerValue;

    @Column(name = "boolean_value")
    private Boolean booleanValue;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "enum_option_id")
    private CustomFieldEnumOption enumOption;

    public TicketCustomFieldValue() {}

    public TicketCustomFieldValue(Long ticketId, CustomField customField) {
        this.customField = customField;
        this.id = new TicketCustomFieldValueId(ticketId, customField.getId());
    }

    public TicketCustomFieldValueId getId() {
        return id;
    }

    public void setId(TicketCustomFieldValueId id) {
        this.id = id;
    }

    public CustomField getCustomField() {
        return customField;
    }

    public void setCustomField(CustomField customField) {
        this.customField = customField;
    }

    public Long getTicketId() {
        return id == null ? null : id.getTicketId();
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public String getTextValue() {
        return textValue;
    }

    public void setTextValue(String textValue) {
        this.textValue = textValue;
    }

    public Integer getIntegerValue() {
        return integerValue;
    }

    public void setIntegerValue(Integer integerValue) {
        this.integerValue = integerValue;
    }

    public Boolean getBooleanValue() {
        return booleanValue;
    }

    public void setBooleanValue(Boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    public CustomFieldEnumOption getEnumOption() {
        return enumOption;
    }

    public void setEnumOption(CustomFieldEnumOption enumOption) {
        this.enumOption = enumOption;
    }

    public void clearTypedValues() {
        stringValue = null;
        textValue = null;
        integerValue = null;
        booleanValue = null;
        enumOption = null;
    }
}
