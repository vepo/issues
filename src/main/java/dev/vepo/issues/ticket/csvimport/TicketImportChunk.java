package dev.vepo.issues.ticket.csvimport;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_ticket_import_chunks")
@IdClass(TicketImportChunkId.class)
public class TicketImportChunk {

    @Id
    @Column(name = "import_id", nullable = false)
    private Long importId;

    @Id
    @Column(name = "part_index", nullable = false)
    private int partIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_id", insertable = false, updatable = false)
    private TicketImport ticketImport;

    @Column(nullable = false, columnDefinition = "BYTEA")
    private byte[] content;

    @Column(name = "byte_length", nullable = false)
    private int byteLength;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public TicketImportChunk() {}

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

    public TicketImport getTicketImport() {
        return ticketImport;
    }

    public void setTicketImport(TicketImport ticketImport) {
        this.ticketImport = ticketImport;
        if (ticketImport != null) {
            this.importId = ticketImport.getId();
        }
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public int getByteLength() {
        return byteLength;
    }

    public void setByteLength(int byteLength) {
        this.byteLength = byteLength;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
