package dev.vepo.issues.ticket.csvimport;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.CreationTimestamp;

import dev.vepo.issues.project.Project;
import dev.vepo.issues.user.User;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_ticket_imports")
public class TicketImport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "headers_json", nullable = false, columnDefinition = "TEXT")
    private String headersJson;

    @Column(name = "title_column")
    private String titleColumn;

    @Column(name = "description_column")
    private String descriptionColumn;

    @Column(name = "category_column")
    private String categoryColumn;

    @Column(name = "project_column")
    private String projectColumn;

    @Column(name = "priority_column")
    private String priorityColumn;

    @Column(name = "story_points_column")
    private String storyPointsColumn;

    @Column(name = "assignee_email_column")
    private String assigneeEmailColumn;

    @Column(name = "status_column")
    private String statusColumn;

    @Column(name = "custom_field_column_mapping", columnDefinition = "TEXT")
    private String customFieldColumnMapping;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketImportStatus status;

    @Column(name = "row_count", nullable = false)
    private int rowCount;

    @Column(nullable = false)
    private boolean truncated;

    @Column(name = "expected_bytes")
    private Long expectedBytes;

    @Column(name = "received_bytes", nullable = false)
    private long receivedBytes;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "ticketImport", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TicketImportRow> rows = new ArrayList<>();

    public TicketImport() {}

    public Long getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getHeadersJson() {
        return headersJson;
    }

    public void setHeadersJson(String headersJson) {
        this.headersJson = headersJson;
    }

    public String getTitleColumn() {
        return titleColumn;
    }

    public void setTitleColumn(String titleColumn) {
        this.titleColumn = titleColumn;
    }

    public String getDescriptionColumn() {
        return descriptionColumn;
    }

    public void setDescriptionColumn(String descriptionColumn) {
        this.descriptionColumn = descriptionColumn;
    }

    public String getCategoryColumn() {
        return categoryColumn;
    }

    public void setCategoryColumn(String categoryColumn) {
        this.categoryColumn = categoryColumn;
    }

    public String getProjectColumn() {
        return projectColumn;
    }

    public void setProjectColumn(String projectColumn) {
        this.projectColumn = projectColumn;
    }

    public String getPriorityColumn() {
        return priorityColumn;
    }

    public void setPriorityColumn(String priorityColumn) {
        this.priorityColumn = priorityColumn;
    }

    public String getStoryPointsColumn() {
        return storyPointsColumn;
    }

    public void setStoryPointsColumn(String storyPointsColumn) {
        this.storyPointsColumn = storyPointsColumn;
    }

    public String getAssigneeEmailColumn() {
        return assigneeEmailColumn;
    }

    public void setAssigneeEmailColumn(String assigneeEmailColumn) {
        this.assigneeEmailColumn = assigneeEmailColumn;
    }

    public String getStatusColumn() {
        return statusColumn;
    }

    public void setStatusColumn(String statusColumn) {
        this.statusColumn = statusColumn;
    }

    public String getCustomFieldColumnMapping() {
        return customFieldColumnMapping;
    }

    public void setCustomFieldColumnMapping(String customFieldColumnMapping) {
        this.customFieldColumnMapping = customFieldColumnMapping;
    }

    public TicketImportStatus getStatus() {
        return status;
    }

    public void setStatus(TicketImportStatus status) {
        this.status = status;
    }

    public int getRowCount() {
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }

    public Long getExpectedBytes() {
        return expectedBytes;
    }

    public void setExpectedBytes(Long expectedBytes) {
        this.expectedBytes = expectedBytes;
    }

    public long getReceivedBytes() {
        return receivedBytes;
    }

    public void setReceivedBytes(long receivedBytes) {
        this.receivedBytes = receivedBytes;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(int chunkCount) {
        this.chunkCount = chunkCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<TicketImportRow> getRows() {
        return rows;
    }

    public void setRows(List<TicketImportRow> rows) {
        this.rows = rows;
    }

    public void addRow(TicketImportRow row) {
        rows.add(row);
        row.setTicketImport(this);
    }

    public boolean isProjectScoped() {
        return project != null;
    }

    public ColumnMapping toColumnMapping(Map<String, String> customFieldColumns) {
        return new ColumnMapping(titleColumn,
                                 descriptionColumn,
                                 categoryColumn,
                                 priorityColumn,
                                 storyPointsColumn,
                                 assigneeEmailColumn,
                                 statusColumn,
                                 projectColumn,
                                 customFieldColumns);
    }

    public void applyColumnMapping(ColumnMapping mapping) {
        titleColumn = mapping.titleColumn();
        descriptionColumn = mapping.descriptionColumn();
        categoryColumn = mapping.categoryColumn();
        priorityColumn = mapping.priorityColumn();
        storyPointsColumn = mapping.storyPointsColumn();
        assigneeEmailColumn = mapping.assigneeEmailColumn();
        statusColumn = mapping.statusColumn();
        projectColumn = mapping.projectColumn();
    }
}
