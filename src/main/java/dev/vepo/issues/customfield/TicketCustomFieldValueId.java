package dev.vepo.issues.customfield;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class TicketCustomFieldValueId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "ticket_id")
    private Long ticketId;

    @Column(name = "custom_field_id")
    private Long customFieldId;

    public TicketCustomFieldValueId() {}

    public TicketCustomFieldValueId(Long ticketId, Long customFieldId) {
        this.ticketId = ticketId;
        this.customFieldId = customFieldId;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public Long getCustomFieldId() {
        return customFieldId;
    }

    public void setCustomFieldId(Long customFieldId) {
        this.customFieldId = customFieldId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticketId, customFieldId);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof TicketCustomFieldValueId otherId) {
            return Objects.equals(ticketId, otherId.ticketId)
                    && Objects.equals(customFieldId, otherId.customFieldId);
        }
        return false;
    }
}
