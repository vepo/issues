package dev.vepo.issues.phase;

import java.time.LocalDateTime;
import java.util.Set;

public record VersionChangelogEntry(long ticketId,
                                    String identifier,
                                    String title,
                                    String statusName,
                                    String priority,
                                    LocalDateTime finishedAt,
                                    Set<ChangelogAssociation> associations) {}
