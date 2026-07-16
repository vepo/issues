package dev.vepo.issues.git;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.vepo.issues.git.IngestCommitsRequest.IngestCommitItem;
import dev.vepo.issues.project.Project;
import dev.vepo.issues.project.ProjectAccessService;
import dev.vepo.issues.ticket.Ticket;
import dev.vepo.issues.ticket.TicketRepository;
import dev.vepo.issues.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;

@ApplicationScoped
public class GitCommitService {

    private final ProjectGitRepositoryService associationService;
    private final TicketCommitRepository ticketCommitRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final ProjectAccessService accessService;
    private final ObjectMapper objectMapper;

    @Inject
    public GitCommitService(ProjectGitRepositoryService associationService,
                            TicketCommitRepository ticketCommitRepository,
                            TicketRepository ticketRepository,
                            UserRepository userRepository,
                            ProjectAccessService accessService,
                            ObjectMapper objectMapper) {
        this.associationService = associationService;
        this.ticketCommitRepository = ticketCommitRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public IngestCommitsResponse ingestAuthenticated(long projectId, IngestCommitsRequest request, String username) {
        accessService.requireView(projectId, username);
        var association = associationService.requireAssociation(projectId);
        return linkCommits(association.getProject(), association, request.commits());
    }

    @Transactional
    public IngestCommitsResponse ingestWebhook(long projectId, byte[] rawBody, String hubSignature256, String gitlabToken) {
        var association = associationService.requireAssociation(projectId);
        WebhookSignatureVerifier.requireValid(association.getWebhookSecret(), rawBody, hubSignature256, gitlabToken);
        var commits = parseWebhookPayload(rawBody);
        return linkCommits(association.getProject(), association, commits);
    }

    public List<LinkedCommitResponse> listForTicket(long ticketId) {
        return ticketCommitRepository.findByTicketId(ticketId)
                                     .stream()
                                     .map(LinkedCommitResponse::load)
                                     .toList();
    }

    IngestCommitsResponse linkCommits(Project project, ProjectGitRepository association, List<IngestCommitItem> commits) {
        int linked = 0;
        int skipped = 0;
        int unresolved = 0;
        var prefix = project.getPrefix();
        for (var item : commits) {
            if (item == null || item.sha() == null || item.sha().isBlank() || item.message() == null) {
                continue;
            }
            var identifiers = TicketIdentifierParser.findIdentifiers(prefix, item.message());
            if (identifiers.isEmpty()) {
                unresolved++;
                continue;
            }
            var matchedAny = false;
            for (var identifier : identifiers) {
                var ticketOpt = ticketRepository.findByIdentifier(identifier);
                if (ticketOpt.isEmpty()) {
                    unresolved++;
                    continue;
                }
                var ticket = ticketOpt.get();
                if (!ticket.getProject().getId().equals(project.getId())) {
                    unresolved++;
                    continue;
                }
                matchedAny = true;
                if (ticketCommitRepository.findByTicketIdAndSha(ticket.getId(), item.sha()).isPresent()) {
                    skipped++;
                    continue;
                }
                persistLink(ticket, project, association, item);
                linked++;
            }
            if (!matchedAny && identifiers.isEmpty()) {
                // already counted
            }
        }
        return new IngestCommitsResponse(linked, skipped, unresolved);
    }

    private void persistLink(Ticket ticket, Project project, ProjectGitRepository association, IngestCommitItem item) {
        var matchedUser = item.authorEmail() == null || item.authorEmail().isBlank()
                                                                                     ? null
                                                                                     : userRepository.findByEmail(item.authorEmail().trim()).orElse(null);
        var url = CommitDeepLinkBuilder.resolve(item.commitUrl(),
                                                association.getRemoteUrl(),
                                                association.getProvider(),
                                                item.sha());
        ticketCommitRepository.save(new TicketCommit(ticket,
                                                     project,
                                                     item.sha().trim(),
                                                     item.message(),
                                                     item.authorName(),
                                                     item.authorEmail(),
                                                     matchedUser,
                                                     item.committedAt(),
                                                     url));
    }

    private List<IngestCommitItem> parseWebhookPayload(byte[] rawBody) {
        try {
            var root = objectMapper.readTree(rawBody);
            var commits = new ArrayList<IngestCommitItem>();
            var node = root.get("commits");
            if (node != null && node.isArray()) {
                for (JsonNode commit : node) {
                    commits.add(toItem(commit));
                }
            } else if (root.has("sha") || root.has("id")) {
                commits.add(toItem(root));
            }
            if (commits.isEmpty()) {
                throw new BadRequestException("Webhook payload contains no commits");
            }
            return commits;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Invalid webhook JSON payload");
        }
    }

    private static IngestCommitItem toItem(JsonNode commit) {
        var sha = text(commit, "id", "sha");
        var message = text(commit, "message");
        if (sha == null || message == null) {
            throw new BadRequestException("Commit requires id/sha and message");
        }
        var author = commit.get("author");
        String authorName = null;
        String authorEmail = null;
        if (author != null && author.isObject()) {
            authorName = text(author, "name");
            authorEmail = text(author, "email");
        }
        Instant committedAt = null;
        var timestamp = text(commit, "timestamp", "committed_at");
        if (timestamp != null) {
            try {
                committedAt = Instant.parse(timestamp);
            } catch (Exception ignored) {
                committedAt = null;
            }
        }
        var url = text(commit, "url", "html_url");
        return new IngestCommitItem(sha, message, authorName, authorEmail, committedAt, url);
    }

    private static String text(JsonNode node, String... fields) {
        for (var field : fields) {
            var child = node.get(field);
            if (child != null && !child.isNull() && child.isTextual()) {
                return child.asText();
            }
        }
        return null;
    }
}
