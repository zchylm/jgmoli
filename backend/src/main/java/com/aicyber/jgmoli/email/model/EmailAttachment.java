package com.aicyber.jgmoli.email.model;

public record EmailAttachment(
        String filename,
        String contentType,
        String base64Content
) {
}
