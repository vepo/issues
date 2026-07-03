package dev.vepo.issues.ticket.csvimport;

import dev.vepo.issues.ticket.Ticket;
import dev.vepo.issues.ticket.TicketPriority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_ticket_import_rows")
public class TicketImportRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_id", nullable = false)
    private TicketImport ticketImport;

    @Column(name = "row_number", nullable = false)
    private int rowNumber;

    @Column(name = "raw_values_json", nullable = false, columnDefinition = "TEXT")
    private String rawValuesJson;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "category_name")
    private String categoryName;

    @Column(name = "project_name")
    private String projectName;

    @Enumerated(EnumType.STRING)
    private TicketPriority priority;

    @Column(name = "assignee_email")
    private String assigneeEmail;

    @Column(name = "status_name")
    private String statusName;

    private Boolean valid;

    @Column(name = "validation_errors_json", columnDefinition = "TEXT")
    private String validationErrorsJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @Column(name = "import_error", columnDefinition = "TEXT")
    private String importError;

    public TicketImportRow() {}

    public Long getId() {
        return id;
    }

    public TicketImport getTicketImport() {
        return ticketImport;
    }

    public void setTicketImport(TicketImport ticketImport) {
        this.ticketImport = ticketImport;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getRawValuesJson() {
        return rawValuesJson;
    }

    public void setRawValuesJson(String rawValuesJson) {
        this.rawValuesJson = rawValuesJson;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public String getAssigneeEmail() {
        return assigneeEmail;
    }

    public void setAssigneeEmail(String assigneeEmail) {
        this.assigneeEmail = assigneeEmail;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public Boolean getValid() {
        return valid;
    }

    public void setValid(Boolean valid) {
        this.valid = valid;
    }

    public String getValidationErrorsJson() {
        return validationErrorsJson;
    }

    public void setValidationErrorsJson(String validationErrorsJson) {
        this.validationErrorsJson = validationErrorsJson;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public String getImportError() {
        return importError;
    }

    public void setImportError(String importError) {
        this.importError = importError;
    }

    public MappedImportRow toMappedImportRow() {
        return new MappedImportRow(rowNumber, title, description, categoryName, priority, assigneeEmail, statusName, projectName);
    }
}
