package com.aicyber.jgmoli.email.provider;

import com.aicyber.jgmoli.email.model.EmailAttachment;
import com.aicyber.jgmoli.email.model.QueuedEmail;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResendEmailGatewayTest {
    private final ObjectMapper json = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void sendsPdfAttachmentWithInvoiceEmail() throws Exception {
        AtomicReference<JsonNode> requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/emails", exchange -> {
            requestBody.set(json.readTree(exchange.getRequestBody()));
            byte[] response = "{\"id\":\"email_123\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        ResendEmailGateway gateway = new ResendEmailGateway(json, java.net.http.HttpClient.newHttpClient(),
                "test-key", "http://127.0.0.1:" + server.getAddress().getPort() + "/emails",
                "JG MOLI Orders <orders@jgmoli.com.au>", "JG MOLI Support <support@jgmoli.com.au>",
                "support@jgmoli.com.au");
        QueuedEmail email = new QueuedEmail(UUID.randomUUID(), "INVOICE_ISSUED", "buyer@example.com",
                "Buyer", "Tax invoice", "Invoice text", "<p>Invoice</p>",
                new EmailAttachment("JG-MOLI-JGM-INV-2026-000001.pdf", "application/pdf", "JVBERi0="),
                "invoice-issued:test", 1);

        assertEquals("email_123", gateway.send(email));
        assertEquals("JG MOLI Orders <orders@jgmoli.com.au>", requestBody.get().path("from").asText());
        assertEquals("JG-MOLI-JGM-INV-2026-000001.pdf",
                requestBody.get().path("attachments").path(0).path("filename").asText());
        assertEquals("application/pdf",
                requestBody.get().path("attachments").path(0).path("content_type").asText());
        assertEquals("JVBERi0=", requestBody.get().path("attachments").path(0).path("content").asText());
    }

    @Test
    void sendsAccountEmailFromSupportAddress() throws Exception {
        AtomicReference<JsonNode> requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/emails", exchange -> {
            requestBody.set(json.readTree(exchange.getRequestBody()));
            byte[] response = "{\"id\":\"email_456\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        ResendEmailGateway gateway = new ResendEmailGateway(json, java.net.http.HttpClient.newHttpClient(),
                "test-key", "http://127.0.0.1:" + server.getAddress().getPort() + "/emails",
                "JG MOLI Orders <orders@jgmoli.com.au>", "JG MOLI Support <support@jgmoli.com.au>",
                "support@jgmoli.com.au");
        QueuedEmail email = new QueuedEmail(UUID.randomUUID(), "PASSWORD_RESET", "buyer@example.com",
                "Buyer", "Reset password", "Reset text", "<p>Reset</p>", null,
                "password-reset:test", 1);

        assertEquals("email_456", gateway.send(email));
        assertEquals("JG MOLI Support <support@jgmoli.com.au>", requestBody.get().path("from").asText());
    }
}
