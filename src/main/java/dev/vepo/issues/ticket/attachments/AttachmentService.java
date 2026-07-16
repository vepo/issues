package dev.vepo.issues.ticket.attachments;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.issues.project.ProjectAccessService;
import dev.vepo.issues.ticket.Ticket;
import dev.vepo.issues.ticket.TicketRepository;
import dev.vepo.issues.ticket.history.TicketHistoryService;
import dev.vepo.issues.user.Role;
import dev.vepo.issues.user.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

@ApplicationScoped
public class AttachmentService {

    private static final Logger logger = LoggerFactory.getLogger(AttachmentService.class);

    private final AttachmentRepository attachmentRepository;
    private final TicketRepository ticketRepository;
    private final ProjectAccessService projectAccessService;
    private final TicketHistoryService historyService;
    private final Path storageRoot;

    @Inject
    public AttachmentService(AttachmentRepository attachmentRepository,
                             TicketRepository ticketRepository,
                             ProjectAccessService projectAccessService,
                             TicketHistoryService historyService,
                             @ConfigProperty(name = "issues.attachments.storage-dir") String storageDir) {
        this.attachmentRepository = attachmentRepository;
        this.ticketRepository = ticketRepository;
        this.projectAccessService = projectAccessService;
        this.historyService = historyService;
        this.storageRoot = Path.of(storageDir).toAbsolutePath().normalize();
    }

    @Transactional
    public AttachmentResponse upload(long ticketId, FileUpload file, String username) {
        if (file == null) {
            throw new BadRequestException("File is required");
        }
        var user = projectAccessService.requireUser(username);
        var ticket = requireActiveTicket(ticketId);
        requireWrite(user, ticket);

        var originalFilename = AttachmentContentRules.sanitizeFilename(file.fileName());
        var contentType = file.contentType();
        var size = file.size();
        if (size <= 0) {
            throw new BadRequestException("File is empty");
        }
        if (size > AttachmentContentRules.MAX_FILE_BYTES) {
            throw new BadRequestException("File exceeds maximum size of 10 MB");
        }
        if (!AttachmentContentRules.isAllowed(originalFilename, contentType)) {
            throw new BadRequestException("File type is not allowed");
        }
        if (attachmentRepository.countByTicketId(ticketId) >= AttachmentContentRules.MAX_FILES_PER_TICKET) {
            throw new BadRequestException("Ticket already has the maximum of 20 attachments");
        }
        if (attachmentRepository.sumSizeBytesByTicketId(ticketId) + size > AttachmentContentRules.MAX_TOTAL_BYTES_PER_TICKET) {
            throw new BadRequestException("Ticket attachment total size exceeds 50 MB");
        }

        var storageKey = "tickets/%d/%s".formatted(ticketId, UUID.randomUUID());
        var target = storageRoot.resolve(storageKey).normalize();
        if (!target.startsWith(storageRoot)) {
            throw new BadRequestException("Invalid storage path");
        }
        try {
            Files.createDirectories(target.getParent());
            Files.copy(file.filePath(), target);
        } catch (IOException e) {
            throw new BadRequestException("Unable to store attachment", e);
        }

        var attachment = attachmentRepository.save(new Attachment(ticket,
                                                                  originalFilename,
                                                                  contentType.strip(),
                                                                  size,
                                                                  storageKey,
                                                                  user));
        historyService.logAttachmentAdded(ticket, user, originalFilename, attachment.getId());
        logger.info("Attachment uploaded: ticketId={} attachmentId={} filename={}", ticketId, attachment.getId(), originalFilename);
        return AttachmentResponse.load(attachment);
    }

    public List<AttachmentResponse> list(long ticketId, String username) {
        var user = projectAccessService.requireUser(username);
        var ticket = requireTicketForRead(ticketId, user);
        requireRead(user, ticket);
        return attachmentRepository.findByTicketId(ticketId)
                                   .stream()
                                   .map(AttachmentResponse::load)
                                   .toList();
    }

    public Response download(long ticketId, long attachmentId, String username) {
        var user = projectAccessService.requireUser(username);
        var ticket = requireTicketForRead(ticketId, user);
        requireRead(user, ticket);
        var attachment = attachmentRepository.findByIdAndTicketId(attachmentId, ticketId)
                                             .orElseThrow(() -> attachmentNotFound(attachmentId));
        var path = storageRoot.resolve(attachment.getStorageKey()).normalize();
        if (!path.startsWith(storageRoot) || !Files.isRegularFile(path)) {
            throw new NotFoundException("Attachment file not found");
        }
        var filename = AttachmentContentRules.sanitizeFilename(attachment.getOriginalFilename());
        var encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        StreamingOutput stream = output -> {
            try (InputStream in = Files.newInputStream(path)) {
                in.transferTo(output);
            }
        };
        return Response.ok(stream)
                       .type(attachment.getContentType())
                       .header("Content-Disposition",
                               "attachment; filename=\"%s\"; filename*=UTF-8''%s".formatted(asciiFallback(filename), encoded))
                       .header("Content-Length", Long.toString(attachment.getSizeBytes()))
                       .build();
    }

    @Transactional
    public void delete(long ticketId, long attachmentId, String username) {
        var user = projectAccessService.requireUser(username);
        var ticket = requireActiveTicket(ticketId);
        requireWrite(user, ticket);
        var attachment = attachmentRepository.findByIdAndTicketId(attachmentId, ticketId)
                                             .orElseThrow(() -> attachmentNotFound(attachmentId));
        var filename = attachment.getOriginalFilename();
        var storageKey = attachment.getStorageKey();
        var id = attachment.getId();
        attachmentRepository.delete(attachment);
        historyService.logAttachmentRemoved(ticket, user, filename, id);
        deleteFileBestEffort(storageKey);
        logger.info("Attachment deleted: ticketId={} attachmentId={}", ticketId, attachmentId);
    }

    private void deleteFileBestEffort(String storageKey) {
        try {
            var path = storageRoot.resolve(storageKey).normalize();
            if (path.startsWith(storageRoot)) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            logger.warn("Failed to delete attachment file for key={}: {}", storageKey, e.getMessage());
        }
    }

    private Ticket requireActiveTicket(long ticketId) {
        return ticketRepository.findById(ticketId)
                               .orElseThrow(() -> ticketNotFound(ticketId));
    }

    private Ticket requireTicketForRead(long ticketId, User user) {
        if (canViewDeletedTicket(user)) {
            return ticketRepository.findByIdIncludingDeleted(ticketId)
                                   .orElseThrow(() -> ticketNotFound(ticketId));
        }
        return requireActiveTicket(ticketId);
    }

    private boolean canViewDeletedTicket(User user) {
        return user.getRoles().contains(Role.ADMIN) || user.getRoles().contains(Role.PROJECT_MANAGER);
    }

    private void requireWrite(User user, Ticket ticket) {
        if (!projectAccessService.canViewProject(user, ticket.getProject())) {
            throw new ForbiddenException("Access denied to project %d".formatted(ticket.getProject().getId()));
        }
    }

    private void requireRead(User user, Ticket ticket) {
        if (!projectAccessService.canRead(Optional.of(user), ticket.getProject())) {
            throw new ForbiddenException("Access denied to project %d".formatted(ticket.getProject().getId()));
        }
    }

    private static NotFoundException ticketNotFound(long ticketId) {
        return new NotFoundException("Ticket does not found! ticketId=%d".formatted(ticketId));
    }

    private static NotFoundException attachmentNotFound(long attachmentId) {
        return new NotFoundException("Attachment does not found! attachmentId=%d".formatted(attachmentId));
    }

    private static String asciiFallback(String filename) {
        var builder = new StringBuilder(filename.length());
        for (var i = 0; i < filename.length(); i++) {
            var c = filename.charAt(i);
            builder.append(c >= 0x20 && c <= 0x7E && c != '"' ? c : '_');
        }
        var result = builder.toString().strip();
        return result.isEmpty() ? "attachment" : result;
    }
}
