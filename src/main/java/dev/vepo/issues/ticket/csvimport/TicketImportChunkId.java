package dev.vepo.issues.ticket.csvimport;

import java.io.Serializable;
import java.util.Objects;

public class TicketImportChunkId implements Serializable {

    private Long importId;
    private int partIndex;

    public TicketImportChunkId() {}

    public TicketImportChunkId(Long importId, int partIndex) {
        this.importId = importId;
        this.partIndex = partIndex;
    }

    public Long getImportId() {
        return importId;
    }

    public void setImportId(Long importId) {
        this.importId = importId;
    }

    public int getPartIndex() {
        return partIndex;
    }

    public void setPartIndex(int partIndex) {
        this.partIndex = partIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TicketImportChunkId that)) {
            return false;
        }
        return partIndex == that.partIndex && Objects.equals(importId, that.importId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(importId, partIndex);
    }
}
