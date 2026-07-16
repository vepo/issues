package dev.vepo.issues.ticket.attachments;

import dev.vepo.issues.ticket.TicketUserResponse;

public record AttachmentResponse(long id,
                                 String originalFilename,
                                 String contentType,
                                 long sizeBytes,
                                 TicketUserResponse uploadedBy,
                                 long uploadedAt) {

    public static AttachmentResponse load(Attachment attachment) {
        return new AttachmentResponse(attachment.getId(),
                                      attachment.getOriginalFilename(),
                                      attachment.getContentType(),
                                      attachment.getSizeBytes(),
                                      TicketUserResponse.load(attachment.getUploadedBy()),
                                      attachment.getUploadedAt().toEpochMilli());
    }
}
